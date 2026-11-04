/**
 * Full stats dashboard for the signed-in user. Fetches the per-user `/api/stats/me/*` endpoints and
 * hands them to the shared {@link StatsDashboard}; public profiles (/u/:userId) render the same
 * dashboard from a single bundled response. Account-gated.
 */
import { useEffect, useState } from 'react'
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
import { describeFilter, isAllGames } from '@/api/statsFilter'
import { useStatsFilterParam } from '@/components/stats/StatsFilterBar'
import { AccountPage, MessageCard, accountStyles as a } from '@/components/profile/accountUi'
import { pageStyles as p } from '@/components/ui/PageShell'
import { useAuthStore } from '@/store/authStore'

export function StatsPage() {
  const navigate = useNavigate()
  const status = useAuthStore((s) => s.status)
  const init = useAuthStore((s) => s.init)
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)

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
  const [filter, setFilter] = useStatsFilterParam()

  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  // Ratings and tournaments are their own records; the filter doesn't apply to them.
  useEffect(() => {
    if (status !== 'authenticated') return
    void fetchTournamentHistory(15).then(setTournaments).catch(() => setTournaments([]))
    void fetchRatings().then(setRatings).catch(() => setRatings([]))
    void fetchRatingsHistory().then(setRatingHistory).catch(() => setRatingHistory([]))
  }, [status])

  // Everything game-derived follows the filter. A response that lands after the filter moved on is
  // dropped, so a fast click-through never leaves an older slice on screen.
  useEffect(() => {
    if (status !== 'authenticated') return
    let live = true
    function load<T>(request: Promise<T>, set: (value: T) => void, fallback: T) {
      request.then(
        (value) => live && set(value),
        () => live && set(fallback),
      )
    }
    load<AccountStats | null>(fetchStats(filter), setStats, null)
    load(fetchColorStats(filter), setColors, [])
    load(fetchCardTypes(filter), setCardTypes, [])
    load(fetchManaCurve(filter), setCurve, [])
    load(fetchCreatureTypes(12, filter), setCreatureTypes, [])
    load(fetchSetStats(filter), setSets, [])
    load(fetchModeStats(filter), setModes, [])
    load(fetchOpponents(filter), setOpponents, [])
    load(fetchTopCards(24, filter), setTopCards, [])
    return () => {
      live = false
    }
  }, [status, filter])

  if (status !== 'authenticated') {
    const resolving = status === 'idle' || status === 'loading'
    return (
      <AccountPage title="Stats" width="wide" plain>
        <MessageCard>
          <h1 className={p.h1}>Your stats</h1>
          <p className={a.muted}>
            {resolving
              ? 'Loading…'
              : accountsEnabled
                ? 'Sign in to see your record, ratings and the cards you play.'
                : "Accounts aren't available on this server."}
          </p>
          {!resolving && accountsEnabled && (
            <button type="button" className={p.buttonPrimary} onClick={() => navigate('/profile')}>
              Go to sign in
            </button>
          )}
        </MessageCard>
      </AccountPage>
    )
  }

  const empty = (stats?.games ?? 0) === 0
  const filtered = !isAllGames(filter)

  return (
    <AccountPage title="Stats" width="wide" plain>
      <div>
        <h1 className={p.h1}>Your stats</h1>
        <p className={p.lede}>
          {empty && filtered
            ? `No games ${describeFilter(filter)}.`
            : empty
              ? 'Play some games to start building your stats.'
              : filtered
                ? `Your games ${describeFilter(filter)}.`
                : 'Everything you play, at a glance.'}
        </p>
      </div>
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
        filter={filter}
        onFilterChange={setFilter}
      />
      {empty && !filtered && (
        <section className={p.panel}>
          <div className={a.emptyHero}>
            <h2 className={a.sectionTitle}>Nothing to chart yet</h2>
            <p className={a.muted}>Finish a game while signed in — colors, curve, ratings and head-to-head fill in from there.</p>
            <button type="button" className={p.buttonPrimary} onClick={() => navigate('/')}>
              Find a game
            </button>
          </div>
        </section>
      )}
    </AccountPage>
  )
}
