/**
 * Direct messages from anywhere — in a game, a draft, the deckbuilder, the help pages. Mounted beside
 * the router (like the matchmaking layer), so a conversation stays open across a navigation.
 *
 * The way in is the chat button in each screen's top-bar chrome, beside fullscreen (`ChatButton`).
 * The panel drops down from it — your conversations, a friend picker for a new one, or a conversation
 * itself — and stays open while you keep playing, until you close it (×, Esc, or the button again).
 * On a phone it takes the screen. A screen with no chrome of its own (a draft) gets a button pinned
 * top-right instead.
 *
 * Alerts: an unread message puts a count on the button and a short preview drops down beneath it.
 * Away from the home screen both follow the "Message alerts away from home" preference (also a bell
 * in the panel); with it off they stay quiet there and the messages wait. Hidden on the Messages page
 * itself, which is the same conversations at full size.
 *
 * Key presses inside the panel stop at its edge, so typing a message never reaches the game's hotkeys.
 */
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import type React from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { ThreadSummary } from '@/api/messages'
import { Avatar, accountStyles as a } from '@/components/profile/accountUi'
import { useAuthStore } from '@/store/authStore'
import { useGameStore } from '@/store/gameStore'
import { useMessagesStore } from '@/store/messagesStore'
import { usePreferences } from '@/store/preferencesStore'
import { ChatButton } from './ChatButton'
import { useChatDock, useMessageAlertsOn } from './chatDockStore'
import { BackButton, ComposeIcon, ConversationPane, NewMessagePicker, ThreadRow, messagesStyles as m } from './conversation'
import d from './ChatDock.module.css'

/** With a socket, pushes keep the list current and the poll is only a catch-all. */
const POLL_INTERVAL_MS = 60_000
/** A page opened directly (no game connection) has no push: poll like the Messages page does. */
const OFFLINE_POLL_INTERVAL_MS = 15_000
const OFFLINE_CONVERSATION_POLL_MS = 6_000
const PREVIEW_MS = 6_000
/** A screen's own button mounts a frame after a navigation; don't flash the fallback meanwhile. */
const FALLBACK_DELAY_MS = 400
const GAP = 8

/** A new message someone sent you, as the preview shows it. */
interface Notice {
  readonly messageId: string
  readonly accountId: string
  readonly name: string
  readonly avatar: string | null | undefined
  readonly body: string
  /** The conversation is, for you, an unanswered message request. */
  readonly request: boolean
}

/** Where the panel and the preview hang: under the button, on the side of the screen it's on. */
interface Placement {
  readonly top: number
  readonly left?: number
  readonly right?: number
}

/** Pages where the dock would be redundant or in the way. */
function hiddenOn(pathname: string): boolean {
  return pathname.startsWith('/messages') || pathname.startsWith('/login')
}

export default function ChatDock() {
  const { pathname } = useLocation()
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const status = useAuthStore((s) => s.status)
  const user = useAuthStore((s) => s.user)
  const init = useAuthStore((s) => s.init)

  // A deep link to a page that doesn't check the session itself (the deckbuilder, help) still
  // finds out who's signed in.
  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  if (!accountsEnabled || status !== 'authenticated' || !user || hiddenOn(pathname)) return null
  return <Dock myId={user.id} />
}

