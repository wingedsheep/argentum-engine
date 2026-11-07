/**
 * The home screen's "Open lobbies" panel: every public quick game and tournament lobby still
 * waiting for players, each answering the three questions that decide a click — what is being
 * played, who is hosting, and how close the table is to starting.
 *
 * Built for a busy server as well as a quiet one: past a handful of lobbies a category filter
 * appears, past a few more a search field (host, players, sets, format), and the list scrolls
 * inside the panel instead of pushing the rest of the side column off-screen.
 *
 * AI seats are drawn apart from people — square pips after the round human ones, and a robot
 * badge in the byline — because "4/6" means something very different when three of the four
 * are bots.
 */
import { useMemo, useState } from 'react'
import type { TournamentFormat } from '@/types'
import { labelForFormat } from '@/utils/deckLegality'
import { Avatar } from '@/components/profile/accountUi'
import styles from './PublicLobbyList.module.css'

export interface PublicTournamentSummary {
  lobbyId: string
  state: string
  playerCount: number
  maxPlayers: number
  format: TournamentFormat
  setNames: string[]
  boosterCount: number
  /** How many times each pair meets in the round-robin. */
  gamesPerMatch: number
  /** Games per match: 1, or 3 for a best-of-three. */
  bestOf?: number
  deckFormat?: string | null
  hostName?: string | null
  hostAvatar?: string | null
  playerNames?: string[]
  /** Seats in [playerCount] held by AI players. */
  aiPlayerCount?: number
  gameMode?: 'TOURNAMENT' | 'FREE_FOR_ALL' | 'TWO_HEADED_GIANT' | 'TEAM_VS_TEAM'
  ranked?: boolean
  cubeName?: string | null
  /** Seconds the lobby has been open (server-measured). */
  openSeconds?: number
}

export interface PublicQuickGameSummary {
  lobbyId: string
  playerCount: number
  maxPlayers: number
  setCode: string | null
  hostName: string | null
  format?: string | null
  hostAvatar?: string | null
  playerNames?: string[]
  twoHeadedGiant?: boolean
  ranked?: boolean
  momirBasic?: boolean
  /** Seconds the lobby has been open (server-measured). */
  openSeconds?: number
}

export type PublicLobbyEntry =
  | ({ kind: 'tournament' } & PublicTournamentSummary)
  | ({ kind: 'quickGame' } & PublicQuickGameSummary)

type Category = 'duel' | 'multiplayer' | 'tournament'

const CATEGORY_LABELS: Record<Category, string> = {
  duel: '1v1',
  multiplayer: 'Multiplayer',
  tournament: 'Tournaments',
}

/** Lobby count past which the category filter shows (when more than one category is present). */
const FILTER_THRESHOLD = 4
/** Lobby count past which the search field shows. */
const SEARCH_THRESHOLD = 6

