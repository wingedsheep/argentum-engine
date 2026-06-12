import type { ClientMessage, ServerMessage } from '@/types'

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected'

export interface WebSocketConfig {
  url: string
  onMessage: (message: ServerMessage) => void
  onStatusChange: (status: ConnectionStatus) => void
  onError: (error: Event) => void
  reconnectDelay?: number
}

/** Reconnect backoff is capped here; the client never stops retrying on its own. */
const MAX_RECONNECT_DELAY_MS = 30_000

/** How long after a ping probe before a silent server means the socket is half-open. */
const LIVENESS_TIMEOUT_MS = 5_000

/**
 * WebSocket manager for game server communication.
 *
 * Handles:
 * - Connection lifecycle
 * - Automatic reconnection (exponential backoff capped at 30s, retries indefinitely)
 * - Message serialization/deserialization
 * - Connection status tracking
 * - Tab visibility / network-online recovery: reconnects a dead socket immediately and
 *   probes an apparently-open one with a ping to detect half-open sockets after OS sleep
 * - State version gap detection
 */
export class GameWebSocket {
  private ws: WebSocket | null = null
  private config: WebSocketConfig
  private reconnectCount = 0
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null
  private isIntentionallyClosed = false

  /** Last received stateVersion from the server */
  private lastStateVersion: number | null = null
  /** Whether resync has already been requested (debounce) */
  private resyncRequested = false
  /** Bound handlers for cleanup */
  private visibilityHandler: (() => void) | null = null
  private onlineHandler: (() => void) | null = null

  /** Timestamp of the last message received from the server (liveness signal). */
  private lastMessageAt = 0
  /** Pending liveness verdict after a ping probe. */
  private livenessTimeout: ReturnType<typeof setTimeout> | null = null

  constructor(config: WebSocketConfig) {
    this.config = {
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

    // Drop a stale CONNECTING/CLOSING socket so its late events can't fire against
    // the new connection.
    this.discardSocket()

    this.isIntentionallyClosed = false
    this.config.onStatusChange('connecting')

    try {
      this.ws = new WebSocket(this.config.url)
      this.setupEventHandlers()
      this.setupRecoveryHandlers()
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
    this.cancelLivenessCheck()
    this.teardownRecoveryHandlers()
    // Detach handlers before closing: a message already queued on this socket must not
    // dispatch after we've decided the connection is over.
    this.discardSocket()

    this.lastStateVersion = null
    this.resyncRequested = false
    this.config.onStatusChange('disconnected')
  }

  /**
   * Tear down the current socket (if any) and reconnect immediately, resetting backoff.
   * Used when an external signal (tab visible, network back online, failed liveness
   * probe) tells us the connection is dead or suspect — waiting out a backoff timer
   * or trusting `readyState` would leave the user staring at a broken session.
   */
  forceReconnect(reason: string): void {
    if (this.isIntentionallyClosed) return
    console.log(`[WebSocket] Forcing reconnect: ${reason}`)
    this.cancelReconnect()
    this.cancelLivenessCheck()
    this.reconnectCount = 0
    this.discardSocket()
    this.connect()
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

  /**
   * Called by the store when a stateUpdate or stateDeltaUpdate is received.
   * Tracks version and requests resync if a gap is detected.
   */
  onStateVersionReceived(version: number | undefined): void {
    if (version === undefined) return

    if (this.lastStateVersion !== null && version > this.lastStateVersion + 1) {
      console.warn(
        `[WebSocket] State version gap detected: expected ${this.lastStateVersion + 1}, got ${version}. Requesting resync.`
      )
      this.lastStateVersion = version
      this.requestResync()
      return
    }

    this.lastStateVersion = version
    this.resyncRequested = false
  }

  /**
   * Request a full state resync from the server.
   */
  requestResync(): void {
    if (this.resyncRequested) return
    if (!this.isConnected()) return

    this.resyncRequested = true
    console.log('[WebSocket] Requesting state resync from server')
    this.send({ type: 'requestResync' })
  }

  private setupEventHandlers(): void {
    if (!this.ws) return

    this.ws.onopen = () => {
      console.log('WebSocket connected')
      this.reconnectCount = 0
      this.lastStateVersion = null
      this.resyncRequested = false
      this.lastMessageAt = Date.now()
      this.config.onStatusChange('connected')
    }

    this.ws.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason)
      this.cancelLivenessCheck()
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
      this.lastMessageAt = Date.now()
      try {
        const message = JSON.parse(event.data as string) as ServerMessage
        this.config.onMessage(message)
      } catch (error) {
        console.error('Failed to parse message:', error)
      }
    }
  }

  /** Detach handlers from the current socket and close it without status side effects. */
  private discardSocket(): void {
    const old = this.ws
    if (!old) return
    this.ws = null
    old.onopen = null
    old.onclose = null
    old.onerror = null
    old.onmessage = null
    try {
      old.close()
    } catch {
      // Already closed/closing — nothing to do.
    }
  }

  /**
   * Listen for tab visibility changes and the network coming back online.
   *
   * On either signal: if the socket is dead, reconnect immediately (background-tab timer
   * throttling can leave a scheduled reconnect pending long after the network is back).
   * If the socket claims to be open on tab return, request a resync to recover missed
   * messages — and verify the connection is actually alive with a ping probe, since a
   * socket can sit half-open after OS sleep without ever firing `close`.
   */
  private setupRecoveryHandlers(): void {
    this.teardownRecoveryHandlers()
    this.visibilityHandler = () => {
      if (document.visibilityState !== 'visible' || this.isIntentionallyClosed) return
      if (!this.isConnected()) {
        this.forceReconnect('tab became visible while disconnected')
        return
      }
      console.log('[WebSocket] Tab became visible, requesting resync')
      this.requestResync()
      this.startLivenessCheck()
    }
    this.onlineHandler = () => {
      if (this.isIntentionallyClosed || this.isConnected()) return
      this.forceReconnect('network came back online')
    }
    document.addEventListener('visibilitychange', this.visibilityHandler)
    window.addEventListener('online', this.onlineHandler)
  }

  private teardownRecoveryHandlers(): void {
    if (this.visibilityHandler) {
      document.removeEventListener('visibilitychange', this.visibilityHandler)
      this.visibilityHandler = null
    }
    if (this.onlineHandler) {
      window.removeEventListener('online', this.onlineHandler)
      this.onlineHandler = null
    }
  }

  /**
   * Send a ping and reconnect if the server stays silent. Any inbound message counts
   * as proof of life, not just the pong.
   */
  private startLivenessCheck(): void {
    if (this.livenessTimeout) return
    const probeSentAt = Date.now()
    this.send({ type: 'ping' })
    this.livenessTimeout = setTimeout(() => {
      this.livenessTimeout = null
      if (this.lastMessageAt < probeSentAt) {
        this.forceReconnect('no server response to liveness probe')
      }
    }, LIVENESS_TIMEOUT_MS)
  }

  private cancelLivenessCheck(): void {
    if (this.livenessTimeout) {
      clearTimeout(this.livenessTimeout)
      this.livenessTimeout = null
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimeout) return

    const delay = this.config.reconnectDelay ?? 1000
    this.reconnectCount++
    const backoffDelay = Math.min(delay * Math.pow(2, this.reconnectCount - 1), MAX_RECONNECT_DELAY_MS)

    console.log(`Reconnecting in ${backoffDelay}ms (attempt ${this.reconnectCount})`)

    this.reconnectTimeout = setTimeout(() => {
      this.reconnectTimeout = null
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
