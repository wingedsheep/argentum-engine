/**
 * Direct messages, at /messages and /messages/:accountId.
 *
 * Two panes: your conversations (with a separate Requests tab for messages from players who aren't
 * your friends) and the open conversation. On a phone only one pane shows at a time — the list at
 * /messages, the conversation at /messages/:id with a back button.
 *
 * A conversation from a non-friend arrives as a request: until you accept it there's no composer,
 * just Accept / Delete / Block. A conversation you start with a non-friend is a request on their
 * side, and the composer says how many messages you have left until they answer. New messages
 * arrive over the WebSocket (see messagesStore); a slow poll is the catch-all, and a quick one stands
 * in for the socket when this page was opened directly and no game connection exists.
 */
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import type React from 'react'
import { createPortal } from 'react-dom'
import { Link, useNavigate, useParams } from 'react-router-dom'
import type { ThreadSummary } from '@/api/messages'
import { LoginModal } from '@/components/auth/LoginModal'
import { AccountModal, Avatar, MessageCard, accountStyles as a } from '@/components/profile/accountUi'
import { PageShell, pageStyles as p } from '@/components/ui/PageShell'
import { useAuthStore } from '@/store/authStore'
import { useGameStore } from '@/store/gameStore'
import { useFriendsStore } from '@/store/friendsStore'
import { type ChatMessage, type OpenConversation, useMessagesStore } from '@/store/messagesStore'
import { dayLabel, shortTime, sinceLabel } from './messagesTime'
import m from './MessagesPage.module.css'

const POLL_INTERVAL_MS = 30_000
/** Without a socket there's no push, so poll quickly enough to feel like a conversation. */
const OFFLINE_POLL_INTERVAL_MS = 6_000
/** Consecutive messages from one sender closer than this share a group (one timestamp). */
const GROUP_GAP_MS = 5 * 60_000

/** Unsent text per conversation, so switching chats doesn't lose a half-written message. */
const drafts = new Map<string, string>()

