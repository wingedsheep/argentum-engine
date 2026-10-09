/**
 * The pieces of a direct-message conversation, shared by the Messages page and the chat dock that
 * follows the player across every other screen (see ChatDock): a thread row, the open conversation
 * (history, composer, request bar, delete/block) and the friend picker that starts a new one.
 *
 * Nothing here knows which surface it's in. Navigation is the caller's: `onBack` returns to the list,
 * `onLeave` runs after the conversation was deleted or blocked, `onAway` when a link takes the player
 * to another page. `compact` is the dock's narrower layout.
 */
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import type React from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import type { ThreadSummary } from '@/api/messages'
import { AccountModal, Avatar, accountStyles as a } from '@/components/profile/accountUi'
import { pageStyles as p } from '@/components/ui/PageShell'
import { useFriendsStore } from '@/store/friendsStore'
import { type ChatMessage, type OpenConversation, useMessagesStore } from '@/store/messagesStore'
import { dayLabel, shortTime, sinceLabel } from '@/pages/messagesTime'
import m from './Messages.module.css'

export { m as messagesStyles }

/** Consecutive messages from one sender closer than this share a group (one timestamp). */
const GROUP_GAP_MS = 5 * 60_000

/** Unsent text per conversation, so switching chats — or surfaces — doesn't lose a half-written message. */
const drafts = new Map<string, string>()

export interface MenuItem {
  readonly label: string
  readonly danger?: boolean
  readonly onSelect: () => void
}

// ── Conversation list ───────────────────────────────────────────────────────

export function ThreadRow({ thread, myId, selected, onOpen }: { thread: ThreadSummary; myId: string; selected: boolean; onOpen: () => void }) {
  const mine = thread.lastMessage.senderId === myId
  const unread = thread.unread > 0 && !selected
  return (
    <button type="button" className={m.threadRow} data-selected={selected} data-unread={unread} onClick={onOpen} aria-current={selected ? 'true' : undefined}>
      <Avatar name={thread.other.displayName} avatar={thread.other.avatar} small online={thread.isFriend ? thread.other.online : undefined} />
      <span className={m.threadText}>
        <span className={m.threadTop}>
          <span className={m.threadName}>{thread.other.displayName}</span>
          <span className={m.threadTime}>{sinceLabel(thread.lastMessage.createdAt)}</span>
        </span>
        <span className={m.threadBottom}>
          <span className={m.threadPreview}>
            {thread.state === 'INCOMING_REQUEST' ? <span className={m.pendingNote}>Request · </span>
              : thread.state === 'OUTGOING_REQUEST' ? <span className={m.pendingNote}>Request sent · </span>
                : mine ? 'You: ' : ''}
            {thread.lastMessage.body}
          </span>
          {unread && <span className={m.unreadBadge} aria-label={`${thread.unread} unread`}>{thread.unread > 99 ? '99+' : thread.unread}</span>}
        </span>
      </span>
    </button>
  )
}

// ── New message ─────────────────────────────────────────────────────────────

/**
 * Pick a friend to write to — including the ones you've never messaged, who have no thread in the
 * list yet. Online friends first; a friend you already talk to opens that conversation.
 */
