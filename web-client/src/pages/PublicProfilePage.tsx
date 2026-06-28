/**
 * Read-only public profile for any player, at /u/:userId. Fetches the bundled
 * `/api/stats/users/{id}` response and renders the shared {@link StatsDashboard} (plus a compact
 * recent-games list). No auth required — profiles are public — and decklists are never exposed here
 * (the deck viewer stays on the owner's own profile).
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { type PublicProfile, ProfileNotFoundError, fetchPublicProfile } from '@/api/account'
import { StatsDashboard } from '@/components/profile/StatsDashboard'

export function PublicProfilePage() {
  const navigate = useNavigate()
  const { userId } = useParams<{ userId: string }>()
  const [profile, setProfile] = useState<PublicProfile | null>(null)
  const [state, setState] = useState<'loading' | 'ready' | 'notfound' | 'error'>('loading')

  useEffect(() => {
    if (!userId) return
    let live = true
    setState('loading')
    fetchPublicProfile(userId)
      .then((p) => {
        if (!live) return
        setProfile(p)
        setState('ready')
      })
      .catch((e) => {
        if (!live) return
        setState(e instanceof ProfileNotFoundError ? 'notfound' : 'error')
      })
    return () => {
      live = false
    }
  }, [userId])

  return (
    <div style={styles.wrap}>
      <div style={styles.container}>
        <div style={styles.header}>
          <button type="button" style={styles.link} onClick={() => navigate(-1)}>
            ← Back
          </button>
          <button type="button" style={styles.link} onClick={() => navigate('/')}>
            Home
          </button>
        </div>

        {state === 'loading' && <p style={styles.muted}>Loading…</p>}
        {state === 'notfound' && (
          <>
            <h1 style={styles.title}>Player not found</h1>
            <p style={styles.muted}>This profile doesn’t exist or is no longer available.</p>
          </>
        )}
        {state === 'error' && (
          <>
            <h1 style={styles.title}>Couldn’t load profile</h1>
            <p style={styles.muted}>Something went wrong fetching this player’s stats.</p>
          </>
        )}
        {state === 'ready' && profile && (
          <>
            <h1 style={styles.title}>{profile.displayName}</h1>
            <p style={styles.muted}>Player profile</p>
            <StatsDashboard
              stats={profile.stats}
              ratings={profile.ratings}
              ratingHistory={profile.ratingHistory}
              colors={profile.colors}
              cardTypes={profile.cardTypes}
              curve={profile.curve}
              creatureTypes={profile.creatureTypes}
              modes={profile.modes}
              sets={profile.sets}
              topCards={profile.topCards}
              opponents={profile.opponents}
              tournaments={profile.tournaments}
              recentGames={profile.recentGames}
            />
          </>
        )}
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  wrap: { height: '100vh', overflowY: 'auto', backgroundColor: '#0a0a15', padding: '32px 16px' },
  container: { maxWidth: 960, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 14 },
  header: { display: 'flex', justifyContent: 'space-between' },
  link: { background: 'none', border: 'none', color: '#8b9bff', cursor: 'pointer', fontSize: 14, padding: 0 },
  title: { margin: '4px 0 0', color: '#fff', fontSize: 28 },
  muted: { margin: 0, color: '#888', fontSize: 14 },
}
