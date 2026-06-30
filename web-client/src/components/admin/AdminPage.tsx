/**
 * Admin dashboard entry point. Resolves admin access two ways (mirroring the server's AdminAuthService):
 *
 *  1. a signed-in account flagged as admin — taken straight in, using its normal auth token, or
 *  2. the bootstrap `X-Admin-Password`, entered once and kept in sessionStorage for the session.
 *
 * Once in, it's a hub that routes to the admin areas (Stats / Activity / Players). The bootstrap
 * password is only needed to create the first admin: sign in with it, open Players, and promote an
 * account — that account can then reach the dashboard with its own sign-in.
 */
import { useCallback, useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import { AdminDashboard } from './AdminDashboard'
import { AdminActivity } from './AdminActivity'
import { AdminPlayers } from './AdminPlayers'
import { AdminHub, type AdminArea } from './AdminHub'
import { adminTheme } from './adminUi'
import type { AdminAuth } from '@/api/adminAuth'
import { useAuthStore } from '@/store/authStore'

type Access = 'pending' | 'granted' | 'denied'

export function AdminPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const authStatus = useAuthStore((s) => s.status)
  const initAuth = useAuthStore((s) => s.init)

  const [access, setAccess] = useState<Access>('pending')
  /** Bootstrap password if that's how we got in, else null (use the signed-in account's token). */
  const [auth, setAuth] = useState<AdminAuth>(null)
  const [view, setView] = useState<'hub' | AdminArea>('hub')

  const [passwordDraft, setPasswordDraft] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  /** Validate a bootstrap password against an admin endpoint. */
  const validatePassword = useCallback(async (pwd: string): Promise<{ ok: boolean; error?: string }> => {
    try {
      const res = await fetch('/api/admin/users', { headers: { 'X-Admin-Password': pwd } })
      if (res.status === 401) {
        const data = (await res.json().catch(() => null)) as { error?: string } | null
        return { ok: false, error: data?.error ?? 'Invalid admin password' }
      }
      if (!res.ok) return { ok: false, error: `Server error: ${res.status}` }
      return { ok: true }
    } catch {
      return { ok: false, error: 'Failed to connect to server' }
    }
  }, [])

  // Resolve the account session once.
  useEffect(() => {
    if (authStatus === 'idle') void initAuth()
  }, [authStatus, initAuth])

  // Decide access: admin account first, then a stored bootstrap password.
  useEffect(() => {
    if (access === 'granted') return
    if (authStatus === 'idle' || authStatus === 'loading') return // wait for the session to resolve
    let cancelled = false
    async function resolve() {
      if (user?.isAdmin) {
        if (!cancelled) {
          setAuth(null)
          setAccess('granted')
        }
        return
      }
      const saved = sessionStorage.getItem('adminPassword')
      if (saved) {
        const { ok } = await validatePassword(saved)
        if (cancelled) return
        if (ok) {
          setAuth(saved)
          setAccess('granted')
          return
        }
        sessionStorage.removeItem('adminPassword')
      }
      if (!cancelled) setAccess('denied')
    }
    void resolve()
    return () => {
      cancelled = true
    }
  }, [authStatus, user, access, validatePassword])

  const handleLogin = async () => {
    setLoading(true)
    setError(null)
    const result = await validatePassword(passwordDraft)
    if (result.ok) {
      sessionStorage.setItem('adminPassword', passwordDraft)
      setAuth(passwordDraft)
      setView('hub')
      setAccess('granted')
    } else {
      setError(result.error ?? 'Login failed')
    }
    setLoading(false)
  }

  if (access === 'pending') {
    return (
      <div style={styles.pageContainer}>
        <p style={styles.loadingText}>Loading…</p>
      </div>
    )
  }

  if (access === 'denied') {
    return (
      <LoginView
        password={passwordDraft}
        setPassword={setPasswordDraft}
        onLogin={handleLogin}
        onHome={() => navigate('/')}
        error={error}
        loading={loading}
      />
    )
  }

  if (view === 'stats') {
    return <AdminDashboard auth={auth} onBack={() => setView('hub')} />
  }
  if (view === 'activity') {
    return <AdminActivity auth={auth} onBack={() => setView('hub')} />
  }
  if (view === 'players') {
    return <AdminPlayers auth={auth} onBack={() => setView('hub')} />
  }

  const authLabel = auth ? 'Signed in with the admin password' : `Signed in as ${user?.displayName ?? 'admin'}`
  return <AdminHub onNavigate={setView} onExit={() => navigate('/')} authLabel={authLabel} />
}

// ============================================================================
// Login View (bootstrap password)
// ============================================================================

function LoginView({
  password,
  setPassword,
  onLogin,
  onHome,
  error,
  loading,
}: {
  password: string
  setPassword: (p: string) => void
  onLogin: () => void
  onHome: () => void
  error: string | null
  loading: boolean
}) {
  return (
    <div style={styles.pageContainer}>
      <div style={styles.loginCard}>
        <div style={styles.loginIcon} aria-hidden>
          🔑
        </div>
        <h1 style={styles.loginTitle}>Admin Dashboard</h1>
        <p style={styles.loginSubtitle}>
          Enter the admin password, or sign in with an admin account to skip this step.
        </p>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && onLogin()}
          placeholder="Admin password"
          style={styles.input}
          autoFocus
        />
        <button onClick={onLogin} disabled={loading || !password} style={styles.primaryButton}>
          {loading ? 'Connecting…' : 'Continue'}
        </button>
        {error && <p style={styles.errorText}>{error}</p>}
        <button type="button" onClick={onHome} style={styles.homeLink}>
          ← Back to home
        </button>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  pageContainer: {
    minHeight: '100vh',
    height: '100vh',
    overflowY: 'auto',
    backgroundColor: adminTheme.bg,
    color: adminTheme.textSecondary,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  loadingText: { color: adminTheme.textMuted, fontSize: 14 },
  loginCard: {
    backgroundColor: adminTheme.panel,
    borderRadius: 16,
    padding: '36px 40px',
    textAlign: 'center',
    border: `1px solid ${adminTheme.border}`,
    width: 380,
    maxWidth: '92vw',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  loginIcon: { fontSize: 34, marginBottom: 8 },
  loginTitle: { margin: '0 0 8px', fontSize: 22, color: adminTheme.text, fontWeight: 700 },
  loginSubtitle: { margin: '0 0 22px', fontSize: 13.5, color: adminTheme.textMuted, lineHeight: 1.5 },
  input: {
    width: '100%',
    padding: '11px 14px',
    fontSize: 14,
    backgroundColor: '#0a0a14',
    color: adminTheme.text,
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 9,
    outline: 'none',
    marginBottom: 14,
    boxSizing: 'border-box',
  },
  primaryButton: {
    width: '100%',
    padding: '11px 0',
    fontSize: 14,
    fontWeight: 600,
    backgroundColor: adminTheme.accentSolid,
    color: '#fff',
    border: 'none',
    borderRadius: 9,
    cursor: 'pointer',
  },
  errorText: { color: adminTheme.bad, fontSize: 13, marginTop: 12, marginBottom: 0 },
  homeLink: {
    marginTop: 18,
    background: 'none',
    border: 'none',
    color: adminTheme.accent,
    cursor: 'pointer',
    fontSize: 13,
  },
}
