import { API_BASE_URL } from '@/config/env'

export interface ChatStreamParams {
  appId: number | string
  message: string
}

export interface ChatStreamHandlers {
  /** 收到增量内容 */
  onMessage: (chunk: string) => void
  /** 正常结束（后端发送 done 事件） */
  onDone: () => void
  /** 失败（网络异常或后端返回 business-error 事件） */
  onError: (message: string) => void
}

interface SseFrame {
  event: string
  data: string
}

/**
 * 解析单个 SSE 帧（形如 "event: done\ndata: {}"）
 */
function parseFrame(rawFrame: string): SseFrame | null {
  let event = 'message'
  const dataLines: string[] = []
  let hasField = false
  for (const line of rawFrame.split(/\r?\n/)) {
    // 以冒号开头的是注释（心跳），忽略
    if (!line || line.startsWith(':')) {
      continue
    }
    const colonIndex = line.indexOf(':')
    const field = colonIndex === -1 ? line : line.slice(0, colonIndex)
    let value = colonIndex === -1 ? '' : line.slice(colonIndex + 1)
    if (value.startsWith(' ')) {
      value = value.slice(1)
    }
    if (field === 'event') {
      event = value
      hasField = true
    } else if (field === 'data') {
      dataLines.push(value)
      hasField = true
    }
  }
  // 只带 event 字段的帧（如 "event: done"）同样是有效帧
  if (!hasField) {
    return null
  }
  return { event, data: dataLines.join('\n') }
}

/**
 * 从 JSON 文本中取指定字段，解析失败时返回 null
 */
function readJsonField(text: string, field: string): string | null {
  if (!text) {
    return null
  }
  try {
    const parsed = JSON.parse(text)
    const value = parsed?.[field]
    return typeof value === 'string' ? value : null
  } catch (error) {
    console.error('解析 SSE 数据失败：', text, error)
    return null
  }
}

/**
 * 以 fetch 流式读取的方式调用代码生成接口（SSE）
 *
 * 相比浏览器的 EventSource，fetch 可以自定义请求头，因此能够携带 Authorization token。
 * 注意：不做自动重连，避免重复触发 AI 代码生成。
 */
export async function streamChatToGenCode(
  params: ChatStreamParams,
  handlers: ChatStreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const { onMessage, onDone, onError } = handlers
  const query = new URLSearchParams({
    appId: String(params.appId ?? ''),
    message: params.message,
  })
  const response = await fetch(`${API_BASE_URL}/chat/gen/code?${query}`, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      Authorization: 'Bearer ' + (localStorage.getItem('token') ?? ''),
    },
    signal,
  })
  if (!response.ok || !response.body) {
    throw new Error(`请求失败（HTTP ${response.status}）`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let finished = false

  // 处理单个 SSE 帧，返回是否已收到结束事件
  const dispatch = (rawFrame: string): boolean => {
    const frame = parseFrame(rawFrame)
    if (!frame) {
      return false
    }
    if (frame.event === 'business-error') {
      onError(readJsonField(frame.data, 'message') ?? '生成失败，请重试')
      return true
    }
    if (frame.event === 'done') {
      onDone()
      return true
    }
    const content = readJsonField(frame.data, 'd')
    if (content) {
      onMessage(content)
    }
    return false
  }

  try {
    while (!finished) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      // SSE 以空行分隔帧
      let separatorIndex = buffer.search(/\r?\n\r?\n/)
      while (separatorIndex >= 0 && !finished) {
        const rawFrame = buffer.slice(0, separatorIndex)
        buffer = buffer.slice(separatorIndex).replace(/^\r?\n\r?\n/, '')
        finished = dispatch(rawFrame)
        separatorIndex = buffer.search(/\r?\n\r?\n/)
      }
    }
    if (!finished) {
      throw new Error('连接已中断，请重试')
    }
  } finally {
    // 主动释放连接，避免后端继续推送
    await reader.cancel().catch(() => undefined)
  }
}
