/**
 * Full stats dashboard for the signed-in user. Fetches the per-user `/api/stats/me/*` endpoints and
 * hands them to the shared {@link StatsDashboard}; public profiles (/u/:userId) render the same
 * dashboard from a single bundled response. Account-gated.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import {
  type AccountStats,
  type CardStat,
  type HeadToHead,
  type RatingEntry,
  type RatingPoint,
  type StatBucket,
  type UserTournamentEntry,
  fetchCardTypes,
  fetchColorStats,
  fetchCreatureTypes,
  fetchManaCurve,
  fetchModeStats,
  fetchOpponents,
  fetchRatings,
  fetchRatingsHistory,
  fetchSetStats,
  fetchStats,
  fetchTopCards,
  fetchTournamentHistory,
} from '@/api/account'
import { StatsDashboard } from '@/components/profile/StatsDashboard'
import { useAuthStore } from '@/store/authStore'

export function StatsPage() {
  const navigate = useNavigate()
  const status = useAuthStore((s) => s.status)
  const init = useAuthStore((s) => s.init)

  const [stats, setStats] = useState<AccountStats | null>(null)
  const [colors, setColors] = useState<StatBucket[]>([])
  const [cardTypes, setCardTypes] = useState<StatBucket[]>([])
  const [curve, setCurve] = useState<StatBucket[]>([])
  const [creatureTypes, setCreatureTypes] = useState<StatBucket[]>([])
  const [sets, setSets] = useState<StatBucket[]>([])
  const [modes, setModes] = useState<StatBucket[]>([])
  const [opponents, setOpponents] = useState<HeadToHead[]>([])
  const [topCards, setTopCards] = useState<CardStat[]>([])
  const [tournaments, setTournaments] = useState<UserTournamentEntry[]>([])
  const [ratings, setRatings] = useState<RatingEntry[]>([])
  const [ratingHistory, setRatingHistory] = useState<RatingPoint[]>([])

  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  useEffect(() => {
    if (status !== 'authenticated') return
    void fetchStats().then(setStats).catch(() => setStats(null))
    void fetchColorStats().then(setColors).catch(() => setColors([]))
    void fetchCardTypes().then(setCardTypes).catch(() => setCardTypes([]))
    void fetchManaCurve().then(setCurve).catch(() => setCurve([]))
    void fetchCreatureTypes(12).then(setCreatureTypes).catch(() => setCreatureTypes([]))
    void fetchSetStats().then(setSets).catch(() => setSets([]))
    void fetchModeStats().then(setModes).catch(() => setModes([]))
    void fetchOpponents().then(setOpponents).catch(() => setOpponents([]))
    void fetchTopCards(24).then(setTopCards).catch(() => setTopCards([]))
    void fetchTournamentHistory(15).then(setTournaments).catch(() => setTournaments([]))
    void fetchRatings().then(setRatings).catch(() => setRatings([]))
    void fetchRatingsHistory().then(setRatingHistory).catch(() => setRatingHistory([]))
  }, [status])

  if (status !== 'authenticated') {
    return (
      <div style={styles.wrap}>
        <div style={styles.container}>
          <button type="button" style={styles.link} onClick={() => navigate('/profile')}>
            ← Profile
          </button>
          <h1 style={styles.title}>Your stats</h1>
          <p style={styles.muted}>
            {status === 'idle' || status === 'loading' ? 'Loading…' : 'Sign in to see your stats.'}
          </p>
        </div>
      </div>
    )
  }

  return (
    <div style={styles.wrap}>
      <div style={styles.container}>
        <div style={styles.header}>
          <button type="button" style={styles.link} onClick={() => navigate('/profile')}>
            ← Profile
          </button>
          <button type="button" style={styles.link} onClick={() => navigate('/')}>
            Home
          </button>
        </div>
        <h1 style={styles.title}>Your stats</h1>
        {(stats?.games ?? 0) === 0 && <p style={styles.muted}>Play some games to start building your stats.</p>}
        <StatsDashboard
          stats={stats}
          ratings={ratings}
          ratingHistory={ratingHistory}
          colors={colors}
          cardTypes={cardTypes}
          curve={curve}
          creatureTypes={creatureTypes}
          modes={modes}
          sets={sets}
          topCards={topCards}
          opponents={opponents}
          tournaments={tournaments}
        />
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
