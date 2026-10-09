/**
 * Friends page: your shareable friend code, an add-by-code box, incoming/outgoing requests, and your
 * friends list with live online status — plus the "hide my online status" toggle. Presence updates
 * arrive live over the WebSocket (see friendsStore); a slow poll covers the passive side of
 * accept/unfriend. Prompts sign-in when anonymous, and reports gracefully when accounts are disabled.
 */
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { setPresenceHidden } from '@/api/friends'
import { type BlockedPlayer, fetchBlocked, unblock } from '@/api/blocks'
import { LoginModal } from '@/components/auth/LoginModal'
import { AccountPage, Avatar, MessageCard, accountStyles as a } from '@/components/profile/accountUi'
import { pageStyles as p } from '@/components/ui/PageShell'
import { useAuthStore } from '@/store/authStore'
import { useFriendsStore } from '@/store/friendsStore'

const POLL_INTERVAL_MS = 25_000

export function FriendsPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const status = useAuthStore((s) => s.status)
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const init = useAuthStore((s) => s.init)
  const patchUser = useAuthStore((s) => s.patchUser)

  const friends = useFriendsStore((s) => s.friends)
  const onlineFriendCount = friends.filter((f) => f.online).length
  const incoming = useFriendsStore((s) => s.incoming)
  const outgoing = useFriendsStore((s) => s.outgoing)
  const loading = useFriendsStore((s) => s.loading)
  const storeError = useFriendsStore((s) => s.error)
  const load = useFriendsStore((s) => s.load)
  const sendRequest = useFriendsStore((s) => s.sendRequest)
  const accept = useFriendsStore((s) => s.accept)
  const removeRequest = useFriendsStore((s) => s.removeRequest)
  const unfriend = useFriendsStore((s) => s.unfriend)

  const [loginOpen, setLoginOpen] = useState(false)
  const [codeInput, setCodeInput] = useState('')
  const [addError, setAddError] = useState<string | null>(null)
  const [addNotice, setAddNotice] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [copied, setCopied] = useState(false)
  const [hideBusy, setHideBusy] = useState(false)

  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  // Initial load + slow poll while the page is open (presence also arrives live via WS push).
  useEffect(() => {
    if (status !== 'authenticated') return
    void load()
    const timer = window.setInterval(() => void load(), POLL_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [status, load])

  const submitCode = async () => {
    const code = codeInput.trim()
    if (!code) return
    setSending(true)
    setAddError(null)
    setAddNotice(null)
    try {
      await sendRequest(code)
      setCodeInput('')
      setAddNotice('Friend request sent.')
    } catch (e) {
      setAddError(e instanceof Error ? e.message : 'Could not send the request.')
    } finally {
      setSending(false)
    }
  }

  const copyCode = async () => {
    if (!user) return
    try {
      await navigator.clipboard.writeText(user.id)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      /* clipboard blocked — the code is shown for manual copy */
    }
  }

  const toggleHidden = async () => {
    if (!user) return
    const next = !user.hidePresence
    setHideBusy(true)
    try {
      await setPresenceHidden(next)
      patchUser({ hidePresence: next })
    } catch {
      /* leave the toggle as-is on failure */
    } finally {
      setHideBusy(false)
    }
  }

  if (status === 'authenticated' && user) {
    return (
      <AccountPage title="Friends">
        <div>
          <h1 className={p.h1}>Friends</h1>
          <p className={p.lede}>
            {friends.length === 0
              ? 'Share your friend code to add people, then see when they’re online.'
              : `${friends.length} friend${friends.length === 1 ? '' : 's'}${onlineFriendCount > 0 ? ` · ${onlineFriendCount} online now` : ''}`}
          </p>
        </div>

        <div className={a.friendsGrid}>
          {/* Your friend code + presence visibility */}
          <section className={`${p.panel} ${a.stack}`}>
            <h2 className={p.panelTitle}>Your friend code</h2>
            <p className={a.muted}>Share this so others can add you — it isn’t your email.</p>
            <div className={a.codeRow}>
              <code className={a.code}>{user.id}</code>
              <button type="button" className={p.button} onClick={() => void copyCode()}>
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
            <label className={a.toggle}>
              <input type="checkbox" checked={user.hidePresence} disabled={hideBusy} onChange={() => void toggleHidden()} />
              <span>
                Hide my online status
                <span className={a.dim}> — friends will always see you as offline</span>
              </span>
            </label>
          </section>

          {/* Add a friend */}
          <section className={`${p.panel} ${a.stack}`}>
            <h2 className={p.panelTitle}>Add a friend</h2>
            <p className={a.muted}>Paste the friend code they shared with you.</p>
            <div className={a.addRow}>
              <input
                className={p.input}
                placeholder="Friend code"
                aria-label="Friend code"
                value={codeInput}
                onChange={(e) => {
                  setCodeInput(e.target.value)
                  setAddError(null)
                  setAddNotice(null)
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void submitCode()
                }}
              />
              <button
                type="button"
                className={p.buttonPrimary}
                disabled={sending || !codeInput.trim()}
                onClick={() => void submitCode()}
              >
                {sending ? 'Sending…' : 'Send request'}
              </button>
            </div>
            {addError && <p className={a.error}>{addError}</p>}
            {addNotice && <p className={a.notice}>{addNotice}</p>}
          </section>

          {/* Incoming requests */}
          {incoming.length > 0 && (
            <section className={`${p.panel} ${a.full}`}>
              <div className={a.sectionHead}>
                <h2 className={a.sectionTitle}>Friend requests</h2>
                <span className={a.count}>{incoming.length}</span>
              </div>
              {incoming.map((r) => (
                <div key={r.requestId} className={a.personRow}>
                  <span className={a.person}>
                    <Avatar name={r.displayName} avatar={r.avatar} small />
                    <span className={a.personName}>{r.displayName}</span>
                  </span>
                  <span className={a.rowActions}>
                    <button type="button" className={p.buttonPrimary} onClick={() => void accept(r.requestId)}>
                      Accept
                    </button>
                    <button type="button" className={p.buttonGhost} onClick={() => void removeRequest(r.requestId)}>
                      Decline
                    </button>
                  </span>
                </div>
              ))}
            </section>
          )}

          {/* Friends list */}
          <section className={`${p.panel} ${a.full}`}>
            <div className={a.sectionHead}>
              <h2 className={a.sectionTitle}>Your friends</h2>
              {friends.length > 0 && (
                <span className={a.count}>
                  {friends.length}
                  {onlineFriendCount > 0 && <span className={a.win}> · {onlineFriendCount} online</span>}
                </span>
              )}
            </div>
            {storeError && <p className={a.error}>{storeError}</p>}
            {friends.length === 0 ? (
              <p className={a.muted}>{loading ? 'Loading…' : 'No friends yet. Share your code to get started.'}</p>
            ) : (
              friends.map((f) => (
                <div key={f.accountId} className={a.personRow}>
                  <span className={a.person}>
                    <Avatar name={f.displayName} avatar={f.avatar} small online={f.online} />
                    <span className={a.cellStack}>
                      <span className={a.personName}>{f.displayName}</span>
                      <span className={a.personStatus} data-online={f.online}>{f.online ? 'Online' : 'Offline'}</span>
                    </span>
                  </span>
                  <span className={a.rowActions}>
                    <button type="button" className={a.miniButton} onClick={() => navigate(`/messages/${f.accountId}`)}>
                      Message
                    </button>
                    <button type="button" className={a.miniButton} onClick={() => navigate(`/u/${f.accountId}`)}>
                      Profile
                    </button>
                    <button type="button" className={a.miniButton} onClick={() => void unfriend(f.accountId)}>
                      Unfriend
                    </button>
                  </span>
                </div>
              ))
            )}
          </section>

          {/* Outgoing requests */}
          {outgoing.length > 0 && (
            <section className={`${p.panel} ${a.full}`}>
              <div className={a.sectionHead}>
                <h2 className={a.sectionTitle}>Sent requests</h2>
                <span className={a.count}>{outgoing.length}</span>
              </div>
              {outgoing.map((r) => (
                <div key={r.requestId} className={a.personRow}>
                  <span className={a.person}>
                    <Avatar name={r.displayName} avatar={r.avatar} small />
                    <span className={a.cellStack}>
                      <span className={a.personName}>{r.displayName}</span>
                      <span className={a.pendingTag}>Awaiting reply</span>
                    </span>
                  </span>
                  <button type="button" className={a.miniButton} onClick={() => void removeRequest(r.requestId)}>
                    Cancel
                  </button>
                </div>
              ))}
            </section>
          )}

          <BlockedPlayersSection />
        </div>
      </AccountPage>
    )
  }

  const resolving = status === 'idle' || status === 'loading'

  return (
    <AccountPage title="Friends">
      <MessageCard>
        <h1 className={p.h1}>Friends</h1>
        {accountsEnabled ? (
          <>
            <p className={p.lede}>Sign in to add friends and see when they’re online.</p>
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
    </AccountPage>
  )
}

/**
 * Players you blocked from a result screen. Absent until there is one — most people never block
 * anyone, and an empty "Blocked" heading would only suggest they should.
 */
function BlockedPlayersSection() {
  const [blocked, setBlocked] = useState<BlockedPlayer[]>([])
  useEffect(() => {
    let cancelled = false
    fetchBlocked().then((list) => { if (!cancelled) setBlocked(list) }).catch(() => { /* not essential */ })
    return () => { cancelled = true }
  }, [])
  if (blocked.length === 0) return null

  const doUnblock = (accountId: string) => {
    setBlocked((list) => list.filter((b) => b.accountId !== accountId))
    void unblock(accountId).catch(() => fetchBlocked().then(setBlocked).catch(() => {}))
  }

  return (
    <section className={`${p.panel} ${a.full}`}>
      <div className={a.sectionHead}>
        <h2 className={a.sectionTitle}>Blocked</h2>
        <span className={a.count}>{blocked.length}</span>
      </div>
      <p className={a.muted}>You won’t be matched with these players, and their messages and emotes don’t reach you.</p>
      {blocked.map((b) => (
        <div key={b.accountId} className={a.personRow}>
          <span className={a.person}>
            <Avatar name={b.displayName} avatar={b.avatar} small />
            <span className={a.personName}>{b.displayName}</span>
          </span>
          <button type="button" className={a.miniButton} onClick={() => doUnblock(b.accountId)}>
            Unblock
          </button>
        </div>
      ))}
    </section>
  )
}