export function PublicLobbyList({
  lobbies,
  error,
  onlinePlayers,
  onJoin,
}: {
  lobbies: PublicLobbyEntry[]
  error: string | null
  onlinePlayers: number | null
  onJoin: (entry: PublicLobbyEntry) => void
}) {
  const [category, setCategory] = useState<Category | 'all'>('all')
  const [query, setQuery] = useState('')

  const sorted = useMemo(
    // Newest first, so a lobby that just opened is where the eye lands — and a row never jumps
    // when someone joins it, since seat counts don't move the order.
    () => [...lobbies].sort((a, b) => (a.openSeconds ?? 0) - (b.openSeconds ?? 0) || a.lobbyId.localeCompare(b.lobbyId)),
    [lobbies],
  )

  const counts = useMemo(() => {
    const c: Record<Category, number> = { duel: 0, multiplayer: 0, tournament: 0 }
    for (const entry of lobbies) c[categoryOf(entry)]++
    return c
  }, [lobbies])
  const presentCategories = (Object.keys(counts) as Category[]).filter((c) => counts[c] > 0)
  const showFilter = lobbies.length >= FILTER_THRESHOLD && presentCategories.length > 1
  const showSearch = lobbies.length >= SEARCH_THRESHOLD
  // A filter whose category emptied out (its last lobby filled) falls back to everything.
  const activeCategory = category !== 'all' && counts[category] === 0 ? 'all' : category

  const needle = showSearch ? query.trim().toLowerCase() : ''
  const visible = sorted.filter((entry) =>
    (!showFilter || activeCategory === 'all' || categoryOf(entry) === activeCategory) &&
    (!needle || searchText(entry).includes(needle)),
  )

  if (lobbies.length === 0 && !error && (onlinePlayers ?? 0) === 0) return null

  const clearFilters = () => {
    setCategory('all')
    setQuery('')
  }

  return (
    <section className={styles.panel} aria-labelledby="open-lobbies-title">
      <header className={styles.header}>
        <h2 id="open-lobbies-title" className={styles.title}>
          Open lobbies
          {lobbies.length > 0 && <span className={styles.count}>{lobbies.length}</span>}
        </h2>
        {onlinePlayers !== null && onlinePlayers > 0 && (
          <span className={styles.online}>
            <span className={styles.onlineDot} aria-hidden />
            {onlinePlayers} online
          </span>
        )}
      </header>

      {(showFilter || showSearch) && (
        <div className={styles.toolbar}>
          {showFilter && (
            <div className={styles.filters} role="group" aria-label="Filter lobbies">
              <FilterChip label="All" count={lobbies.length} active={activeCategory === 'all'} onClick={() => setCategory('all')} />
              {presentCategories.map((c) => (
                <FilterChip
                  key={c}
                  label={CATEGORY_LABELS[c]}
                  count={counts[c]}
                  active={activeCategory === c}
                  onClick={() => setCategory(c)}
                />
              ))}
            </div>
          )}
          {showSearch && (
            <div className={styles.search}>
              <SearchIcon />
              <input
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search host, set or format"
                aria-label="Search lobbies"
                className={styles.searchInput}
              />
            </div>
          )}
        </div>
      )}

      {lobbies.length === 0 ? (
        <p className={styles.empty}>
          {error ?? 'No open lobbies right now. Create one from Play and pick “Anyone” to list it here.'}
        </p>
      ) : visible.length === 0 ? (
        <div className={styles.noMatch}>
          <span>No lobbies match{needle ? ` “${query.trim()}”` : ''}.</span>
          <button type="button" className={styles.clearButton} onClick={clearFilters}>Clear filters</button>
        </div>
      ) : (
        <ul className={styles.list} data-scrolls={visible.length > 3}>
          {visible.map((entry) => (
            <LobbyRow key={`${entry.kind}-${entry.lobbyId}`} entry={entry} onJoin={onJoin} />
          ))}
        </ul>
      )}

      {visible.length > 0 && visible.length < lobbies.length && (
        <p className={styles.footnote}>
          Showing {visible.length} of {lobbies.length}
          {' · '}
          <button type="button" className={styles.linkButton} onClick={clearFilters}>Show all</button>
        </p>
      )}
    </section>
  )
}

