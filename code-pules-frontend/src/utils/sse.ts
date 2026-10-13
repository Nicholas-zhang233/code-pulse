export interface FetchSseOptions {
  url: string
  token?: string | null
  signal?: AbortSignal
  onMessage?: (data: string, event: string) => void
  onDone?: () => void
  onError?: (error: Error) => void
}

/** 解析单个 SSE 帧（不含帧末尾的空行） */
function parseFrame(raw: string): { event: string; data: string } | null {
  let event = 'message'
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
    } else if (field === 'data') {
      dataLines.push(value)
      hasData = true
    }
  }
  return hasData ? { event, data: dataLines.join('\n') } : null
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

    if (!response.ok || !response.body) {
      throw new Error(`SSE 连接失败，状态码：${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

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
          finish()
          continue
        }
        onMessage?.(frame.data, frame.event)
      }
    }

    finish()
  } catch (error) {
    if (finished) return
    finished = true
    onError?.(error as Error)
  }
}