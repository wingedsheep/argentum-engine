import { useMemo, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { BetweenGamesMessage, SealedCardInfo } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { ManaCost, ManaSymbol } from '../ui/ManaSymbols'
import { HoverCardPreview } from '../ui/HoverCardPreview'
import ls from '../draft/limitedScreen.module.css'
import styles from './SideboardingScreen.module.css'
import {
  adjustBasic,
  compareCards,
  countCards,
  deckChanges,
  deckGroupOf,
  FILTER_COLORS,
  matchesColorFilter,
  moveOne,
  type FilterColor,
  type CardCounts,
  type DeckGroup,
} from './sideboarding'

/**
 * Between the games of a best-of-three: the score, who plays first, and the deck for the next game.
 *
 * Sideboarding is a swap between two piles — the deck and everything else the player could have
 * played (the unplayed pool in Limited, CR 100.4b) — so the screen is exactly that: the sideboard as
 * a wall of cards to click in, the deck as a list to click out, basic lands on steppers. What the
 * changes amount to is spelled out next to the Ready button, because "did I actually take the
 * Giant Growth out?" is the question a player asks before committing.
 *
 * The server's snapshot is the truth once the player is ready; until then the piles are local, so an
 * opponent readying (which re-sends the snapshot) never clobbers edits in progress. The component is
 * keyed by game in [TournamentOverlay], so game 3 starts from what was played in game 2.
 */
export function SideboardingScreen({ snapshot }: { snapshot: BetweenGamesMessage }) {
  const submitMatchDeck = useGameStore((s) => s.submitMatchDeck)
  const editMatchDeck = useGameStore((s) => s.editMatchDeck)
  const leaveTournament = useGameStore((s) => s.leaveTournament)

  const [deck, setDeck] = useState<CardCounts>(snapshot.deck)
  const [sideboard, setSideboard] = useState<CardCounts>(snapshot.sideboard)
  const [playFirst, setPlayFirst] = useState(true)
  const [hover, setHover] = useState<{ card: SealedCardInfo; pos: { x: number; y: number } } | null>(null)
  const [confirmLeave, setConfirmLeave] = useState(false)
  const [colorFilter, setColorFilter] = useState<ReadonlySet<FilterColor>>(new Set())
  const toggleColor = (c: FilterColor) => setColorFilter((prev) => {
    const next = new Set(prev)
    if (next.has(c)) next.delete(c)
    else next.add(c)
    return next
  })

  const ready = snapshot.youAreReady
  const shownDeck = ready ? snapshot.deck : deck
  const shownSideboard = ready ? snapshot.sideboard : sideboard

  const cardsByName = useMemo(() => {
    const map = new Map<string, SealedCardInfo>()
    for (const c of snapshot.cards) map.set(c.name, c)
    for (const c of snapshot.basicLands) if (!map.has(c.name)) map.set(c.name, c)
    return map
  }, [snapshot.cards, snapshot.basicLands])
  const basicNames = useMemo(() => new Set(snapshot.basicLands.map((c) => c.name)), [snapshot.basicLands])

  const deckSize = countCards(shownDeck)
  const tooSmall = deckSize < snapshot.minDeckSize
  const changes = deckChanges(snapshot.registeredDeck, shownDeck)

  const bringIn = (name: string) => {
    if (ready) return
    const moved = moveOne(sideboard, deck, name)
    if (!moved) return
    setSideboard(moved[0]); setDeck(moved[1])
  }
  const takeOut = (name: string) => {
    if (ready) return
    if (basicNames.has(name)) { setDeck(adjustBasic(deck, name, -1)); return }
    const moved = moveOne(deck, sideboard, name)
    if (!moved) return
    setDeck(moved[0]); setSideboard(moved[1])
  }
  const resetToRegistered = () => {
    // Everything the player owns for this event is deck ∪ sideboard; rebuild the split around the
    // registered deck. Free basics simply drop out of the deck again.
    const all: Record<string, number> = {}
    for (const [n, c] of Object.entries(deck)) if (!basicNames.has(n)) all[n] = (all[n] ?? 0) + c
    for (const [n, c] of Object.entries(sideboard)) all[n] = (all[n] ?? 0) + c
    const nextSideboard: Record<string, number> = {}
    for (const [n, c] of Object.entries(all)) {
      const left = c - (basicNames.has(n) ? 0 : (snapshot.registeredDeck[n] ?? 0))
      if (left > 0) nextSideboard[n] = left
    }
    setDeck(snapshot.registeredDeck)
    setSideboard(nextSideboard)
  }
  const onReady = () => {
    submitMatchDeck(deck, snapshot.youChoosePlayDraw ? playFirst : undefined)
  }
  // Opened by movement only, not on enter: the screen appears under the cursor that just clicked
  // "Sideboard for game N", and a preview popping up unasked would cover the play/draw choice.
  const hoverProps = (card: SealedCardInfo | undefined) => card ? {
    onMouseMove: (e: React.MouseEvent) => setHover({ card, pos: { x: e.clientX, y: e.clientY } }),
    onMouseLeave: () => setHover(null),
  } : {}

  const allSideboardTiles = useMemo(() => Object.entries(shownSideboard)
    .filter(([, n]) => n > 0)
    .sort(([a], [b]) => compareCards(cardsByName.get(a), cardsByName.get(b), a, b)), [shownSideboard, cardsByName])
  const sideboardTiles = allSideboardTiles.filter(([name]) => matchesColorFilter(cardsByName.get(name), colorFilter))

  const deckGroups = useMemo(() => {
    const groups: Record<DeckGroup, Array<[string, number]>> = { Creatures: [], Spells: [], Lands: [] }
    for (const [name, count] of Object.entries(shownDeck)) {
      if (count <= 0 || basicNames.has(name)) continue
      groups[deckGroupOf(cardsByName.get(name))].push([name, count])
    }
    for (const list of Object.values(groups)) {
      list.sort(([a], [b]) => (cardsByName.get(a)?.cmc ?? 0) - (cardsByName.get(b)?.cmc ?? 0) || a.localeCompare(b))
    }
    return groups
  }, [shownDeck, cardsByName, basicNames])

  const result = snapshot.lastGameResult
  const resultLabel = result === 'WIN' ? 'You won' : result === 'LOSS' ? 'You lost' : 'Drawn'
  const previousGame = snapshot.nextGameNumber - 1
  const opp = snapshot.opponentName

  return (
    <div className={ls.screen} data-testid="sideboarding-screen">
      <header className={ls.header}>
        <div className={ls.headerGroup}>
          <h1 className={ls.title}>
            {snapshot.canSideboard ? 'Sideboarding' : 'Between games'}
            <span className={ls.titleSub}> · Round {snapshot.round} vs {opp}</span>
          </h1>
        </div>
        <div className={ls.headerGroup}>
          <span className={ls.pill} data-tone={snapshot.opponentReady ? 'good' : undefined} data-testid="opponent-status">
            {snapshot.opponentReady ? `${opp} is ready` : `${opp} is ${snapshot.canSideboard ? 'sideboarding' : 'getting ready'}…`}
          </span>
          {confirmLeave ? (
            <>
              <button type="button" className={ls.leave} onClick={leaveTournament}>Forfeit &amp; leave</button>
              <button type="button" className={ls.button} onClick={() => setConfirmLeave(false)}>Stay</button>
            </>
          ) : (
            <button type="button" className={ls.leave} onClick={() => setConfirmLeave(true)}>Leave</button>
          )}
        </div>
      </header>

      {/* ── Where the match stands, and who goes first ── */}
      <section className={styles.hero}>
        <div className={styles.scoreBlock}>
          <p className={styles.eyebrow} data-result={result}>
            Game {previousGame} · {resultLabel}
          </p>
          <div className={styles.score} aria-label={`Match score: you ${snapshot.yourGameWins}, ${opp} ${snapshot.opponentGameWins}`}>
            <span className={styles.scoreSide}>
              <span className={styles.scoreName}>You</span>
              <span className={styles.scoreNum}>{snapshot.yourGameWins}</span>
            </span>
            <span className={styles.scoreDash}>–</span>
            <span className={styles.scoreSide}>
              <span className={styles.scoreNum}>{snapshot.opponentGameWins}</span>
              <span className={styles.scoreName}>{opp}</span>
            </span>
          </div>
          <p className={styles.scoreNote}>
            Best of {snapshot.bestOf === 3 ? 'three' : snapshot.bestOf}
            {snapshot.drawnGames > 0 && ` · ${snapshot.drawnGames} drawn`}
            {' · '}Game {snapshot.nextGameNumber} next
          </p>
        </div>

        <div className={styles.playDraw}>
          {snapshot.youChoosePlayDraw ? (
            <>
              <p className={styles.playDrawLabel}>
                {result === 'DRAW' ? 'You chose last game, so you choose again' : 'You lost, so you choose'}
              </p>
              <div className={styles.segmented} role="radiogroup" aria-label="Who takes the first turn">
                <button
                  type="button"
                  role="radio"
                  aria-checked={ready ? snapshot.startingPlayer === 'YOU' : playFirst}
                  className={styles.segment}
                  disabled={ready}
                  onClick={() => setPlayFirst(true)}
                >
                  Play first
                </button>
                <button
                  type="button"
                  role="radio"
                  aria-checked={ready ? snapshot.startingPlayer === 'OPPONENT' : !playFirst}
                  className={styles.segment}
                  disabled={ready}
                  onClick={() => setPlayFirst(false)}
                >
                  Draw first
                </button>
              </div>
            </>
          ) : (
            <>
              <p className={styles.playDrawLabel}>Who plays first</p>
              <p className={styles.playDrawValue}>
                {snapshot.startingPlayer === 'YOU'
                  ? `${opp} chose to draw — you're on the play`
                  : snapshot.startingPlayer === 'OPPONENT'
                    ? `${opp} chose to play first`
                    : `${opp} ${result === 'DRAW' ? 'chooses again' : 'lost, so they choose'}`}
              </p>
            </>
          )}
        </div>
      </section>

      {/* ── The two piles ── */}
      {snapshot.canSideboard ? (
        <div className={styles.body} data-locked={ready || undefined}>
          <section className={styles.pool} aria-label="Sideboard">
            <div className={styles.sectionHead}>
              <span className={ls.sectionLabel}>Sideboard · {countCards(shownSideboard)}</span>
              <div className={styles.filters} role="group" aria-label="Filter the sideboard by colour">
                {FILTER_COLORS.map((c) => (
                  <button
                    key={c}
                    type="button"
                    className={styles.filterChip}
                    aria-pressed={colorFilter.has(c)}
                    onClick={() => toggleColor(c)}
                    title={c === 'C' ? 'Colourless' : `Show ${({ W: 'white', U: 'blue', B: 'black', R: 'red', G: 'green' } as const)[c]} cards`}
                  >
                    <ManaSymbol symbol={c} size={16} />
                  </button>
                ))}
              </div>
              <span className={styles.hint}>Click a card to bring it in</span>
            </div>
            {sideboardTiles.length === 0 ? (
              <p className={styles.empty}>
                {allSideboardTiles.length === 0 ? 'Nothing left to bring in.' : 'No sideboard cards in those colours.'}
              </p>
            ) : (
              <div className={styles.grid}>
                {sideboardTiles.map(([name, count]) => {
                  const card = cardsByName.get(name)
                  const wasOut = (snapshot.registeredDeck[name] ?? 0) > (shownDeck[name] ?? 0)
                  return (
                    <button
                      key={name}
                      type="button"
                      className={styles.tile}
                      onClick={() => bringIn(name)}
                      disabled={ready}
                      aria-label={`Bring in ${name}`}
                      {...hoverProps(card)}
                    >
                      <img src={getCardImageUrl(name, card?.imageUri ?? null, 'small')} alt={name} loading="lazy" />
                      {count > 1 && <span className={styles.count}>×{count}</span>}
                      {wasOut && <span className={styles.tag} data-kind="out">Out</span>}
                    </button>
                  )
                })}
              </div>
            )}
          </section>

          <aside className={`${styles.deck} ${ls.rail}`} aria-label="Deck">
            <div className={styles.sectionHead}>
              <span className={ls.sectionLabel}>Deck</span>
              <span className={styles.deckCount} data-short={tooSmall || undefined}>
                {deckSize}<span className={styles.deckMin}> / {snapshot.minDeckSize}</span>
              </span>
            </div>
            <div className={styles.deckScroll}>
              {(['Creatures', 'Spells', 'Lands'] as const).map((group) => {
                const rows = deckGroups[group]
                if (rows.length === 0) return null
                return (
                  <div key={group} className={styles.group}>
                    <div className={styles.groupHead}>
                      {group} <span>{rows.reduce((n, [, c]) => n + c, 0)}</span>
                    </div>
                    {rows.map(([name, count]) => {
                      const card = cardsByName.get(name)
                      const cameIn = (shownDeck[name] ?? 0) > (snapshot.registeredDeck[name] ?? 0)
                      return (
                        <button
                          key={name}
                          type="button"
                          className={styles.row}
                          onClick={() => takeOut(name)}
                          disabled={ready}
                          aria-label={`Take out ${name}`}
                          {...hoverProps(card)}
                        >
                          <span className={styles.rowCount}>{count}</span>
                          <span className={styles.rowName}>{name}</span>
                          {cameIn && <span className={styles.tag} data-kind="in">In</span>}
                          <span className={styles.rowCost}><ManaCost cost={card?.manaCost ?? null} size={12} /></span>
                        </button>
                      )
                    })}
                  </div>
                )
              })}

              {snapshot.basicLands.length > 0 && (
                <div className={styles.group}>
                  <div className={styles.groupHead}>
                    Basic lands <span>{snapshot.basicLands.reduce((n, b) => n + (shownDeck[b.name] ?? 0), 0)}</span>
                  </div>
                  {snapshot.basicLands.map((land) => {
                    const n = shownDeck[land.name] ?? 0
                    return (
                      <div key={land.name} className={styles.landRow} {...hoverProps(land)}>
                        <span className={styles.rowCount}>{n}</span>
                        <span className={styles.rowName}>{land.name}</span>
                        <button
                          type="button"
                          className={styles.stepper}
                          onClick={() => setDeck(adjustBasic(deck, land.name, -1))}
                          disabled={ready || n === 0}
                          aria-label={`Remove a ${land.name}`}
                        >
                          −
                        </button>
                        <button
                          type="button"
                          className={styles.stepper}
                          onClick={() => setDeck(adjustBasic(deck, land.name, 1))}
                          disabled={ready}
                          aria-label={`Add a ${land.name}`}
                        >
                          +
                        </button>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </aside>
        </div>
      ) : (
        <div className={styles.fixedBody}>
          <div className={`${ls.panel} ${styles.fixedPanel}`}>
            <p className={styles.fixedTitle}>Decks stay as registered</p>
            <p className={styles.fixedText}>This format doesn't allow sideboarding, so game {snapshot.nextGameNumber} is played with the same deck.</p>
          </div>
        </div>
      )}

      {/* ── Commit ── */}
      <footer className={`${ls.footer} ${styles.footer}`}>
        <div className={styles.changes} data-testid="sideboard-changes">
          {!snapshot.canSideboard ? null : changes.length === 0 ? (
            <span className={styles.noChanges}>Registered deck</span>
          ) : (
            <>
              <span className={styles.changesLabel}>Changes</span>
              {changes.map((c) => (
                <span key={c.name} className={styles.change} data-kind={c.delta > 0 ? 'in' : 'out'}>
                  {c.delta > 0 ? '+' : '−'}{Math.abs(c.delta) > 1 ? `${Math.abs(c.delta)} ` : ''}{c.name}
                </span>
              ))}
              {!ready && (
                <button type="button" className={styles.reset} onClick={resetToRegistered}>Reset</button>
              )}
            </>
          )}
        </div>
        <div className={styles.commit}>
          {ready ? (
            <>
              <span className={styles.waiting}>
                <span className={styles.dot} aria-hidden />
                {snapshot.opponentReady ? 'Starting…' : `Waiting for ${opp}`}
              </span>
              <button type="button" className={ls.button} onClick={editMatchDeck}>Keep editing</button>
            </>
          ) : (
            <button
              type="button"
              className={`${ls.primary} ${ls.large}`}
              onClick={onReady}
              disabled={snapshot.canSideboard && tooSmall}
              title={tooSmall ? `Your deck needs at least ${snapshot.minDeckSize} cards` : undefined}
              data-testid="sideboard-ready"
            >
              {snapshot.canSideboard && tooSmall
                ? `${snapshot.minDeckSize - deckSize} more card${snapshot.minDeckSize - deckSize === 1 ? '' : 's'} needed`
                : `Ready for game ${snapshot.nextGameNumber}`}
            </button>
          )}
        </div>
      </footer>

      {hover && (
        <HoverCardPreview name={hover.card.name} imageUri={hover.card.imageUri} pos={hover.pos} rulings={hover.card.rulings} />
      )}
    </div>
  )
}