function LobbyRow({ entry, onJoin }: { entry: PublicLobbyEntry; onJoin: (entry: PublicLobbyEntry) => void }) {
  const title = lobbyTitle(entry)
  const tags = lobbyTags(entry)
  const host = entry.hostName ?? null
  const others = (entry.playerNames ?? []).filter((n) => n !== host)
  const aiSeats = aiSeatsOf(entry)
  const openSeats = Math.max(0, entry.maxPlayers - entry.playerCount)
  const waited = entry.openSeconds !== undefined ? formatWaited(entry.openSeconds) : null

  return (
    <li className={styles.row} data-category={categoryOf(entry)}>
      <div className={styles.rowTop}>
        <span className={styles.mode}>{modeLabel(entry)}</span>
        {entry.ranked && <span className={styles.ranked}>Ranked</span>}
        {waited && <span className={styles.waited} title="How long this lobby has been open">{waited}</span>}
      </div>

      <div className={styles.rowMain}>
        <div className={styles.what}>
          <span className={styles.lobbyTitle} title={title}>{title}</span>
          {tags.length > 0 && (
            <span className={styles.tags}>
              {tags.map((t) => <span key={t} className={styles.tag}>{t}</span>)}
            </span>
          )}
        </div>
        <button
          type="button"
          onClick={() => onJoin(entry)}
          className={styles.joinButton}
          aria-label={`Join ${host ? `${host}’s ` : ''}${title} lobby`}
        >
          Join
        </button>
      </div>

      <div className={styles.rowBottom}>
        <span className={styles.host} title={host ? [host, ...others].join(', ') : undefined}>
          {host ? (
            <>
              <Avatar name={host} avatar={entry.hostAvatar} small />
              <span className={styles.hostText}>
                Hosted by <span className={styles.hostName}>{host}</span>
                {others.length > 0 && <span className={styles.others}> · with {others.join(', ')}</span>}
              </span>
            </>
          ) : (
            <span className={styles.hostText}>Host unknown</span>
          )}
          {aiSeats > 0 && <AiBadge count={aiSeats} />}
        </span>
        <SeatMeter filled={entry.playerCount} ai={aiSeats} total={entry.maxPlayers} openSeats={openSeats} />
      </div>
    </li>
  )
}

/**
 * One pip per seat — the table filling up at a glance — with the count for the exact number.
 * People come first as round pips in the lobby's accent, AI seats follow as cyan squares after a
 * small gap, so the two read apart by colour, shape and grouping.
 */
function SeatMeter({ filled, ai, total, openSeats }: { filled: number; ai: number; total: number; openSeats: number }) {
  const humans = filled - ai
  const left = openSeats === 1 ? '1 seat left' : `${openSeats} seats left`
  const label = ai > 0 ? `${plural(humans, 'player')}, ${ai} AI, ${left}` : left
  return (
    <span className={styles.seats} title={label} aria-label={`${filled} of ${total} seats taken: ${label}`}>
      {total <= 8 && (
        <span className={styles.pips} aria-hidden>
          {Array.from({ length: total }, (_, i) => (
            <span
              key={i}
              className={styles.pip}
              data-seat={i < humans ? 'human' : i < filled ? 'ai' : 'open'}
              data-group-start={i === humans && ai > 0 && humans > 0}
            />
          ))}
        </span>
      )}
      <span className={styles.seatCount} aria-hidden>{filled}/{total}</span>
    </span>
  )
}

/** "3 AI" beside the host: the byline lists people by name, so the bots get a count of their own. */
function AiBadge({ count }: { count: number }) {
  const label = count === 1 ? '1 AI player' : `${count} AI players`
  return (
    <span className={styles.aiBadge} title={`${label} seated`} aria-label={`${label} seated`}>
      <BotIcon />
      {count} AI
    </span>
  )
}

