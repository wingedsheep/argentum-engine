/**
 * Friends page: your shareable friend code, an add-by-code box, incoming/outgoing requests, and your
 * friends list with live online status — plus the "hide my online status" toggle. Presence updates
 * arrive live over the WebSocket (see friendsStore); a slow poll covers the passive side of
 * accept/unfriend. Prompts sign-in when anonymous, and reports gracefully when accounts are disabled.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import { setPresenceHidden } from '@/api/friends'
import { LoginModal } from '@/components/auth/LoginModal'
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
      <div style={styles.wrap}>
        <div style={styles.container}>
          <div style={styles.header}>
            <button type="button" style={styles.link} onClick={() => navigate('/')}>
              ← Home
            </button>
            <button type="button" style={styles.link} onClick={() => navigate('/profile')}>
              Profile
            </button>
          </div>

          <h1 style={styles.title}>Friends</h1>

          {/* Your friend code + presence visibility */}
          <div style={styles.sectionBlock}>
            <h2 style={styles.section}>Your friend code</h2>
            <p style={styles.muted}>Share this so others can add you — it isn't your email.</p>
            <div style={styles.codeRow}>
              <code style={styles.code}>{user.id}</code>
              <button type="button" style={styles.smallPrimary} onClick={() => void copyCode()}>
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
            <label style={styles.toggleRow}>
              <input
                type="checkbox"
                checked={user.hidePresence}
                disabled={hideBusy}
                onChange={() => void toggleHidden()}
              />
              <span style={styles.toggleText}>
                Hide my online status
                <span style={styles.muted}> — friends will always see you as offline</span>
              </span>
            </label>
          </div>

          {/* Add a friend */}
          <div style={styles.sectionBlock}>
            <h2 style={styles.section}>Add a friend</h2>
            <div style={styles.addRow}>
              <input
                style={styles.input}
                placeholder="Paste a friend code"
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
              <button type="button" style={styles.smallPrimary} disabled={sending} onClick={() => void submitCode()}>
                {sending ? 'Sending…' : 'Send request'}
              </button>
            </div>
            {addError && <p style={styles.error}>{addError}</p>}
            {addNotice && <p style={styles.notice}>{addNotice}</p>}
          </div>

          {/* Incoming requests */}
          {incoming.length > 0 && (
            <div style={styles.sectionBlock}>
              <h2 style={styles.section}>Friend requests</h2>
              {incoming.map((r) => (
                <div key={r.requestId} style={styles.personRow}>
                  <span style={styles.personName}>{r.displayName}</span>
                  <div style={styles.rowActions}>
                    <button type="button" style={styles.smallPrimary} onClick={() => void accept(r.requestId)}>
                      Accept
                    </button>
                    <button type="button" style={styles.smallGhost} onClick={() => void removeRequest(r.requestId)}>
                      Decline
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Outgoing requests */}
          {outgoing.length > 0 && (
            <div style={styles.sectionBlock}>
              <h2 style={styles.section}>Pending (sent)</h2>
              {outgoing.map((r) => (
                <div key={r.requestId} style={styles.personRow}>
                  <span style={styles.personName}>{r.displayName}</span>
                  <div style={styles.rowActions}>
                    <span style={styles.pendingTag}>Awaiting reply</span>
                    <button type="button" style={styles.smallGhost} onClick={() => void removeRequest(r.requestId)}>
                      Cancel
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Friends list */}
          <div style={styles.sectionBlock}>
            <h2 style={styles.section}>
              Your friends {friends.length > 0 && <span style={styles.muted}>({friends.length})</span>}
            </h2>
            {storeError && <p style={styles.error}>{storeError}</p>}
            {friends.length === 0 ? (
              <p style={styles.muted}>{loading ? 'Loading…' : 'No friends yet. Share your code to get started.'}</p>
            ) : (
              friends.map((f) => (
                <div key={f.accountId} style={styles.personRow}>
                  <span style={styles.personName}>
                    <span style={{ ...styles.dot, backgroundColor: f.online ? '#5bd16e' : '#555' }} />
                    {f.displayName}
                    <span style={styles.statusText}>{f.online ? 'Online' : 'Offline'}</span>
                  </span>
                  <span style={{ display: 'flex', gap: 8 }}>
                    <button type="button" style={styles.smallGhost} onClick={() => navigate(`/u/${f.accountId}`)}>
                      View profile
                    </button>
                    <button type="button" style={styles.smallGhost} onClick={() => void unfriend(f.accountId)}>
                      Unfriend
                    </button>
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    )
  }

  const resolving = status === 'idle' || status === 'loading'

  return (
    <div style={styles.wrap}>
      <div style={styles.container}>
        <button type="button" style={styles.link} onClick={() => navigate('/')}>
          ← Home
        </button>
        <h1 style={styles.title}>Friends</h1>
        {accountsEnabled ? (
          <>
            <p style={styles.muted}>Sign in to add friends and see when they're online.</p>
            <button type="button" style={styles.primary} onClick={() => setLoginOpen(true)}>
              Sign in
            </button>
            <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
          </>
        ) : resolving ? (
          <p style={styles.muted}>Loading…</p>
        ) : (
          <p style={styles.muted}>Accounts aren't available on this server.</p>
        )}
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  wrap: { height: '100vh', overflowY: 'auto', backgroundColor: '#0a0a15', padding: '32px 16px' },
  container: { maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 12 },
  header: { display: 'flex', justifyContent: 'space-between' },
  link: { background: 'none', border: 'none', color: '#8b9bff', cursor: 'pointer', fontSize: 14, padding: 0 },
  title: { margin: '8px 0 0', color: '#fff', fontSize: 28 },
  muted: { margin: 0, color: '#888', fontSize: 14 },
  error: { margin: '8px 0 0', color: '#ff6b6b', fontSize: 13 },
  notice: { margin: '8px 0 0', color: '#5bd16e', fontSize: 13 },
  section: { margin: '0 0 10px', color: '#fff', fontSize: 18 },
  sectionBlock: {
    marginTop: 8,
    backgroundColor: '#14141f',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '16px 18px',
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
  },
  codeRow: { display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' },
  code: {
    flex: '1 1 240px',
    minWidth: 0,
    backgroundColor: '#0e0e18',
    border: '1px solid #2a2a3e',
    borderRadius: 8,
    padding: '8px 12px',
    color: '#cdd',
    fontFamily: 'monospace',
    fontSize: 13,
    wordBreak: 'break-all',
  },
  toggleRow: { display: 'flex', alignItems: 'flex-start', gap: 8, cursor: 'pointer', marginTop: 4 },
  toggleText: { color: '#ddd', fontSize: 14 },
  addRow: { display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' },
  input: {
    flex: '1 1 220px',
    minWidth: 0,
    backgroundColor: '#0e0e18',
    border: '1px solid #2a2a3e',
    borderRadius: 8,
    padding: '8px 12px',
    color: '#fff',
    fontSize: 14,
  },
  personRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    padding: '8px 0',
    borderTop: '1px solid #1f1f2e',
  },
  personName: { display: 'flex', alignItems: 'center', gap: 8, color: '#fff', fontSize: 15 },
  statusText: { color: '#888', fontSize: 12 },
  rowActions: { display: 'flex', alignItems: 'center', gap: 8 },
  pendingTag: { color: '#c9a227', fontSize: 12 },
  dot: { display: 'inline-block', width: 9, height: 9, borderRadius: 999 },
  smallPrimary: {
    padding: '8px 14px',
    borderRadius: 8,
    border: 'none',
    backgroundColor: '#5b6ee1',
    color: '#fff',
    fontWeight: 600,
    fontSize: 13,
    cursor: 'pointer',
  },
  smallGhost: {
    padding: '8px 14px',
    borderRadius: 8,
    border: '1px solid #2a2a3e',
    backgroundColor: 'transparent',
    color: '#aaa',
    fontSize: 13,
    cursor: 'pointer',
  },
  primary: {
    marginTop: 8,
    padding: '10px 18px',
    borderRadius: 8,
    border: 'none',
    backgroundColor: '#5b6ee1',
    color: '#fff',
    fontWeight: 600,
    fontSize: 14,
    cursor: 'pointer',
    alignSelf: 'flex-start',
  },
}