export function MessagesPage() {
  const { accountId } = useParams<{ accountId: string }>()
  const user = useAuthStore((s) => s.user)
  const status = useAuthStore((s) => s.status)
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const loadThreads = useMessagesStore((s) => s.loadThreads)
  const openConversation = useMessagesStore((s) => s.openConversation)
  const live = useGameStore((s) => s.connectionStatus === 'connected')
  const [loginOpen, setLoginOpen] = useState(false)

  useEffect(() => {
    if (status !== 'authenticated') return
    void loadThreads()
    const timer = window.setInterval(() => {
      if (document.visibilityState !== 'visible') return
      void loadThreads()
      if (!live && accountId) void openConversation(accountId)
    }, live ? POLL_INTERVAL_MS : OFFLINE_POLL_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [status, live, accountId, loadThreads, openConversation])

  if (status === 'authenticated' && user) {
    return (
      <PageShell title="Messages" width="wide" fit>
        <div className={m.layout} data-open={!!accountId}>
          <ThreadList selectedId={accountId} myId={user.id} />
          {accountId && accountId !== user.id ? (
            <ConversationPane key={accountId} accountId={accountId} myId={user.id} />
          ) : (
            <EmptyPane />
          )}
        </div>
      </PageShell>
    )
  }

  const resolving = status === 'idle' || status === 'loading'
  return (
    <PageShell title="Messages">
      <MessageCard>
        <h1 className={p.h1}>Messages</h1>
        {accountsEnabled ? (
          <>
            <p className={p.lede}>Sign in to message your friends and the players you meet.</p>
            <button type="button" className={`${p.buttonPrimary} ${a.fullButton}`} onClick={() => setLoginOpen(true)}>
              Sign in
            </button>
            <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
          </>
        ) : resolving ? (
          <p className={a.muted}>Loading…</p>
        ) : (
          <p className={a.muted}>Accounts aren’t available on this server.</p>
        )}
      </MessageCard>
    </PageShell>
  )
}

// ── Conversation list ───────────────────────────────────────────────────────

function ThreadList({ selectedId, myId }: { selectedId: string | undefined; myId: string }) {
  const navigate = useNavigate()
  const threads = useMessagesStore((s) => s.threads)
  const loaded = useMessagesStore((s) => s.threadsLoaded)
  const chats = threads.filter((t) => t.state !== 'INCOMING_REQUEST')
  const requests = threads.filter((t) => t.state === 'INCOMING_REQUEST')
  const selectedTab = requests.some((t) => t.other.accountId === selectedId)
    ? 'requests'
    : chats.some((t) => t.other.accountId === selectedId) ? 'chats' : null
  const [tab, setTab] = useState<'chats' | 'requests'>(selectedTab ?? 'chats')

  // The open conversation's tab follows it — a request opened from a link, or one just accepted.
  useEffect(() => {
    if (selectedTab) setTab(selectedTab)
  }, [selectedTab])

  const shown = tab === 'chats' ? chats : requests
  const unreadRequests = requests.filter((t) => t.unread > 0).length

  return (
    <aside className={`${p.panel} ${m.listPane}`} aria-label="Conversations">
      <div className={m.listHead}>
        <h1 className={m.listTitle}>Messages</h1>
      </div>
      <div className={m.tabs} role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'chats'} className={m.tab} onClick={() => setTab('chats')}>
          Chats
        </button>
        <button type="button" role="tab" aria-selected={tab === 'requests'} className={m.tab} onClick={() => setTab('requests')}>
          Requests
          {requests.length > 0 && (
            <span className={m.tabCount} data-unread={unreadRequests > 0}>{requests.length}</span>
          )}
        </button>
      </div>
      <div className={m.threadScroll}>
        {!loaded ? (
          <div className={m.listEmpty}><span className={a.spinner} aria-hidden /></div>
        ) : shown.length === 0 ? (
          <div className={m.listEmpty}>
            {tab === 'chats' ? (
              <>
                <p className={m.emptyTitle}>No conversations yet</p>
                <p className={a.muted}>Message a friend from your <Link className={a.link} to="/friends">friends list</Link> or any player’s profile.</p>
                {requests.length > 0 && (
                  <button type="button" className={a.link} onClick={() => setTab('requests')}>
                    You have {requests.length} message {requests.length === 1 ? 'request' : 'requests'} →
                  </button>
                )}
              </>
            ) : (
              <>
                <p className={m.emptyTitle}>No message requests</p>
                <p className={a.muted}>When someone who isn’t your friend messages you, it waits here until you accept it.</p>
              </>
            )}
          </div>
        ) : (
          <ul className={m.threadList}>
            {shown.map((t) => (
              <li key={t.other.accountId}>
                <ThreadRow thread={t} myId={myId} selected={t.other.accountId === selectedId} onOpen={() => navigate(`/messages/${t.other.accountId}`)} />
              </li>
            ))}
          </ul>
        )}
      </div>
    </aside>
  )
}

function ThreadRow({ thread, myId, selected, onOpen }: { thread: ThreadSummary; myId: string; selected: boolean; onOpen: () => void }) {
  const mine = thread.lastMessage.senderId === myId
  const unread = thread.unread > 0 && !selected
  return (
    <button type="button" className={m.threadRow} data-selected={selected} data-unread={unread} onClick={onOpen} aria-current={selected ? 'true' : undefined}>
      <Avatar name={thread.other.displayName} small online={thread.isFriend ? thread.other.online : undefined} />
      <span className={m.threadText}>
        <span className={m.threadTop}>
          <span className={m.threadName}>{thread.other.displayName}</span>
          <span className={m.threadTime}>{sinceLabel(thread.lastMessage.createdAt)}</span>
        </span>
        <span className={m.threadBottom}>
          <span className={m.threadPreview}>
            {thread.state === 'OUTGOING_REQUEST' ? <span className={m.pendingNote}>Request sent · </span> : mine ? 'You: ' : ''}
            {thread.lastMessage.body}
          </span>
          {unread && <span className={m.unreadBadge} aria-label={`${thread.unread} unread`}>{thread.unread > 99 ? '99+' : thread.unread}</span>}
        </span>
      </span>
    </button>
  )
}

/** Nothing selected: a quick way into a conversation with a friend. */
function EmptyPane() {
  const navigate = useNavigate()
  const friends = useFriendsStore((s) => s.friends)
  const loadFriends = useFriendsStore((s) => s.load)
  useEffect(() => {
    void loadFriends()
  }, [loadFriends])
  const shown = friends.slice(0, 8)

  return (
    <section className={`${p.panel} ${m.convPane} ${m.emptyPane}`}>
      <div className={m.emptyInner}>
        <svg className={m.emptyIcon} viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
          <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
        </svg>
        <h2 className={m.emptyHeading}>Your messages</h2>
        <p className={a.muted}>Pick a conversation, or start one with a friend.</p>
        {shown.length > 0 ? (
          <ul className={m.friendPicks}>
            {shown.map((f) => (
              <li key={f.accountId}>
                <button type="button" className={m.friendPick} onClick={() => navigate(`/messages/${f.accountId}`)}>
                  <Avatar name={f.displayName} small online={f.online} />
                  <span className={m.friendPickName}>{f.displayName}</span>
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <button type="button" className={p.button} onClick={() => navigate('/friends')}>
            Find friends
          </button>
        )}
      </div>
    </section>
  )
}

// ── Open conversation ───────────────────────────────────────────────────────

function ConversationPane({ accountId, myId }: { accountId: string; myId: string }) {
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

  if (!entry || entry.status === 'loading') {
    return (
      <section className={`${p.panel} ${m.convPane} ${m.emptyPane}`}>
        <BackLink />
        <span className={a.spinner} aria-hidden />
      </section>
    )
  }
  if (entry.status !== 'ready') {
    return (
      <section className={`${p.panel} ${m.convPane} ${m.emptyPane}`}>
        <BackLink />
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
  return <ReadyConversation accountId={accountId} myId={myId} conversation={entry} />
}

function BackLink() {
  return (
    <Link to="/messages" className={m.back} aria-label="Back to conversations">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
        <path d="M15 18l-6-6 6-6" />
      </svg>
    </Link>
  )
}

function ReadyConversation({ accountId, myId, conversation: c }: { accountId: string; myId: string; conversation: OpenConversation }) {
  const navigate = useNavigate()
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
    <section className={`${p.panel} ${m.convPane}`} aria-label={`Conversation with ${name}`}>
      <header className={m.convHead}>
        <BackLink />
        <Link to={`/u/${accountId}`} className={m.convWho} title="View profile">
          <Avatar name={name} small online={c.isFriend ? c.other.online : undefined} />
          <span className={m.convWhoText}>
            <span className={m.convName}>{name}</span>
            <span className={m.convStatus} data-online={c.isFriend && c.other.online}>{statusLine}</span>
          </span>
        </Link>
        <OverflowMenu
          items={[
            { label: 'View profile', onSelect: () => navigate(`/u/${accountId}`) },
            ...(c.messages.length > 0 ? [{ label: 'Delete conversation', onSelect: () => setConfirm('delete') }] : []),
            ...(c.messages.length > 0 ? [{ label: `Block ${name}`, danger: true, onSelect: () => setConfirm('block') }] : []),
          ]}
        />
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
          onConfirm={() => void run(() => remove(accountId), () => navigate('/messages'))}
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
          onConfirm={() => void run(() => block(accountId), () => navigate('/messages'))}
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
          <Avatar name={c.other.displayName} />
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

function OverflowMenu({ items }: { items: { label: string; danger?: boolean; onSelect: () => void }[] }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement | null>(null)
  useEffect(() => {
    if (!open) return
    const close = (e: PointerEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    const esc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', esc)
    return () => {
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', esc)
    }
  }, [open])

  return (
    <div className={m.menuRoot} ref={rootRef}>
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