function BotIcon() {
  return (
    <svg className={styles.botIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden focusable="false">
      <path d="M12 4v3" />
      <rect x="4" y="7" width="16" height="12" rx="3" />
      <path d="M9 12v1.5M15 12v1.5" />
    </svg>
  )
}

function FilterChip({ label, count, active, onClick }: { label: string; count: number; active: boolean; onClick: () => void }) {
  return (
    <button type="button" className={styles.chip} data-active={active} aria-pressed={active} onClick={onClick}>
      {label}
      <span className={styles.chipCount}>{count}</span>
    </button>
  )
}

function SearchIcon() {
  return (
    <svg className={styles.searchIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden focusable="false">
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
  )
}

/** Quick-game listings never carry AI seats (vs-AI lobbies aren't public), so only tournaments report them. */
function aiSeatsOf(entry: PublicLobbyEntry): number {
  if (entry.kind !== 'tournament') return 0
  return Math.max(0, Math.min(entry.aiPlayerCount ?? 0, entry.playerCount))
}

function plural(n: number, word: string): string {
  return `${n} ${word}${n === 1 ? '' : 's'}`
}

function categoryOf(entry: PublicLobbyEntry): Category {
  if (entry.kind === 'quickGame') return entry.twoHeadedGiant ? 'multiplayer' : 'duel'
  return (entry.gameMode ?? 'TOURNAMENT') === 'TOURNAMENT' ? 'tournament' : 'multiplayer'
}

/** The table shape — the first thing that decides whether a lobby is for you. */
function modeLabel(entry: PublicLobbyEntry): string {
  if (entry.kind === 'quickGame') return entry.twoHeadedGiant ? 'Two-Headed Giant' : '1v1 Duel'
  switch (entry.gameMode ?? 'TOURNAMENT') {
    case 'FREE_FOR_ALL': return 'Free-for-All'
    case 'TWO_HEADED_GIANT': return 'Two-Headed Giant'
    case 'TEAM_VS_TEAM': return 'Team vs. Team'
    case 'TOURNAMENT': return 'Tournament'
  }
}

/** What is being played: the sets or cube for limited, the deck format for constructed. */
function lobbyTitle(entry: PublicLobbyEntry): string {
  if (entry.kind === 'quickGame') {
    if (entry.momirBasic) return 'Momir Basic'
    if (entry.format) return labelForFormat(entry.format)
    return 'Any deck'
  }
  if (entry.format === 'PREMADE_DECKS') return entry.deckFormat ? labelForFormat(entry.deckFormat) : 'Constructed'
  if (entry.cubeName) return entry.cubeName
  return entry.setNames.join(' + ') || formatTournamentFormat(entry.format)
}

/** The rules that shape the experience, as short tags. */
function lobbyTags(entry: PublicLobbyEntry): string[] {
  if (entry.kind === 'quickGame') {
    if (entry.momirBasic) return ['60 basics, random creatures']
    return [entry.format ? `${labelForFormat(entry.format)}-legal decks` : 'Bring your own deck']
  }
  const tags: string[] = []
  if (entry.format === 'PREMADE_DECKS') {
    tags.push('Bring your own deck')
  } else {
    tags.push(formatTournamentFormat(entry.format))
    if (entry.cubeName) tags.push('Cube')
    tags.push(`${entry.boosterCount} ${isDraft(entry.format) ? 'packs' : 'boosters'}`)
  }
  if ((entry.gameMode ?? 'TOURNAMENT') === 'TOURNAMENT') {
    if ((entry.bestOf ?? 1) > 1) tags.push(`Best of ${entry.bestOf}`)
    if (entry.gamesPerMatch > 1) tags.push(`Meet each player ${entry.gamesPerMatch}×`)
  }
  return tags
}

function searchText(entry: PublicLobbyEntry): string {
  const parts = [
    lobbyTitle(entry),
    modeLabel(entry),
    ...lobbyTags(entry),
    entry.hostName ?? '',
    ...(entry.playerNames ?? []),
    entry.lobbyId,
  ]
  if (entry.kind === 'tournament') parts.push(...entry.setNames)
  else if (entry.setCode) parts.push(entry.setCode)
  if (entry.ranked) parts.push('ranked')
  if (aiSeatsOf(entry) > 0) parts.push('ai bot')
  return parts.join(' ').toLowerCase()
}

function isDraft(format: TournamentFormat): boolean {
  return format === 'DRAFT' || format === 'COMMANDER_DRAFT' || format === 'WINSTON_DRAFT' || format === 'GRID_DRAFT'
}

function formatTournamentFormat(format: TournamentFormat): string {
  switch (format) {
    case 'WINSTON_DRAFT':
      return 'Winston Draft'
    case 'GRID_DRAFT':
      return 'Grid Draft'
    case 'DRAFT':
      return 'Draft'
    case 'COMMANDER_DRAFT':
      return 'Commander Draft'
    case 'SEALED':
      return 'Sealed'
    case 'COMMANDER_SEALED':
      return 'Commander Sealed'
    case 'PREMADE_DECKS':
      return 'Premade Decks'
  }
}

/** "just now", "4m", "1h 20m" — coarse on purpose; it only says whether the host is likely still there. */
function formatWaited(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours}h ${rest}m` : `${hours}h`
}