export function NewMessagePicker({ onPick, autoFocus = false, onAway }: { onPick: (accountId: string) => void; autoFocus?: boolean; onAway?: () => void }) {
  const friends = useFriendsStore((s) => s.friends)
  const loading = useFriendsStore((s) => s.loading)
  const loadFriends = useFriendsStore((s) => s.load)
  const threads = useMessagesStore((s) => s.threads)
  const [query, setQuery] = useState('')
  const inputRef = useRef<HTMLInputElement | null>(null)

  useEffect(() => {
    void loadFriends()
  }, [loadFriends])
  useEffect(() => {
    if (autoFocus) inputRef.current?.focus()
  }, [autoFocus])

  const talkingTo = useMemo(() => new Set(threads.map((t) => t.other.accountId)), [threads])
  const q = query.trim().toLowerCase()
  const shown = useMemo(
    () =>
      friends
        .filter((f) => !q || f.displayName.toLowerCase().includes(q))
        .sort((x, y) => Number(y.online) - Number(x.online) || x.displayName.localeCompare(y.displayName)),
    [friends, q],
  )

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && shown.length > 0) {
      e.preventDefault()
      onPick(shown[0]!.accountId)
    }
  }

  return (
    <div className={m.picker}>
      {friends.length > 0 && (
        <div className={m.pickerSearch}>
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden>
            <circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" />
          </svg>
          <input
            ref={inputRef}
            className={m.pickerInput}
            type="search"
            placeholder="Search friends"
            aria-label="Search friends"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={onKeyDown}
          />
        </div>
      )}
      <div className={m.pickerScroll}>
        {friends.length === 0 ? (
          <div className={m.listEmpty}>
            {loading ? (
              <span className={a.spinner} aria-hidden />
            ) : (
              <>
                <p className={m.emptyTitle}>No friends yet</p>
                <p className={a.muted}>
                  Add friends from your <Link className={a.link} to="/friends" onClick={onAway}>friends list</Link>, or message
                  any player from their profile.
                </p>
              </>
            )}
          </div>
        ) : shown.length === 0 ? (
          <div className={m.listEmpty}>
            <p className={a.muted}>No friend matches “{query.trim()}”.</p>
          </div>
        ) : (
          <ul className={m.threadList} aria-label="Friends">
            {shown.map((f) => (
              <li key={f.accountId}>
                <button type="button" className={m.threadRow} onClick={() => onPick(f.accountId)}>
                  <Avatar name={f.displayName} avatar={f.avatar} small online={f.online} />
                  <span className={m.threadText}>
                    <span className={m.threadName}>{f.displayName}</span>
                    <span className={m.pickerStatus} data-online={f.online}>
                      {f.online ? 'Online' : 'Offline'}
                      {talkingTo.has(f.accountId) ? ' · in your chats' : ''}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

// ── Open conversation ───────────────────────────────────────────────────────

export interface ConversationPaneProps {
  readonly accountId: string
  readonly myId: string
  /** Back to the conversation list. The page shows the button on phones only; the dock always. */
  readonly onBack: () => void
  /** After the conversation is deleted or its player blocked. */
  readonly onLeave: () => void
  /** A link is taking the player to another page (their profile). */
  readonly onAway?: (() => void) | undefined
  /** The dock's narrow layout. */
  readonly compact?: boolean
  /** Extra overflow-menu items, after View profile. */
  readonly menuItems?: readonly MenuItem[]
  /** Controls at the right end of the header, after the overflow menu (the dock's close button). */
  readonly headerEnd?: React.ReactNode
  readonly className?: string | undefined
}

export function ConversationPane(props: ConversationPaneProps) {
  const { accountId, onBack, compact = false, headerEnd, className = '' } = props
  const entry = useMessagesStore((s) => s.conversations[accountId])
  const openConversation = useMessagesStore((s) => s.openConversation)
  const markRead = useMessagesStore((s) => s.markRead)
  const threadUnread = useMessagesStore((s) => s.threads.find((t) => t.other.accountId === accountId)?.unread ?? 0)

  useEffect(() => {
    void openConversation(accountId)
  }, [accountId, openConversation])

  // Reading the open conversation clears its unread count — only while the tab is actually visible.
  const ready = entry?.status === 'ready'
  const isRequest = ready && entry.state === 'INCOMING_REQUEST'
  useEffect(() => {
    if (!ready || isRequest || threadUnread === 0) return
    const readIfVisible = () => {
      if (document.visibilityState === 'visible') markRead(accountId)
    }
    readIfVisible()
    document.addEventListener('visibilitychange', readIfVisible)
    return () => document.removeEventListener('visibilitychange', readIfVisible)
  }, [ready, isRequest, threadUnread, accountId, markRead])

  if (entry?.status === 'ready') return <ReadyConversation {...props} conversation={entry} />

  const frame = `${m.convPane} ${m.emptyPane} ${className}`
  if (!entry || entry.status === 'loading') {
    return (
      <section className={frame} data-compact={compact}>
        <div className={m.floatingHead}>
          <BackButton onClick={onBack} />
          {headerEnd}
        </div>
        <span className={a.spinner} aria-hidden />
      </section>
    )
  }
  return (
    <section className={frame} data-compact={compact}>
      <div className={m.floatingHead}>
        <BackButton onClick={onBack} />
        {headerEnd}
      </div>
      <div className={m.emptyInner}>
        <h2 className={m.emptyHeading}>{entry.status === 'unavailable' ? 'Can’t message this player' : 'Couldn’t load this conversation'}</h2>
        <p className={a.muted}>
          {entry.status === 'unavailable' ? 'This account isn’t available to message.' : entry.message}
        </p>
        {entry.status === 'error' && (
          <button type="button" className={p.button} onClick={() => void openConversation(accountId)}>
            Try again
          </button>
        )}
      </div>
    </section>
  )
}

/** Shown on phones only, where the list and the conversation take turns — unless [always]. */
export function BackButton({ onClick, always = false, label = 'Back to conversations' }: { onClick: () => void; always?: boolean; label?: string }) {
  return (
    <button type="button" onClick={onClick} className={m.back} data-always={always} aria-label={label} title={label}>
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
        <path d="M15 18l-6-6 6-6" />
      </svg>
    </button>
  )
}

function ReadyConversation({
  accountId,
  myId,
  onBack,
  onLeave,
  onAway,
  compact = false,
  menuItems = [],
  headerEnd,
  className = '',
  conversation: c,
}: ConversationPaneProps & { conversation: OpenConversation }) {
  const accept = useMessagesStore((s) => s.accept)
  const remove = useMessagesStore((s) => s.remove)
  const block = useMessagesStore((s) => s.block)
  const loadOlder = useMessagesStore((s) => s.loadOlder)
  const [confirm, setConfirm] = useState<'delete' | 'block' | null>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const name = c.other.displayName

  const run = async (action: () => Promise<void>, after?: () => void) => {
    setBusy(true)
    setActionError(null)
    try {
      await action()
      setConfirm(null)
      after?.()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Something went wrong.')
    } finally {
      setBusy(false)
    }
  }

  const statusLine = c.isFriend
    ? c.other.online ? 'Online' : 'Friend'
    : 'Not on your friends list'

  return (
    <section className={`${m.convPane} ${className}`} data-compact={compact} aria-label={`Conversation with ${name}`}>
      <header className={m.convHead}>
        <BackButton onClick={onBack} />
        <Link to={`/u/${accountId}`} className={m.convWho} title="View profile" onClick={onAway}>
          <Avatar name={name} avatar={c.other.avatar} small online={c.isFriend ? c.other.online : undefined} />
          <span className={m.convWhoText}>
            <span className={m.convName}>{name}</span>
            <span className={m.convStatus} data-online={c.isFriend && c.other.online}>{statusLine}</span>
          </span>
        </Link>
        <OverflowMenu
          items={[
            ...menuItems,
            ...(c.messages.length > 0 ? [{ label: 'Delete conversation', onSelect: () => setConfirm('delete') }] : []),
            ...(c.messages.length > 0 ? [{ label: `Block ${name}`, danger: true, onSelect: () => setConfirm('block') }] : []),
          ]}
        />
        {headerEnd}
      </header>

      <MessageList conversation={c} myId={myId} onLoadOlder={() => void loadOlder(accountId)} />

      {c.state === 'INCOMING_REQUEST' ? (
        <div className={m.requestBar}>
          <p className={m.requestText}>
            <strong>{name}</strong> isn’t on your friends list. Accept the request to reply.
          </p>
          <div className={m.requestActions}>
            <button type="button" className={p.buttonPrimary} disabled={busy} onClick={() => void run(() => accept(accountId))}>
              Accept
            </button>
            <button type="button" className={p.button} disabled={busy} onClick={() => setConfirm('delete')}>
              Delete
            </button>
            <button type="button" className={`${p.buttonGhost} ${m.danger}`} disabled={busy} onClick={() => setConfirm('block')}>
              Block
            </button>
          </div>
          {actionError && <p className={a.error}>{actionError}</p>}
        </div>
      ) : (
        <Composer accountId={accountId} myId={myId} conversation={c} />
      )}

      {confirm === 'delete' && (
        <ConfirmDialog
          title={c.state === 'INCOMING_REQUEST' ? 'Delete this request?' : 'Delete this conversation?'}
          body={
            c.state === 'INCOMING_REQUEST'
              ? `It’s removed from your requests. ${name} isn’t told, and could message you again — block them to stop that.`
              : `It’s removed for you only — ${name} keeps their copy. If either of you writes again, it starts fresh.`
          }
          confirmLabel="Delete"
          busy={busy}
          error={actionError}
          onCancel={() => { setConfirm(null); setActionError(null) }}
          onConfirm={() => void run(() => remove(accountId), onLeave)}
        />
      )}
      {confirm === 'block' && (
        <ConfirmDialog
          title={`Block ${name}?`}
          body={`They won’t be able to message you, you won’t be matched against each other, and their emotes won’t reach you. ${c.isFriend ? 'They’ll also be removed from your friends. ' : ''}They aren’t told. You can unblock them from your Friends page.`}
          confirmLabel="Block"
          danger
          busy={busy}
          error={actionError}
          onCancel={() => { setConfirm(null); setActionError(null) }}
          onConfirm={() => void run(() => block(accountId), onLeave)}
        />
      )}
    </section>
  )
}

/** The scrolling message history: day separators, sender groups, load-older at the top. */
function MessageList({ conversation: c, myId, onLoadOlder }: { conversation: OpenConversation; myId: string; onLoadOlder: () => void }) {
  const scrollRef = useRef<HTMLDivElement | null>(null)
  const nearBottom = useRef(true)
  const prevHeight = useRef(0)
  const prevFirstId = useRef<string | undefined>(undefined)
  const prevCount = useRef(0)

  const onScroll = () => {
    const el = scrollRef.current
    if (!el) return
    nearBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 80
    if (el.scrollTop < 60 && c.hasMore && !c.loadingOlder) onLoadOlder()
  }

  const prevLastId = useRef<string | undefined>(undefined)

  // Older messages prepended keep the reader's place; new ones at the bottom follow it when they
  // were already at the bottom (or sent the message themselves).
  useLayoutEffect(() => {
    const el = scrollRef.current
    if (!el) return
    const firstId = c.messages[0]?.id
    const last = c.messages[c.messages.length - 1]
    const prepended = prevCount.current > 0 && firstId !== prevFirstId.current && last?.id === prevLastId.current
    if (prepended) {
      el.scrollTop += el.scrollHeight - prevHeight.current
    } else if (prevCount.current === 0 || nearBottom.current || last?.senderId === myId) {
      el.scrollTop = el.scrollHeight
    }
    prevFirstId.current = firstId
    prevLastId.current = last?.id
    prevCount.current = c.messages.length
    prevHeight.current = el.scrollHeight
  }, [c.messages, myId])

  const groups = useMemo(() => groupMessages(c.messages), [c.messages])

  return (
    <div className={m.messages} ref={scrollRef} onScroll={onScroll} role="log" aria-live="polite">
      {c.hasMore && (
        <div className={m.olderRow}>
          <button type="button" className={a.miniButton} disabled={c.loadingOlder} onClick={onLoadOlder}>
            {c.loadingOlder ? 'Loading…' : 'Load older messages'}
          </button>
        </div>
      )}
      {c.messages.length === 0 && (
        <div className={m.convIntro}>
          <Avatar name={c.other.displayName} avatar={c.other.avatar} />
          <p className={m.introName}>{c.other.displayName}</p>
          <p className={a.muted}>
            {c.isFriend
              ? 'This is the start of your conversation.'
              : `${c.other.displayName} isn’t on your friends list, so your first messages arrive as a request they can accept.`}
          </p>
        </div>
      )}
      {groups.map((g) =>
        g.kind === 'day' ? (
          <div key={g.key} className={m.daySep}><span>{g.label}</span></div>
        ) : (
          <div key={g.key} className={m.group} data-mine={g.senderId === myId}>
            {g.messages.map((msg) => (
              <div key={msg.id} className={m.bubble} data-mine={msg.senderId === myId} data-pending={!!msg.pending} title={new Date(msg.createdAt).toLocaleString()}>
                {msg.body}
              </div>
            ))}
            <GroupTime last={g.messages[g.messages.length - 1]} />
          </div>
        ),
      )}
    </div>
  )
}

function GroupTime({ last }: { last: ChatMessage | undefined }) {
  if (!last) return null
  return <span className={m.groupTime}>{last.pending ? 'Sending…' : shortTime(last.createdAt)}</span>
}

type Group =
  | { kind: 'day'; key: string; label: string }
  | { kind: 'messages'; key: string; senderId: string; messages: ChatMessage[] }

function groupMessages(messages: ChatMessage[]): Group[] {
  const out: Group[] = []
  let lastDay = ''
  let current: Extract<Group, { kind: 'messages' }> | null = null
  for (const msg of messages) {
    const day = new Date(msg.createdAt).toDateString()
    if (day !== lastDay) {
      out.push({ kind: 'day', key: `day-${day}`, label: dayLabel(msg.createdAt) })
      lastDay = day
      current = null
    }
    const prev = current?.messages[current.messages.length - 1]
    if (current && prev && current.senderId === msg.senderId && Date.parse(msg.createdAt) - Date.parse(prev.createdAt) < GROUP_GAP_MS) {
      current.messages.push(msg)
    } else {
      current = { kind: 'messages', key: `g-${msg.id}`, senderId: msg.senderId, messages: [msg] }
      out.push(current)
    }
  }
  return out
}

function Composer({ accountId, myId, conversation: c }: { accountId: string; myId: string; conversation: OpenConversation }) {
  const send = useMessagesStore((s) => s.send)
  const [text, setText] = useState(() => drafts.get(accountId) ?? '')
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLTextAreaElement | null>(null)
  const outgoingRequest = c.state === 'OUTGOING_REQUEST'
  const left = c.requestMessagesLeft ?? 0
  const blockedByRequest = outgoingRequest && left <= 0
  const trimmed = text.trim()
  const tooLong = trimmed.length > c.maxLength

  useEffect(() => {
    if (!blockedByRequest) inputRef.current?.focus()
  }, [blockedByRequest])

  // Grow with the text up to a cap, then scroll.
  useLayoutEffect(() => {
    const el = inputRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`
  }, [text])

  const update = (value: string) => {
    setText(value)
    setError(null)
    if (value) drafts.set(accountId, value)
    else drafts.delete(accountId)
  }

  const submit = async () => {
    if (!trimmed || tooLong || blockedByRequest) return
    update('')
    try {
      await send(accountId, myId, trimmed)
    } catch (e) {
      update(trimmed)
      setError(e instanceof Error ? e.message : 'Couldn’t send the message.')
    }
  }

  const onKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      void submit()
    }
  }

  if (blockedByRequest) {
    return (
      <div className={m.composerNote}>
        <p className={a.muted}>Waiting for <strong className={m.strong}>{c.other.displayName}</strong> to accept your message request.</p>
      </div>
    )
  }

  return (
    <div className={m.composerWrap}>
      {outgoingRequest && (
        <p className={m.requestHint}>
          {c.messages.length === 0
            ? `This will arrive as a message request — you can send up to ${left} ${left === 1 ? 'message' : 'messages'} until they accept.`
            : `Sent as a message request. You can send ${left} more until they accept.`}
        </p>
      )}
      <div className={m.composer}>
        <textarea
          ref={inputRef}
          className={m.input}
          rows={1}
          placeholder={`Message ${c.other.displayName}`}
          aria-label={`Message ${c.other.displayName}`}
          value={text}
          maxLength={c.maxLength + 200}
          onChange={(e) => update(e.target.value)}
          onKeyDown={onKeyDown}
        />
        <button type="button" className={m.send} disabled={!trimmed || tooLong} onClick={() => void submit()} aria-label="Send">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M5 12h13M13 6l6 6-6 6" />
          </svg>
        </button>
      </div>
      <div className={m.composerMeta}>
        {error ? <span className={a.error}>{error}</span> : <span className={m.hint}>Enter to send · Shift+Enter for a new line</span>}
        {trimmed.length > c.maxLength * 0.8 && (
          <span className={m.charCount} data-over={tooLong}>{trimmed.length}/{c.maxLength}</span>
        )}
      </div>
    </div>
  )
}

// ── Small pieces ────────────────────────────────────────────────────────────

function OverflowMenu({ items }: { items: readonly MenuItem[] }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement | null>(null)
  useEffect(() => {
    if (!open) return
    const close = (e: PointerEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', close)
    return () => document.removeEventListener('pointerdown', close)
  }, [open])

  if (items.length === 0) return null

  // Escape is handled here rather than on the document so it closes only the menu — the dock
  // around it closes on the next one.
  const onKeyDown = (e: React.KeyboardEvent) => {
    if (open && e.key === 'Escape') {
      e.stopPropagation()
      setOpen(false)
    }
  }

  return (
    <div className={m.menuRoot} ref={rootRef} onKeyDown={onKeyDown}>
      <button type="button" className={m.menuButton} aria-haspopup="menu" aria-expanded={open} aria-label="Conversation options" onClick={() => setOpen((o) => !o)}>
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden>
          <circle cx="5" cy="12" r="1.8" /><circle cx="12" cy="12" r="1.8" /><circle cx="19" cy="12" r="1.8" />
        </svg>
      </button>
      {open && (
        <div className={m.menu} role="menu">
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={m.menuItem}
              data-danger={!!item.danger}
              onClick={() => {
                setOpen(false)
                item.onSelect()
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function ConfirmDialog({
  title,
  body,
  confirmLabel,
  danger = false,
  busy,
  error,
  onCancel,
  onConfirm,
}: {
  title: string
  body: string
  confirmLabel: string
  danger?: boolean
  busy: boolean
  error: string | null
  onCancel: () => void
  onConfirm: () => void
}) {
  // Portalled: the glass panel's backdrop-filter would otherwise contain the fixed backdrop.
  return createPortal(
    <AccountModal title={title} size="small" onClose={onCancel}>
      <p className={m.confirmBody}>{body}</p>
      {error && <p className={a.error}>{error}</p>}
      <div className={m.confirmActions}>
        <button type="button" className={p.buttonGhost} onClick={onCancel} disabled={busy}>
          Cancel
        </button>
        <button type="button" className={danger ? m.dangerButton : p.buttonPrimary} onClick={onConfirm} disabled={busy} autoFocus>
          {busy ? `${confirmLabel}…` : confirmLabel}
        </button>
      </div>
    </AccountModal>,
    document.body,
  )
}

export function ComposeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" />
    </svg>
  )
}