function Dock({ myId }: { myId: string }) {
  const navigate = useNavigate()
  const open = useChatDock((s) => s.open)
  const view = useChatDock((s) => s.view)
  const show = useChatDock((s) => s.show)
  const close = useChatDock((s) => s.close)
  const setView = useChatDock((s) => s.setView)
  const anchor = useChatDock((s) => s.anchors[s.anchors.length - 1] ?? null)
  const [preview, setPreview] = useState<Notice | null>(null)
  const panelRef = useRef<HTMLDivElement | null>(null)

  const alerts = useMessageAlertsOn()
  const alertsAway = usePreferences((s) => s.prefs.messages.alertsAway)
  const updatePrefs = usePreferences((s) => s.update)

  const live = useGameStore((s) => s.connectionStatus === 'connected')
  const loadThreads = useMessagesStore((s) => s.loadThreads)
  const openConversation = useMessagesStore((s) => s.openConversation)
  const threads = useMessagesStore((s) => s.threads)
  const threadsLoaded = useMessagesStore((s) => s.threadsLoaded)
  const openChatId = open && view.kind === 'chat' ? view.accountId : null

  useEffect(() => {
    void loadThreads()
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') void loadThreads()
    }, live ? POLL_INTERVAL_MS : OFFLINE_POLL_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [live, loadThreads])

  useEffect(() => {
    if (live || !openChatId) return
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') void openConversation(openChatId)
    }, OFFLINE_CONVERSATION_POLL_MS)
    return () => window.clearInterval(timer)
  }, [live, openChatId, openConversation])

  // A new message previews under the button — unless it's already on screen, or alerts are off here.
  // Read off the thread list, which both the socket push and the poll refresh, so a page without a
  // game connection previews too.
  const seen = useRef<Map<string, string> | null>(null)
  useEffect(() => {
    if (!threadsLoaded) return
    const previous = seen.current
    seen.current = new Map(threads.map((t) => [t.other.accountId, t.lastMessage.id]))
    // The first list is history, not news.
    if (!previous || !alerts) return
    const fresh = newestIncoming(threads, previous)
    if (!fresh) return
    if (open && (view.kind === 'list' || openChatId === fresh.other.accountId)) return
    setPreview({
      messageId: fresh.lastMessage.id,
      accountId: fresh.other.accountId,
      name: fresh.other.displayName,
      avatar: fresh.other.avatar,
      body: fresh.lastMessage.body,
      request: fresh.state === 'INCOMING_REQUEST',
    })
  }, [threads, threadsLoaded, alerts, open, view.kind, openChatId])

  useEffect(() => {
    if (!alerts || open) setPreview(null)
  }, [alerts, open])

  // No chat button on this screen: after a beat, pin one top-right.
  const [fallback, setFallback] = useState(false)
  const hasScreenButton = useChatDock((s) => s.anchors.some((el) => !el.closest(`.${d.fallback}`)))
  useEffect(() => {
    if (hasScreenButton) {
      setFallback(false)
      return
    }
    const timer = window.setTimeout(() => setFallback(true), FALLBACK_DELAY_MS)
    return () => window.clearTimeout(timer)
  }, [hasScreenButton])

  // Hang the panel and the preview off the button; follow it through a resize.
  const [place, setPlace] = useState<Placement | null>(null)
  const floating = open || preview !== null
  useLayoutEffect(() => {
    if (!floating || !anchor) return
    const update = () => {
      const r = anchor.getBoundingClientRect()
      const top = r.bottom + GAP
      setPlace(r.left + r.width / 2 > window.innerWidth / 2
        ? { top, right: Math.max(GAP, window.innerWidth - r.right) }
        : { top, left: Math.max(GAP, r.left) })
    }
    update()
    window.addEventListener('resize', update)
    return () => window.removeEventListener('resize', update)
  }, [floating, anchor])

  // Focus follows the panel: into it on open (the composer or search take it themselves), back to
  // the button on close.
  const wasOpen = useRef(false)
  useEffect(() => {
    if (open && !wasOpen.current && view.kind === 'list') panelRef.current?.focus()
    if (!open && wasOpen.current) anchor?.focus({ preventScroll: true })
    wasOpen.current = open
  }, [open, view.kind, anchor])

  // Esc and outside clicks: Esc closes; a click elsewhere leaves it open, so you can play beside it.
  const onPanelKeyDown = (e: React.KeyboardEvent) => {
    // A confirm dialog portalled out of the panel bubbles here through React, not through the DOM.
    if (!panelRef.current?.contains(e.target as Node)) return
    e.stopPropagation()
    if (e.key === 'Escape') {
      e.preventDefault()
      close()
    }
  }

  const dismissPreview = useCallback(() => setPreview(null), [])
  const openFullPage = (path: string) => {
    close()
    navigate(path)
  }
  const toList = () => setView({ kind: 'list' })
  const closeButton = <IconButton label="Close messages" onClick={close}><CloseIcon /></IconButton>
  const placeStyle = (p: Placement | null): React.CSSProperties | undefined =>
    p ? { top: p.top, ...(p.left !== undefined ? { left: p.left } : { right: p.right }) } : undefined

  return (
    <>
      {fallback && (
        <div className={d.fallback}>
          <ChatButton />
        </div>
      )}

      {preview && !open && place && (
        <Preview
          notice={preview}
          style={placeStyle(place)}
          onOpen={() => show({ kind: 'chat', accountId: preview.accountId })}
          onDismiss={dismissPreview}
        />
      )}

      {open && place && (
        <div
          ref={panelRef}
          className={d.panel}
          style={{ ...placeStyle(place), '--panel-top': `${place.top}px` } as React.CSSProperties}
          role="dialog"
          aria-label="Messages"
          tabIndex={-1}
          onKeyDown={onPanelKeyDown}
        >
          {view.kind === 'chat' ? (
            <ConversationPane
              key={view.accountId}
              accountId={view.accountId}
              myId={myId}
              compact
              className={d.fill}
              onBack={toList}
              onLeave={toList}
              onAway={close}
              menuItems={[
                { label: 'View profile', onSelect: () => openFullPage(`/u/${view.accountId}`) },
                { label: 'Open in Messages', onSelect: () => openFullPage(`/messages/${view.accountId}`) },
              ]}
              headerEnd={closeButton}
            />
          ) : view.kind === 'new' ? (
            <div className={d.fill}>
              <header className={d.head}>
                <BackButton always onClick={toList} />
                <h2 className={d.title}>New message</h2>
                {closeButton}
              </header>
              <NewMessagePicker autoFocus onAway={close} onPick={(accountId) => setView({ kind: 'chat', accountId })} />
            </div>
          ) : (
            <div className={d.fill}>
              <header className={d.head}>
                <h2 className={d.title} data-lead>Messages</h2>
                <IconButton label="New message" onClick={() => setView({ kind: 'new' })}><ComposeIcon /></IconButton>
                <IconButton
                  label={alertsAway ? 'Alerts away from home: on' : 'Alerts away from home: off'}
                  pressed={alertsAway}
                  onClick={() => updatePrefs('messages', { alertsAway: !alertsAway })}
                >
                  <BellIcon off={!alertsAway} />
                </IconButton>
                <IconButton label="Open Messages page" onClick={() => openFullPage('/messages')}><ExpandIcon /></IconButton>
                {closeButton}
              </header>
              <ThreadPicker myId={myId} onOpen={(accountId) => setView({ kind: 'chat', accountId })} onCompose={() => setView({ kind: 'new' })} />
            </div>
          )}
        </div>
      )}
    </>
  )
}

