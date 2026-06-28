/**
 * Account presence widget for the landing screen — deliberately separate from the navigation
 * buttons so the sign-in state reads as a distinct, persistent affordance. Anchored top-right.
 *
 *  - signed in  → "Signed in as <name>" (opens the profile) + a Log out action
 *  - anonymous  → a single Log in button that opens the magic-link modal
 *
 * Renders nothing when the server has accounts disabled, so a no-accounts deployment shows no
 * sign-in UI at all (the whole point — a login form there can only fail).
 */
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LoginModal } from '@/components/auth/LoginModal'
import { useAuthStore } from '@/store/authStore'
import { useFriendsStore } from '@/store/friendsStore'
import styles from './AuthWidget.module.css'

export function AuthWidget() {
  const navigate = useNavigate()
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const status = useAuthStore((s) => s.status)
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  const incomingCount = useFriendsStore((s) => s.incoming.length)
  const loadFriends = useFriendsStore((s) => s.load)
  const resetFriends = useFriendsStore((s) => s.reset)
  const [loginOpen, setLoginOpen] = useState(false)

  // Keep the friends data (and the incoming-request badge) populated app-wide once signed in; clear
  // it on sign-out. Live updates then arrive via the WebSocket push (see friendsStore).
  useEffect(() => {
    if (status === 'authenticated') void loadFriends()
    else if (status === 'anonymous') resetFriends()
  }, [status, loadFriends, resetFriends])

  if (!accountsEnabled) return null

  const signOut = () => {
    resetFriends()
    logout()
  }

  return (
    <div className={styles.widget}>
      {status === 'authenticated' && user ? (
        <>
          <button
            type="button"
            className={styles.identity}
            onClick={() => navigate('/profile')}
            title="View your profile"
          >
            <span className={styles.dot} aria-hidden="true" />
            <span className={styles.labels}>
              <span className={styles.muted}>Signed in as</span>
              <span className={styles.name}>{user.displayName}</span>
            </span>
          </button>
          <button
            type="button"
            className={styles.logout}
            onClick={() => navigate('/friends')}
            title="Friends"
            style={{ position: 'relative' }}
          >
            Friends
            {incomingCount > 0 && (
              <span
                aria-label={`${incomingCount} pending friend requests`}
                style={{
                  marginLeft: 6,
                  display: 'inline-block',
                  minWidth: 16,
                  padding: '0 5px',
                  borderRadius: 999,
                  backgroundColor: '#e15b6e',
                  color: '#fff',
                  fontSize: 11,
                  fontWeight: 700,
                  lineHeight: '16px',
                  textAlign: 'center',
                }}
              >
                {incomingCount}
              </span>
            )}
          </button>
          <button type="button" className={styles.logout} onClick={signOut} title="Sign out">
            Log out
          </button>
        </>
      ) : (
        <button type="button" className={styles.login} onClick={() => setLoginOpen(true)}>
          Log in
        </button>
      )}
      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
    </div>
  )
}
