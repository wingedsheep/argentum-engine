/**
 * The games behind the "AI vs humans" numbers: a list of games (who sat down, with which colours, how it
 * went), the decks of any one of them, and one human's games against the AI. The list is the
 * "Recent games" panel and the body of a human's drawer; both open a game's decks in the shared
 * deck viewer ({@link GameDeckColumns}), so a surprising result can be read off the two decklists.
 *
 * The open game and human live in the query string (`?game=`, `?human=`) — see {@link useAdminParam} —
 * so the browser's Back closes them and a refresh or a trip to a replay comes back to them.
 */
import { useEffect, useState } from 'react'
import {
  type AiHumanGames,
  type AiRecentGame,
  type AiVsHumanFilter,
  fetchAiHumanGames,
  fetchMatchDecks,
} from '@/api/adminStats'
import type { AdminAuth } from '@/api/adminAuth'
import type { GameDecks } from '@/api/account'
import { GameDeckColumns } from '@/components/deck/GameDeckView'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { ModalShell } from './AdminPlayerDecks'
import { cellStyle } from './adminUi'
import { useAdminNav } from './adminNav'
import { colorLabel, gameModeLabel } from './statFormat'
import css from './aiVsHuman.module.css'

const OUTCOME_LABEL = { AI: 'AI won', HUMAN: 'Human won', DRAW: 'Draw' } as const

/** Open a finished game's replay in a new tab, so the admin keeps their place here. */
const watchReplay = (gameId: string) => window.open(`/replay/${gameId}`, '_blank', 'noopener')

function modeText(gameMode: string | null, format: string | null): string {
  const { primary, variant } = gameModeLabel(gameMode, format)
  return variant ? `${primary} › ${variant}` : primary
}

function when(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

/** A deck's colours as mana symbols — `C` for colourless, a dash when no deck was recorded. */
export function ColorPips({ colors }: { colors: string | null }) {
  if (colors == null) {
    return (
      <span className={css.pipsNone} title="No deck recorded">
        —
      </span>
    )
  }
  const label = colorLabel(colors)
  return (
    <span className={css.pips} title={label} aria-label={label} role="img">
      {(colors ? [...colors] : ['C']).map((c, i) => (
        <ManaSymbol key={i} symbol={c} size={15} />
      ))}
    </span>
  )
}

/**
 * Games as rows: when, mode, the AI's colours, the human(s) and their colours, the outcome, and the
 * deck/replay actions. `hideHumans` drops the human column inside one human's own drawer, where it
 * would repeat their name on every row — their colours stay, beside the AI's.
 */
export function GameList({
  games,
  onOpenGame,
  onOpenHuman,
  focusKey,
}: {
  games: readonly AiRecentGame[]
  onOpenGame: (matchId: number) => void
  onOpenHuman?: (key: string) => void
  /** Inside a human's drawer: their key, so the row shows their own colours instead of a human column. */
  focusKey?: string
}) {
  return (
    <div className={css.games} role="table" aria-label="Games">
      <div className={`${css.gameRow} ${css.gameHead}`} role="row">
        <span role="columnheader">When</span>
        <span role="columnheader">Mode</span>
        <span role="columnheader">AI deck</span>
        <span role="columnheader">{focusKey ? 'Their deck' : 'Human'}</span>
        <span role="columnheader">Result</span>
        <span role="columnheader" className={css.srOnly}>
          Actions
        </span>
      </div>
      {games.map((g) => {
        const own = focusKey ? g.humans.filter((h) => h.key === focusKey) : g.humans
        const others = focusKey ? g.humans.filter((h) => h.key !== focusKey) : []
        return (
          <div key={g.matchId} className={css.gameRow} role="row">
            <span className={css.gameWhen} role="cell">
              {when(g.endedAt)}
            </span>
            <span className={css.gameMode} role="cell" title={modeText(g.gameMode, g.format)}>
              {modeText(g.gameMode, g.format)}
            </span>
            <span className={css.gameSide} role="cell">
              <span className={css.gameSideLabel}>AI</span>
              {g.aiColors.length === 0 ? <ColorPips colors={null} /> : g.aiColors.map((c, i) => <ColorPips key={i} colors={c} />)}
            </span>
            <span className={css.gameHumans} role="cell">
              {own.map((h) => (
                <span key={h.key} className={css.gameHuman}>
                  {!focusKey &&
                    (onOpenHuman ? (
                      <button
                        type="button"
                        className={css.humanLink}
                        data-won={h.won}
                        onClick={() => onOpenHuman(h.key)}
                        title={`${h.name}'s games against the AI`}
                      >
                        {h.name}
                      </button>
                    ) : (
                      <span className={css.humanName} data-won={h.won}>
                        {h.name}
                      </span>
                    ))}
                  {!focusKey && h.isGuest && <span className={css.guest}>guest</span>}
                  <ColorPips colors={h.colors} />
                </span>
              ))}
              {others.length > 0 && (
                <span className={css.gameWith} title={others.map((h) => h.name).join(', ')}>
                  with {others.map((h) => h.name).join(', ')}
                </span>
              )}
            </span>
            <span role="cell">
              <span className={css.outcome} data-outcome={g.outcome}>
                {OUTCOME_LABEL[g.outcome]}
              </span>
            </span>
            <span className={css.gameActions} role="cell">
              <button type="button" className={css.deckButton} onClick={() => onOpenGame(g.matchId)}>
                Decks
              </button>
              {g.replayGameId ? (
                <button
                  type="button"
                  className={css.excludeButton}
                  onClick={() => watchReplay(g.replayGameId as string)}
                  title="Open the replay in a new tab"
                >
                  Replay ↗
                </button>
              ) : (
                <span className={css.noReplay} title="No replay was stored for this game">
                  No replay
                </span>
              )}
            </span>
          </div>
        )
      })}
    </div>
  )
}

/** Every seat's deck for one game, AI first, with a link to its replay. */
export function MatchDecksModal({ auth, matchId, onClose }: { auth: AdminAuth; matchId: number; onClose: () => void }) {
  const [decks, setDecks] = useState<GameDecks | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let live = true
    setDecks(null)
    setError(null)
    fetchMatchDecks(auth, matchId)
      .then((d) => live && setDecks(d))
      .catch(() => live && setError('Could not load this game’s decks.'))
    return () => {
      live = false
    }
  }, [auth, matchId])

  const ai = decks?.participants.filter((p) => p.isAi) ?? []
  const humans = decks?.participants.filter((p) => !p.isAi) ?? []
  const title = decks
    ? `${ai.length > 1 ? `${ai.length} AI seats` : 'Engine AI'} vs ${humans.map((p) => p.playerName).join(', ') || 'humans'}`
    : 'Game decks'
  const subtitle = decks
    ? `${modeText(decks.gameMode, decks.format ?? null)} · ${new Date(decks.endedAt).toLocaleString(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
      })}`
    : undefined
  const recorded = decks?.participants.some((p) => p.cards.length > 0) ?? false

  return (
    <ModalShell
      title={title}
      subtitle={subtitle}
      width={1040}
      onClose={onClose}
      actions={
        decks?.hasReplay ? (
          <button type="button" className={css.deckButton} onClick={() => watchReplay(decks.gameId)}>
            Watch replay ↗
          </button>
        ) : undefined
      }
    >
      {error ? (
        <p className={css.error}>{error}</p>
      ) : !decks ? (
        <p style={cellStyle.muted}>Loading…</p>
      ) : !recorded ? (
        <p style={cellStyle.muted}>No decklist was recorded for this game.</p>
      ) : (
        <GameDeckColumns participants={decks.participants} />
      )}
    </ModalShell>
  )
}