/** The newest thread whose last message is theirs, unread, and wasn't its last message before. */
function newestIncoming(threads: readonly ThreadSummary[], previous: ReadonlyMap<string, string>): ThreadSummary | null {
  let best: ThreadSummary | null = null
  for (const t of threads) {
    if (t.unread === 0 || t.lastMessage.senderId !== t.other.accountId) continue
    if (previous.get(t.other.accountId) === t.lastMessage.id) continue
    if (!best || t.lastMessage.createdAt > best.lastMessage.createdAt) best = t
  }
  return best
}

/** Every conversation in one list — requests are marked in their row rather than on a tab. */
function ThreadPicker({ myId, onOpen, onCompose }: { myId: string; onOpen: (accountId: string) => void; onCompose: () => void }) {
  const threads = useMessagesStore((s) => s.threads)
  const loaded = useMessagesStore((s) => s.threadsLoaded)

  if (!loaded) {
    return <div className={m.listEmpty}><span className={a.spinner} aria-hidden /></div>
  }
  if (threads.length === 0) {
    return (
      <div className={m.listEmpty}>
        <p className={m.emptyTitle}>No conversations yet</p>
        <button type="button" className={d.textButton} onClick={onCompose}>Message a friend</button>
      </div>
    )
  }
  return (
    <div className={d.scroll}>
      <ul className={m.threadList}>
        {threads.map((t) => (
          <li key={t.other.accountId}>
            <ThreadRow thread={t} myId={myId} selected={false} onOpen={() => onOpen(t.other.accountId)} />
          </li>
        ))}
      </ul>
    </div>
  )
}

/** The new message, dropped down under the button for a few seconds. Hovering holds it. */
function Preview({ notice, style, onOpen, onDismiss }: { notice: Notice; style: React.CSSProperties | undefined; onOpen: () => void; onDismiss: () => void }) {
  const [held, setHeld] = useState(false)

  useEffect(() => {
    if (held) return
    const timer = window.setTimeout(onDismiss, PREVIEW_MS)
    return () => window.clearTimeout(timer)
  }, [held, notice.messageId, onDismiss])

  return (
    <div
      className={d.preview}
      style={style}
      role="status"
      aria-live="polite"
      onMouseEnter={() => setHeld(true)}
      onMouseLeave={() => setHeld(false)}
      onFocus={() => setHeld(true)}
      onBlur={() => setHeld(false)}
      data-testid="chat-dock-preview"
    >
      <button type="button" className={d.previewBody} onClick={onOpen}>
        <Avatar name={notice.name} avatar={notice.avatar} small />
        <span className={d.previewText}>
          <span className={d.previewName}>
            {notice.name}
            {notice.request && <span className={d.previewTag}>Request</span>}
          </span>
          <span className={d.previewMessage}>{notice.body}</span>
        </span>
      </button>
      <button type="button" className={d.previewClose} onClick={onDismiss} aria-label="Dismiss">
        <CloseIcon size={14} />
      </button>
    </div>
  )
}

function IconButton({ label, pressed, onClick, children }: { label: string; pressed?: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" className={m.iconButton} onClick={onClick} aria-label={label} title={label} aria-pressed={pressed}>
      {children}
    </button>
  )
}

function CloseIcon({ size = 18 }: { size?: number }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden>
      <path d="M6 6l12 12M18 6L6 18" />
    </svg>
  )
}

function ExpandIcon() {
  return (
    <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M14 4h6v6M20 4l-7 7M10 20H4v-6M4 20l7-7" />
    </svg>
  )
}

function BellIcon({ off }: { off: boolean }) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z" />
      <path d="M10 21a2 2 0 0 0 4 0" />
      {off && <path d="M4 4l16 16" />}
    </svg>
  )
}
