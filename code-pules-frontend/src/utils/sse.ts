export interface FetchSseOptions {
  url: string
  token?: string | null
  signal?: AbortSignal
  onMessage?: (data: string, event: string) => void
  onDone?: () => void
  onError?: (error: Error) => void
}

export type SseErrorKind = 'business' | 'http' | 'network'

/** SSE 请求失败的结构化错误，便于调用方区分业务异常与连接异常 */
export class SseError extends Error {
  readonly kind: SseErrorKind
  readonly status?: number
  readonly code?: number

  constructor(kind: SseErrorKind, message: string, options?: { status?: number; code?: number }) {
    super(message)
    this.name = 'SseError'
    this.kind = kind
    this.status = options?.status
    this.code = options?.code
  }
}

/** 解析单个 SSE 帧（不含帧末尾的空行） */
function parseFrame(raw: string): { event: string; data: string } | null {
  let event = 'message'
  let sawEventField = false
  const dataLines: string[] = []
  let hasData = false
  for (const line of raw.split('\n')) {
    // 忽略空行与注释/心跳行
    if (!line || line.startsWith(':')) continue
    const idx = line.indexOf(':')
    const field = idx === -1 ? line : line.slice(0, idx)
    let value = idx === -1 ? '' : line.slice(idx + 1)
    if (value.startsWith(' ')) value = value.slice(1)
    if (field === 'event') {
      event = value
      sawEventField = true
    } else if (field === 'data') {
      dataLines.push(value)
      hasData = true
    }
  }
  // 显式声明了 event 但没有 data 的帧（例如空 data 的 done 事件）也要交给调用方处理
  return hasData || sawEventField ? { event, data: dataLines.join('\n') } : null
}

/** 从响应体中尽力提取后端返回的错误码与错误信息 */
async function readJsonError(response: Response): Promise<{ code?: number; message?: string }> {
  try {
    const parsed = JSON.parse(await response.text())
    return {
      code: typeof parsed?.code === 'number' ? parsed.code : undefined,
      message: typeof parsed?.message === 'string' && parsed.message ? parsed.message : undefined,
    }
  } catch {
    return {}
  }
}

/**
 * 基于 fetch 的 SSE 客户端。
 * 原生 EventSource 无法设置自定义请求头，导致无法携带 Authorization，
 * 这里改用 fetch + ReadableStream 手动解析 SSE，从而可以带上鉴权头。
 */
export async function fetchSse({
  url,
  token,
  signal,
  onMessage,
  onDone,
  onError,
}: FetchSseOptions): Promise<void> {
  let finished = false
  const finish = () => {
    if (!finished) {
      finished = true
      onDone?.()
    }
  }

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      credentials: 'include',
      signal,
    })

    if (!response.ok) {
      const contentType = response.headers.get('content-type') || ''
      // 只在确知是 JSON 时才读取响应体，避免非 2xx 的流式连接读取时挂住
      const info: { code?: number; message?: string } = contentType.includes('application/json')
        ? await readJsonError(response)
        : {}
      throw new SseError('http', info.message || `连接失败（HTTP ${response.status}）`, {
        status: response.status,
        code: info.code,
      })
    }

    if (!response.body) {
      throw new SseError('network', '连接异常：服务端未返回流式内容')
    }

    // 后端异常处理器未走 SSE 通道、而是返回了普通 JSON 时，直接给出可读错误
    const responseContentType = response.headers.get('content-type') || ''
    if (responseContentType.includes('application/json')) {
      const info = await readJsonError(response)
      throw new SseError('business', info.message || '服务端返回了非流式响应', { code: info.code })
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    // 是否已收到服务端的 done 事件，用于识别连接提前中断
    let sawDone = false

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      // 兼容 \r\n / \r 换行
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n|\r/g, '\n')

      let sep: number
      while ((sep = buffer.indexOf('\n\n')) !== -1) {
        const frame = parseFrame(buffer.slice(0, sep))
        buffer = buffer.slice(sep + 2)
        if (!frame) continue
        if (frame.event === 'done') {
          sawDone = true
          finish()
          continue
        }
        onMessage?.(frame.data, frame.event)
      }
    }

    if (!finished) {
      if (sawDone) {
        finish()
      } else {
        // 服务端在发送 done 之前断开（例如生成流内部报错），按连接异常处理
        finished = true
        onError?.(new SseError('network', '连接异常：生成未完成，请重试'))
      }
    }
  } catch (error) {
    if (finished) return
    finished = true
    onError?.(
      error instanceof SseError ? error : new SseError('network', '连接异常，请检查网络后重试'),
    )
  }
}
