import { useState, useEffect, useCallback } from 'react'
import { ReplayViewer, type GameSummary, type SpectatorStateUpdate } from './ReplayViewer'

// ============================================================================
// AdminPage
// ============================================================================

type View = 'login' | 'replays'

export function AdminPage() {
  const [view, setView] = useState<View>('login')
  const [password, setPassword] = useState(() => sessionStorage.getItem('adminPassword') ?? '')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const validatePassword = useCallback(async (pwd: string): Promise<boolean> => {
    try {
      const res = await fetch('/api/admin/games', {
        headers: { 'X-Admin-Password': pwd },
      })
      if (res.status === 401) {
        const data = await res.json()
        setError(data.error ?? 'Unauthorized')
        return false
      }
      if (!res.ok) {
        setError(`Server error: ${res.status}`)
        return false
      }
      return true
    } catch {
      setError('Failed to connect to server')
      return false
    }
  }, [])

  const handleLogin = async () => {
    setLoading(true)
    setError(null)
    const ok = await validatePassword(password)
    if (ok) {
      sessionStorage.setItem('adminPassword', password)
      setView('replays')
    }
    setLoading(false)
  }

  const fetchGames = useCallback(async (): Promise<GameSummary[]> => {
    const res = await fetch('/api/admin/games', {
      headers: { 'X-Admin-Password': password },
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`)
    return await res.json() as GameSummary[]
  }, [password])

  const fetchReplay = useCallback(async (gameId: string): Promise<SpectatorStateUpdate[]> => {
    const res = await fetch(`/api/admin/games/${gameId}/replay`, {
      headers: { 'X-Admin-Password': password },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as SpectatorStateUpdate[]
  }, [password])

  // Try auto-login on mount
  useEffect(() => {
    const saved = sessionStorage.getItem('adminPassword')
    if (saved) {
      validatePassword(saved).then((ok) => {
        if (ok) setView('replays')
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (view === 'login') {
    return <LoginView password={password} setPassword={setPassword} onLogin={handleLogin} error={error} loading={loading} />
  }

  return (
    <ReplayViewer
      fetchGames={fetchGames}
      fetchReplay={fetchReplay}
      onBack={() => {
        sessionStorage.removeItem('adminPassword')
        setView('login')
      }}
    />
  )
}

// ============================================================================
// Login View
// ============================================================================

function LoginView({
  password,
  setPassword,
  onLogin,
  error,
  loading,
}: {
  password: string
  setPassword: (p: string) => void
  onLogin: () => void
  error: string | null
  loading: boolean
}) {
  return (
    <div style={styles.pageContainer}>
      <div style={styles.loginCard}>
        <h1 style={styles.loginTitle}>Game Replays</h1>
        <p style={styles.loginSubtitle}>Enter admin password to continue</p>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && onLogin()}
          placeholder="Password"
          style={styles.input}
          autoFocus
        />
        <button onClick={onLogin} disabled={loading || !password} style={styles.primaryButton}>
          {loading ? 'Connecting...' : 'Login'}
        </button>
        {error && <p style={styles.errorText}>{error}</p>}
      </div>
    </div>
  )
}

// ============================================================================
// Styles
// ============================================================================

const styles: Record<string, React.CSSProperties> = {
  pageContainer: {
    minHeight: '100vh',
    backgroundColor: '#0a0a12',
    color: '#ccc',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'flex-start',
    paddingTop: 80,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  loginCard: {
    backgroundColor: '#12121e',
    borderRadius: 12,
    padding: '40px 48px',
    textAlign: 'center',
    border: '1px solid #1a1a2e',
    width: 360,
  },
  loginTitle: {
    margin: '0 0 8px',
    fontSize: 24,
    color: '#e0e0e0',
  },
  loginSubtitle: {
    margin: '0 0 24px',
    fontSize: 14,
    color: '#666',
  },
  input: {
    width: '100%',
    padding: '10px 14px',
    fontSize: 14,
    backgroundColor: '#0a0a14',
    color: '#ccc',
    border: '1px solid #2a2a3e',
    borderRadius: 6,
    outline: 'none',
    marginBottom: 16,
    boxSizing: 'border-box',
  },
  primaryButton: {
    width: '100%',
    padding: '10px 0',
    fontSize: 14,
    backgroundColor: '#2563eb',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  errorText: {
    color: '#ef4444',
    fontSize: 13,
    marginTop: 12,
    marginBottom: 0,
  },
}