/**
 * One human's games against the AI — the same slice their row in the humans table counts — each
 * openable to both decks, with a way through to their full admin profile when they have an account.
 */
export function HumanGamesModal({
  auth,
  humanKey,
  days,
  rangeLabel,
  filter,
  onOpenGame,
  onClose,
}: {
  auth: AdminAuth
  humanKey: string
  days: number
  rangeLabel: string
  filter: AiVsHumanFilter
  onOpenGame: (matchId: number) => void
  onClose: () => void
}) {
  const { open } = useAdminNav()
  const [data, setData] = useState<AiHumanGames | null>(null)
  const [error, setError] = useState<string | null>(null)

  // The slice, not the filter object: leaving someone else out mustn't refetch this player's games.
  const { gameMode, format, setCode } = filter
  useEffect(() => {
    let live = true
    setData(null)
    setError(null)
    fetchAiHumanGames(auth, humanKey, days, { gameMode, format, setCode, excludeHumans: [], excludeTopHumans: 0 })
      .then((d) => live && setData(d))
      .catch(() => live && setError('Could not load this player’s games.'))
    return () => {
      live = false
    }
  }, [auth, humanKey, days, gameMode, format, setCode])

  const human = data?.human
  const fallbackName = humanKey.replace(/^(user|guest):/, '')
  const name = human?.name ?? data?.games[0]?.humans.find((h) => h.key === humanKey)?.name ?? (humanKey.startsWith('guest:') ? fallbackName : 'Player')
  const userId = humanKey.startsWith('user:') ? humanKey.slice('user:'.length) : null
  const decisive = human ? human.wins + human.losses : 0
  const slice = [
    days ? `last ${rangeLabel}` : 'all time',
    filter.gameMode ? modeText(filter.gameMode, filter.format) : null,
    filter.setCode,
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <ModalShell
      title={
        <>
          {name} <span className={css.vsAi}>vs the AI</span>
          {human?.isGuest && <span className={css.guest}>guest</span>}
        </>
      }
      subtitle={
        human ? (
          <>
            {human.games} game{human.games === 1 ? '' : 's'} · {human.wins}–{human.losses}–{human.draws}
            {decisive > 0 && ` · beats the AI ${Math.round((human.wins / decisive) * 100)}%`}
            <span className={css.sliceNote}> · {slice}</span>
          </>
        ) : (
          slice
        )
      }
      width={980}
      onClose={onClose}
      actions={
        userId ? (
          <button type="button" className={css.excludeButton} onClick={() => open(`players/${encodeURIComponent(userId)}`)}>
            Full profile →
          </button>
        ) : undefined
      }
    >
      {error ? (
        <p className={css.error}>{error}</p>
      ) : !data ? (
        <p style={cellStyle.muted}>Loading…</p>
      ) : data.games.length === 0 ? (
        <p style={cellStyle.muted}>No games against the AI in this slice.</p>
      ) : (
        <>
          <GameList games={data.games} onOpenGame={onOpenGame} focusKey={humanKey} />
          {human && human.games > data.games.length && (
            <p className={css.listNote}>
              Showing the latest {data.games.length} of {human.games} games.
            </p>
          )}
        </>
      )}
    </ModalShell>
  )
}
