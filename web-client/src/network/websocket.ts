import type { ClientMessage, ServerMessage } from '../types'

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected'

export interface WebSocketConfig {
  url: string
  onMessage: (message: ServerMessage) => void
  onStatusChange: (status: ConnectionStatus) => void
  onError: (error: Event) => void
  reconnectAttempts?: number
  reconnectDelay?: number
}

/**
 * WebSocket manager for game server communication.
 *
 * Handles:
 * - Connection lifecycle
 * - Automatic reconnection
 * - Message serialization/deserialization
 * - Connection status tracking
 */
export class GameWebSocket {
  private ws: WebSocket | null = null
  private config: WebSocketConfig
  private reconnectCount = 0
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null
  private isIntentionallyClosed = false

  constructor(config: WebSocketConfig) {
    this.config = {
      reconnectAttempts: 5,
      reconnectDelay: 1000,
      ...config,
    }
  }

  /**
   * Connect to the WebSocket server.
   */
  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.warn('WebSocket already connected')
      return
    }

    this.isIntentionallyClosed = false
    this.config.onStatusChange('connecting')

    try {
      this.ws = new WebSocket(this.config.url)
      this.setupEventHandlers()
    } catch (error) {
      console.error('Failed to create WebSocket:', error)
      this.config.onStatusChange('disconnected')
      this.scheduleReconnect()
    }
  }

  /**
   * Disconnect from the WebSocket server.
   */
  disconnect(): void {
    this.isIntentionallyClosed = true
    this.cancelReconnect()

    if (this.ws) {
      this.ws.close(1000, 'Client disconnected')
      this.ws = null
    }

    this.config.onStatusChange('disconnected')
  }

  /**
   * Send a message to the server.
   */
  send(message: ClientMessage): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('Cannot send message: WebSocket not connected')
      return
    }

    try {
      const json = JSON.stringify(message)
      this.ws.send(json)
    } catch (error) {
      console.error('Failed to send message:', error)
    }
  }

  /**
   * Check if the WebSocket is connected.
   */
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  private setupEventHandlers(): void {
    if (!this.ws) return

    this.ws.onopen = () => {
      console.log('WebSocket connected')
      this.reconnectCount = 0
      this.config.onStatusChange('connected')
    }

    this.ws.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason)
      this.config.onStatusChange('disconnected')

      if (!this.isIntentionallyClosed) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = (event) => {
      console.error('WebSocket error:', event)
      this.config.onError(event)
    }

    this.ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data as string) as ServerMessage
        this.config.onMessage(message)
      } catch (error) {
        console.error('Failed to parse message:', error)
      }
    }
  }

  private scheduleReconnect(): void {
    const maxAttempts = this.config.reconnectAttempts ?? 5
    const delay = this.config.reconnectDelay ?? 1000

    if (this.reconnectCount >= maxAttempts) {
      console.log('Max reconnection attempts reached')
      return
    }

    this.reconnectCount++
    const backoffDelay = delay * Math.pow(2, this.reconnectCount - 1)

    console.log(`Reconnecting in ${backoffDelay}ms (attempt ${this.reconnectCount}/${maxAttempts})`)

    this.reconnectTimeout = setTimeout(() => {
      this.connect()
    }, backoffDelay)
  }

  private cancelReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }
  }
}

/**
 * Default WebSocket URL based on environment.
 */
export function getWebSocketUrl(): string {
  // In development, use Vite proxy
  if (import.meta.env.DEV) {
    return `ws://${window.location.host}/game`
  }

  // In production, use same host with ws/wss based on protocol
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/game`
}
