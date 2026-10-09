/**
 * Admin dashboard entry point. Resolves admin access two ways (mirroring the server's AdminAuthService):
 *
 *  1. a signed-in account flagged as admin — taken straight in, using its normal auth token, or
 *  2. the bootstrap `X-Admin-Password`, entered once and kept in sessionStorage for the session.
 *
 * Once in, it's a hub that routes to the admin areas (Stats / Live overview / Activity / Players). The bootstrap
 * password is only needed to create the first admin: sign in with it, open Players, and promote an
 * account — that account can then reach the dashboard with its own sign-in.
 */
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AdminDashboard } from './AdminDashboard'
import { AdminActivity } from './AdminActivity'
import { AdminLiveOverview } from './AdminLiveOverview'
import { AdminPlayers } from './AdminPlayers'
import { AdminHub, type AdminArea } from './AdminHub'
import { PageShell, pageStyles } from '@/components/ui/PageShell'
import { devStyles } from './DevPage'
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

  /**
   * Validate a bootstrap password against an admin endpoint. Live overview is the one mounted on every
   * server — the account-backed ones 404 when accounts are off, which is exactly when the password is
   * the only way in.
   */
  const validatePassword = useCallback(async (pwd: string): Promise<{ ok: boolean; error?: string }> => {
    try {
      const res = await fetch('/api/admin/live-overview', { headers: { 'X-Admin-Password': pwd } })
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
      <PageShell title="Admin" width="narrow" plain>
        <p className={`${pageStyles.empty} ${pageStyles.muted}`}>Loading…</p>
      </PageShell>
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
  if (view === 'live') {
    return <AdminLiveOverview auth={auth} onBack={() => setView('hub')} />
  }
  if (view === 'activity') {
    return <AdminActivity auth={auth} onBack={() => setView('hub')} />
  }
  if (view === 'players') {
    return <AdminPlayers auth={auth} onBack={() => setView('hub')} />
  }

  const authLabel = auth ? 'Signed in with the admin password' : `Signed in as ${user?.displayName ?? 'admin'}`
  return <AdminHub onNavigate={setView} authLabel={authLabel} />
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
    <PageShell title="Admin" width="narrow" plain>
      <div className={`${pageStyles.panel} ${devStyles.loginCard}`}>
        <h1 className={pageStyles.h1}>Admin dashboard</h1>
        <p className={devStyles.loginLede}>
          Enter the admin password, or sign in with an admin account to skip this step.
        </p>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && onLogin()}
          placeholder="Admin password"
          className={pageStyles.input}
          style={{ width: '100%' }}
          autoFocus
        />
        <button onClick={onLogin} disabled={loading || !password} className={pageStyles.buttonPrimary} style={{ width: '100%', minHeight: 42 }}>
          {loading ? 'Connecting…' : 'Continue'}
        </button>
        {error && <p className={devStyles.error}>{error}</p>}
        <button type="button" onClick={onHome} className={pageStyles.buttonGhost}>
          ← Back to home
        </button>
      </div>
    </PageShell>
  )
}
