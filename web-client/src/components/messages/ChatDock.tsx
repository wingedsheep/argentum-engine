/**
 * Direct messages from anywhere — in a game, a draft, the deckbuilder, the help pages. Mounted beside
 * the router (like the matchmaking layer), so a conversation stays open across a navigation.
 *
 * At rest it is only a sliver on the right edge of the screen: low contrast, out of the way of the
 * hand, the pass button and the corners where page chrome lives. Hovering or focusing it slides out a
 * small tab; clicking opens a narrow panel — your conversations, a friend picker for a new one, or a
 * conversation itself — which stays open while you keep playing, until you close it (×, Esc, or the
 * tab again). On a phone the panel takes the screen.
 *
 * Alerts: an unread message tints the sliver and a short preview slides out beside it. Away from the
 * home screen both follow the "Message alerts away from home" preference (also a bell in the panel);
 * with it off the sliver stays quiet there and the messages wait. Hidden on the Messages page itself,
 * which is the same conversations at full size.
 *
 * Key presses inside the panel stop at its edge, so typing a message never reaches the game's hotkeys.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import type React from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Avatar, accountStyles as a } from '@/components/profile/accountUi'
import { useHomeVisible } from '@/hooks/useHomeVisible'
import { useAuthStore } from '@/store/authStore'
import { useGameStore } from '@/store/gameStore'
import type { ThreadSummary } from '@/api/messages'
import { unreadBadgeCount, useMessagesStore } from '@/store/messagesStore'
import { usePreferences } from '@/store/preferencesStore'
import { BackButton, ComposeIcon, ConversationPane, NewMessagePicker, ThreadRow, messagesStyles as m } from './conversation'
import d from './ChatDock.module.css'

/** With a socket, pushes keep the list current and the poll is only a catch-all. */
const POLL_INTERVAL_MS = 60_000
/** A page opened directly (no game connection) has no push: poll like the Messages page does. */
const OFFLINE_POLL_INTERVAL_MS = 15_000
const OFFLINE_CONVERSATION_POLL_MS = 6_000
const PREVIEW_MS = 6_000

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

type View = { readonly kind: 'list' } | { readonly kind: 'new' } | { readonly kind: 'chat'; readonly accountId: string }

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
  const [open, setOpen] = useState(false)
  const [view, setView] = useState<View>({ kind: 'list' })
  const [preview, setPreview] = useState<Notice | null>(null)
  const handleRef = useRef<HTMLButtonElement | null>(null)
  const panelRef = useRef<HTMLDivElement | null>(null)

  const homeVisible = useHomeVisible()
  const alertsAway = usePreferences((s) => s.prefs.messages.alertsAway)
  const updatePrefs = usePreferences((s) => s.update)
  const alerts = homeVisible || alertsAway

  const live = useGameStore((s) => s.connectionStatus === 'connected')
  const loadThreads = useMessagesStore((s) => s.loadThreads)
  const openConversation = useMessagesStore((s) => s.openConversation)
  const threads = useMessagesStore((s) => s.threads)
  const threadsLoaded = useMessagesStore((s) => s.threadsLoaded)
  const openChatId = open && view.kind === 'chat' ? view.accountId : null
  // The conversation on screen is being read; it doesn't count.
  const unread = useMessagesStore((s) =>
    unreadBadgeCount(s.threads.filter((t) => t.other.accountId !== openChatId)),
  )

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

  // A new message previews beside the tab — unless it's already on screen, or alerts are off here.
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
    if (!alerts) setPreview(null)
  }, [alerts])

  const show = (next: View) => {
    setView(next)
    setOpen(true)
    setPreview(null)
  }
  const close = () => setOpen(false)
  const dismissPreview = useCallback(() => setPreview(null), [])

  // Focus follows the panel: into it on open (the composer or search take it themselves), back to
  // the tab on close.
  const wasOpen = useRef(false)
  useEffect(() => {
    if (open && !wasOpen.current && view.kind === 'list') panelRef.current?.focus()
    if (!open && wasOpen.current) handleRef.current?.focus({ preventScroll: true })
    wasOpen.current = open
  }, [open, view.kind])

  const onPanelKeyDown = (e: React.KeyboardEvent) => {
    // A confirm dialog portalled out of the panel bubbles here through React, not through the DOM.
    if (!panelRef.current?.contains(e.target as Node)) return
    e.stopPropagation()
    if (e.key === 'Escape') {
      e.preventDefault()
      close()
    }
  }

  const away = () => setOpen(false)
  const openFullPage = (path: string) => {
    setOpen(false)
    navigate(path)
  }

  const showCount = alerts && unread > 0
  const closeButton = <IconButton label="Close messages" onClick={close}><CloseIcon /></IconButton>

  return (
    <>
      <button
        ref={handleRef}
        type="button"
        className={d.handle}
        data-hidden={open}
        data-alert={showCount}
        onClick={() => (open ? close() : show(view))}
        aria-label={showCount ? `Messages, ${unread} unread` : 'Messages'}
        aria-expanded={open}
        title={showCount ? `Messages · ${unread} unread` : 'Messages'}
        data-testid="chat-dock-handle"
      >
        <span className={d.tab} aria-hidden>
          <ChatIcon />
          {showCount && <span className={d.count}>{unread > 99 ? '99+' : unread}</span>}
        </span>
        {/* Re-keyed on every arrival so the ping replays. */}
        {preview && <span key={preview.messageId} className={d.ping} aria-hidden />}
      </button>

      {preview && !open && (
        <Preview notice={preview} onOpen={() => show({ kind: 'chat', accountId: preview.accountId })} onDismiss={dismissPreview} />
      )}

      {open && (
        <div ref={panelRef} className={d.panel} role="dialog" aria-label="Messages" tabIndex={-1} onKeyDown={onPanelKeyDown}>
          {view.kind === 'chat' ? (
            <ConversationPane
              key={view.accountId}
              accountId={view.accountId}
              myId={myId}
              compact
              className={d.fill}
              onBack={() => setView({ kind: 'list' })}
              onLeave={() => setView({ kind: 'list' })}
              onAway={away}
              menuItems={[
                { label: 'View profile', onSelect: () => openFullPage(`/u/${view.accountId}`) },
                { label: 'Open in Messages', onSelect: () => openFullPage(`/messages/${view.accountId}`) },
              ]}
              headerEnd={closeButton}
            />
          ) : view.kind === 'new' ? (
            <div className={d.fill}>
              <header className={d.head}>
                <BackButton always onClick={() => setView({ kind: 'list' })} />
                <h2 className={d.title}>New message</h2>
                {closeButton}
              </header>
              <NewMessagePicker autoFocus onAway={away} onPick={(accountId) => setView({ kind: 'chat', accountId })} />
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

/** The new message, slid out beside the tab for a few seconds. Hovering holds it. */
function Preview({ notice, onOpen, onDismiss }: { notice: Notice; onOpen: () => void; onDismiss: () => void }) {
  const [held, setHeld] = useState(false)

  useEffect(() => {
    if (held) return
    const timer = window.setTimeout(onDismiss, PREVIEW_MS)
    return () => window.clearTimeout(timer)
  }, [held, notice.messageId, onDismiss])

  return (
    <div
      className={d.preview}
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

function ChatIcon() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
    </svg>
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
