import { useState, useMemo, useCallback, useEffect } from 'react'
import { useGameStore, type DeckBuildingState } from '../../store/gameStore'
import type { SealedCardInfo } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'
import { ManaSymbol, ManaCost } from '../ui/ManaSymbols'

/**
 * Deck Builder overlay for sealed draft mode.
 */
export function DeckBuilderOverlay() {
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)

  if (!deckBuildingState) return null

  if (deckBuildingState.phase === 'waiting') {
    return <WaitingForOpponent setName={deckBuildingState.setName} />
  }

  return <DeckBuilder state={deckBuildingState} />
}

/**
 * Waiting screen shown to the first player while waiting for opponent.
 */
function WaitingForOpponent({ setName }: { setName: string }) {
  const sessionId = useGameStore((state) => state.sessionId)
  const cancelGame = useGameStore((state) => state.cancelGame)
  const responsive = useResponsive()

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 24,
        padding: responsive.containerPadding,
        zIndex: 1000,
      }}
    >
      <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 20 : 28 }}>
        Sealed Draft - {setName}
      </h2>
      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.large }}>
        Waiting for opponent to join...
      </p>
      <div
        style={{
          backgroundColor: '#222',
          padding: '16px 24px',
          borderRadius: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 8,
        }}
      >
        <p style={{ color: '#aaa', margin: 0, fontSize: responsive.fontSize.normal }}>
          Session ID:
        </p>
        <code
          style={{
            color: '#4fc3f7',
            fontSize: responsive.isMobile ? 14 : 18,
            backgroundColor: '#333',
            padding: '8px 16px',
            borderRadius: 4,
            fontFamily: 'monospace',
            userSelect: 'all',
          }}
        >
          {sessionId}
        </code>
        <p style={{ color: '#666', margin: 0, fontSize: responsive.fontSize.small }}>
          Share this ID with your opponent
        </p>
      </div>
      <button
        onClick={cancelGame}
        style={{
          marginTop: 8,
          padding: '10px 20px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#c0392b',
          color: 'white',
          border: 'none',
          borderRadius: 4,
          cursor: 'pointer',
        }}
      >
        Cancel Game
      </button>
    </div>
  )
}

/**
 * Main deck builder interface.
 */
function DeckBuilder({ state }: { state: DeckBuildingState }) {
  const responsive = useResponsive()
  const addCardToDeck = useGameStore((s) => s.addCardToDeck)
  const removeCardFromDeck = useGameStore((s) => s.removeCardFromDeck)
  const setLandCount = useGameStore((s) => s.setLandCount)
  const submitSealedDeck = useGameStore((s) => s.submitSealedDeck)
  const unsubmitDeck = useGameStore((s) => s.unsubmitDeck)
  const leaveLobby = useGameStore((s) => s.leaveLobby)
  const stopLobby = useGameStore((s) => s.stopLobby)
  const cancelGame = useGameStore((s) => s.cancelGame)
  const lobbyState = useGameStore((s) => s.lobbyState)
  const isInLobby = lobbyState !== null
  const isHost = lobbyState?.isHost ?? false

  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [sortBy, setSortBy] = useState<'color' | 'cmc' | 'rarity'>('rarity')
  const [colorFilter, setColorFilter] = useState<Set<string>>(new Set())
  const [typeFilter, setTypeFilter] = useState<string | null>(null)

  const handleHover = useCallback((card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard(card)
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }, [])

  // Count cards in deck
  const nonLandCount = state.deck.length
  const landCount = Object.values(state.landCounts).reduce((a, b) => a + b, 0)
  const totalCount = nonLandCount + landCount
  const isValidDeck = totalCount >= 40

  // Deck analytics
  const deckAnalytics = useMemo(() => {
    const cardInfos: SealedCardInfo[] = []
    for (const cardName of state.deck) {
      const info = state.cardPool.find((c) => c.name === cardName)
      if (info) cardInfos.push(info)
    }

    let creatureCount = 0
    let nonCreatureCount = 0
    for (const card of cardInfos) {
      if (card.typeLine.toLowerCase().includes('creature')) {
        creatureCount++
      } else {
        nonCreatureCount++
      }
    }

    // Mana curve
    const curve: Record<number, number> = {}
    for (const card of cardInfos) {
      const cmc = Math.min(getCmc(card), 7)
      curve[cmc] = (curve[cmc] || 0) + 1
    }

    // Color symbol counts in deck (for mana distribution)
    const colorSymbols: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }
    for (const card of cardInfos) {
      const cost = card.manaCost || ''
      const matches = cost.match(/\{([^}]+)\}/g) || []
      for (const match of matches) {
        const inner = match.slice(1, -1)
        if (inner in colorSymbols) {
          colorSymbols[inner] = (colorSymbols[inner] ?? 0) + 1
        }
      }
    }

    // Land color counts
    const landColors: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }
    const landColorMap: Record<string, string[]> = {
      Plains: ['W'], Island: ['U'], Swamp: ['B'], Mountain: ['R'], Forest: ['G'],
    }
    for (const [landName, count] of Object.entries(state.landCounts)) {
      const colors = landColorMap[landName]
      if (colors) {
        for (const c of colors) {
          landColors[c] = (landColors[c] ?? 0) + count
        }
      }
    }

    return { creatureCount, nonCreatureCount, curve, colorSymbols, landColors }
  }, [state.deck, state.cardPool, state.landCounts])

  // Group and sort pool cards
  const poolCardGroups = useMemo(() => {
    const deckCardCounts = state.deck.reduce<Record<string, number>>((acc, name) => {
      acc[name] = (acc[name] || 0) + 1
      return acc
    }, {})

    const poolCardCounts: Record<string, { card: SealedCardInfo; totalCount: number }> = {}
    for (const card of state.cardPool) {
      const existing = poolCardCounts[card.name]
      if (existing) {
        existing.totalCount++
      } else {
        poolCardCounts[card.name] = { card, totalCount: 1 }
      }
    }

    const groups: { card: SealedCardInfo; availableCount: number }[] = []
    for (const [name, { card, totalCount }] of Object.entries(poolCardCounts)) {
      const inDeckCount = deckCardCounts[name] || 0
      const availableCount = totalCount - inDeckCount
      if (availableCount > 0) {
        if (colorFilter.size > 0) {
          const cardColors = getCardColors(card)
          let matches = false
          if (colorFilter.has('C')) {
            matches = matches || cardColors.size === 0
          }
          for (const c of ['W', 'U', 'B', 'R', 'G']) {
            if (colorFilter.has(c) && cardColors.has(c)) {
              matches = true
            }
          }
          if (!matches) continue
        }
        if (typeFilter) {
          if (!matchesTypeFilter(card, typeFilter)) continue
        }
        groups.push({ card, availableCount })
      }
    }

    return groups.sort((a, b) => {
      if (sortBy === 'color') {
        return getColorOrder(a.card) - getColorOrder(b.card) || getCmc(a.card) - getCmc(b.card)
      } else if (sortBy === 'cmc') {
        return getCmc(a.card) - getCmc(b.card)
      } else {
        return getRarityOrder(a.card) - getRarityOrder(b.card) || getCmc(a.card) - getCmc(b.card)
      }
    })
  }, [state.cardPool, state.deck, sortBy, colorFilter, typeFilter])

  const totalPoolCards = poolCardGroups.reduce((sum, g) => sum + g.availableCount, 0)

  const poolByRarity = useMemo(() => {
    if (sortBy !== 'rarity') return null

    type PoolGroup = { card: SealedCardInfo; availableCount: number }
    const mythic: PoolGroup[] = []
    const rare: PoolGroup[] = []
    const uncommon: PoolGroup[] = []
    const common: PoolGroup[] = []

    for (const group of poolCardGroups) {
      const rarity = group.card.rarity.toUpperCase()
      if (rarity === 'MYTHIC') mythic.push(group)
      else if (rarity === 'RARE') rare.push(group)
      else if (rarity === 'UNCOMMON') uncommon.push(group)
      else common.push(group)
    }

    return { MYTHIC: mythic, RARE: rare, UNCOMMON: uncommon, COMMON: common }
  }, [poolCardGroups, sortBy])

  // Group deck cards by name for vertical list
  const deckCardGroups = useMemo(() => {
    const groups: Record<string, { card: SealedCardInfo; count: number }> = {}
    for (const cardName of state.deck) {
      if (!groups[cardName]) {
        const cardInfo = state.cardPool.find((c) => c.name === cardName)
        if (cardInfo) {
          groups[cardName] = { card: cardInfo, count: 0 }
        }
      }
      if (groups[cardName]) {
        groups[cardName].count++
      }
    }
    return Object.values(groups).sort((a, b) => getCmc(a.card) - getCmc(b.card) || a.card.name.localeCompare(b.card.name))
  }, [state.deck, state.cardPool])

  const isSubmitted = state.phase === 'submitted'

  // Max bar height for mana curve
  const maxCurveCount = Math.max(1, ...Object.values(deckAnalytics.curve))
  const totalColorSymbols = Object.values(deckAnalytics.colorSymbols).reduce((a, b) => a + b, 0)

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: '#1a1a1a',
        display: 'flex',
        flexDirection: 'column',
        zIndex: 1000,
        overflow: 'hidden',
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: responsive.isMobile ? '6px 12px' : '8px 24px',
          backgroundColor: '#222',
          borderBottom: '1px solid #444',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 8,
        }}
      >
        <div>
          <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 16 : 20 }}>
            Deck Builder - {state.setName}
          </h2>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {state.opponentReady && (
            <span style={{ color: '#4caf50', fontSize: responsive.fontSize.small }}>
              Opponent ready
            </span>
          )}

          <div
            style={{
              padding: '6px 14px',
              backgroundColor: isValidDeck ? '#2e7d32' : '#555',
              borderRadius: 6,
              color: 'white',
              fontWeight: 600,
              fontSize: responsive.fontSize.normal,
            }}
          >
            {totalCount} / 40
          </div>

          {isSubmitted ? (
            <button
              onClick={unsubmitDeck}
              style={{
                padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#ff9800',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Edit Deck
            </button>
          ) : (
            <button
              onClick={submitSealedDeck}
              disabled={!isValidDeck}
              style={{
                padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: isValidDeck ? '#4caf50' : '#555',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: isValidDeck ? 'pointer' : 'not-allowed',
                fontWeight: 600,
              }}
            >
              Submit Deck
            </button>
          )}

          {isInLobby ? (
            isHost ? (
              <button
                onClick={stopLobby}
                style={{
                  padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: '#c0392b',
                  color: 'white',
                  border: 'none',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontWeight: 600,
                }}
              >
                Stop Game
              </button>
            ) : (
              <button
                onClick={leaveLobby}
                style={{
                  padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: '#c0392b',
                  color: 'white',
                  border: 'none',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontWeight: 600,
                }}
              >
                Leave
              </button>
            )
          ) : (
            <button
              onClick={cancelGame}
              style={{
                padding: responsive.isMobile ? '6px 14px' : '8px 20px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#c0392b',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Cancel Game
            </button>
          )}
        </div>
      </div>

      {/* Main content: Pool (left ~70%) | Deck (right ~30%) */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: responsive.isMobile ? 'column' : 'row',
          overflow: 'hidden',
        }}
      >
        {/* Card Pool (left) */}
        <div
          style={{
            flex: responsive.isMobile ? 1 : 7,
            display: 'flex',
            flexDirection: 'column',
            borderRight: responsive.isMobile ? 'none' : '1px solid #444',
            borderBottom: responsive.isMobile ? '1px solid #444' : 'none',
            minHeight: 0,
            minWidth: 0,
          }}
        >
          {/* Sort + Filter toolbar */}
          <div
            style={{
              padding: '6px 12px',
              backgroundColor: '#2a2a2a',
              borderBottom: '1px solid #333',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              flexWrap: 'wrap',
            }}
          >
            <span style={{ color: '#888', fontSize: 12 }}>Sort:</span>
            {(['color', 'cmc', 'rarity'] as const).map((option) => (
              <button
                key={option}
                onClick={() => setSortBy(option)}
                style={{
                  padding: '3px 10px',
                  fontSize: 12,
                  backgroundColor: sortBy === option ? '#4fc3f7' : '#444',
                  color: sortBy === option ? '#000' : '#ccc',
                  border: 'none',
                  borderRadius: 4,
                  cursor: 'pointer',
                  textTransform: 'capitalize',
                }}
              >
                {option}
              </button>
            ))}

            <div style={{ width: 1, height: 18, backgroundColor: '#444', margin: '0 4px' }} />

            <span style={{ color: '#888', fontSize: 12 }}>Filter:</span>
            {COLOR_FILTER_OPTIONS.map(({ key, label }) => {
              const active = colorFilter.has(key)
              return (
                <button
                  key={key}
                  onClick={() => {
                    setColorFilter((prev) => {
                      const next = new Set(prev)
                      if (next.has(key)) next.delete(key)
                      else next.add(key)
                      return next
                    })
                  }}
                  title={label}
                  style={{
                    width: 28,
                    height: 28,
                    borderRadius: '50%',
                    border: active ? '2px solid #4fc3f7' : '2px solid transparent',
                    backgroundColor: active ? 'rgba(79, 195, 247, 0.15)' : 'transparent',
                    cursor: 'pointer',
                    opacity: active ? 1 : 0.5,
                    transition: 'all 0.15s',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: 0,
                  }}
                >
                  <ManaSymbol symbol={key === 'M' ? 'WUBRG' : key} size={20} />
                </button>
              )
            })}
            {colorFilter.size > 0 && (
              <button
                onClick={() => setColorFilter(new Set())}
                style={{
                  padding: '2px 8px',
                  fontSize: 11,
                  backgroundColor: 'transparent',
                  color: '#888',
                  border: '1px solid #555',
                  borderRadius: 4,
                  cursor: 'pointer',
                }}
              >
                Clear
              </button>
            )}

            <div style={{ width: 1, height: 18, backgroundColor: '#444', margin: '0 4px' }} />

            <span style={{ color: '#888', fontSize: 12 }}>Type:</span>
            {TYPE_FILTER_OPTIONS.map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setTypeFilter(typeFilter === key ? null : key)}
                style={{
                  padding: '3px 10px',
                  fontSize: 11,
                  backgroundColor: typeFilter === key ? '#4fc3f7' : '#444',
                  color: typeFilter === key ? '#000' : '#ccc',
                  border: 'none',
                  borderRadius: 4,
                  cursor: 'pointer',
                }}
              >
                {label}
              </button>
            ))}

            <span style={{ color: '#666', fontSize: 12, marginLeft: 'auto' }}>
              Pool: {totalPoolCards}
            </span>
          </div>

          {/* Pool cards */}
          <div
            style={{
              flex: 1,
              overflow: 'auto',
              padding: 8,
            }}
          >
            {sortBy === 'rarity' && poolByRarity ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {(['MYTHIC', 'RARE', 'UNCOMMON', 'COMMON'] as const).map((rarity) => {
                  const groups = poolByRarity[rarity] ?? []
                  if (groups.length === 0) return null
                  const totalInSection = groups.reduce((sum, g) => sum + g.availableCount, 0)
                  return (
                    <div key={rarity}>
                      <RaritySectionHeader rarity={rarity} count={totalInSection} />
                      <div
                        style={{
                          display: 'flex',
                          flexWrap: 'wrap',
                          gap: 6,
                          justifyContent: 'flex-start',
                        }}
                      >
                        {groups.map(({ card, availableCount }) => (
                          <PoolCard
                            key={card.name}
                            card={card}
                            count={availableCount}
                            onClick={() => !isSubmitted && addCardToDeck(card.name)}
                            onHover={handleHover}
                            disabled={isSubmitted}
                          />
                        ))}
                      </div>
                    </div>
                  )
                })}
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: 6,
                  justifyContent: 'flex-start',
                }}
              >
                {poolCardGroups.map(({ card, availableCount }) => (
                  <PoolCard
                    key={card.name}
                    card={card}
                    count={availableCount}
                    onClick={() => !isSubmitted && addCardToDeck(card.name)}
                    onHover={handleHover}
                    disabled={isSubmitted}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Deck Panel (right) */}
        <div
          style={{
            flex: responsive.isMobile ? 1 : 3,
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: '#1e1e1e',
            minHeight: 0,
            minWidth: 0,
          }}
        >
          {/* Deck Analytics */}
          <div
            style={{
              padding: '8px 12px',
              backgroundColor: '#252525',
              borderBottom: '1px solid #333',
            }}
          >
            {/* Live counters */}
            <div style={{ display: 'flex', gap: 12, marginBottom: 8, flexWrap: 'wrap' }}>
              <DeckStat label="Creatures" value={deckAnalytics.creatureCount} color="#8bc34a" />
              <DeckStat label="Spells" value={deckAnalytics.nonCreatureCount} color="#4fc3f7" />
              <DeckStat label="Lands" value={landCount} color="#a1887f" />
            </div>

            {/* Mana curve histogram */}
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 48 }}>
              {[0, 1, 2, 3, 4, 5, 6, 7].map((cmc) => {
                const count = deckAnalytics.curve[cmc] || 0
                const height = maxCurveCount > 0 ? (count / maxCurveCount) * 40 : 0
                return (
                  <div key={cmc} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1, minWidth: 0 }}>
                    {count > 0 && (
                      <span style={{ color: '#aaa', fontSize: 9, marginBottom: 2 }}>{count}</span>
                    )}
                    <div
                      style={{
                        width: '100%',
                        maxWidth: 24,
                        height: Math.max(height, count > 0 ? 3 : 0),
                        backgroundColor: count > 0 ? '#4fc3f7' : 'transparent',
                        borderRadius: '2px 2px 0 0',
                        transition: 'height 0.2s',
                      }}
                    />
                    <span style={{ color: '#666', fontSize: 9, marginTop: 2 }}>
                      {cmc >= 7 ? '7+' : cmc}
                    </span>
                  </div>
                )
              })}
            </div>

            {/* Color distribution vs lands */}
            {totalColorSymbols > 0 && (
              <div style={{ marginTop: 8 }}>
                <div style={{ display: 'flex', gap: 1, height: 6, borderRadius: 3, overflow: 'hidden' }}>
                  {(['W', 'U', 'B', 'R', 'G'] as const).map((c) => {
                    const pct = ((deckAnalytics.colorSymbols[c] ?? 0) / totalColorSymbols) * 100
                    if (pct === 0) return null
                    return (
                      <div
                        key={c}
                        style={{
                          flex: pct,
                          backgroundColor: MANA_COLORS[c],
                          transition: 'flex 0.2s',
                        }}
                        title={`${c}: ${deckAnalytics.colorSymbols[c]} symbols (${Math.round(pct)}%)`}
                      />
                    )
                  })}
                </div>
                <div style={{ display: 'flex', gap: 1, height: 6, borderRadius: 3, overflow: 'hidden', marginTop: 2 }}>
                  {(['W', 'U', 'B', 'R', 'G'] as const).map((c) => {
                    const count = deckAnalytics.landColors[c] ?? 0
                    if (count === 0) return null
                    return (
                      <div
                        key={c}
                        style={{
                          flex: count,
                          backgroundColor: MANA_COLORS[c],
                          opacity: 0.5,
                          transition: 'flex 0.2s',
                        }}
                        title={`${c} lands: ${count}`}
                      />
                    )
                  })}
                  {Object.values(deckAnalytics.landColors).every((v) => v === 0) && (
                    <div style={{ flex: 1, backgroundColor: '#333' }} />
                  )}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 2 }}>
                  <span style={{ color: '#555', fontSize: 9 }}>Spells</span>
                  <span style={{ color: '#555', fontSize: 9 }}>Lands</span>
                </div>
              </div>
            )}
          </div>

          {/* Deck header */}
          <div
            style={{
              padding: '6px 12px',
              backgroundColor: '#222',
              borderBottom: '1px solid #333',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            <span style={{ color: '#ccc', fontSize: 13, fontWeight: 600 }}>
              Your Deck
            </span>
            <span style={{ color: '#888', fontSize: 11 }}>
              ({nonLandCount} spells + {landCount} lands)
            </span>
          </div>

          {/* Deck card list (vertical) */}
          <div
            style={{
              flex: 1,
              overflow: 'auto',
              padding: '4px 0',
            }}
          >
            {deckCardGroups.map(({ card, count }) => (
              <DeckListRow
                key={card.name}
                card={card}
                count={count}
                onClick={() => !isSubmitted && removeCardFromDeck(card.name)}
                onHover={handleHover}
                disabled={isSubmitted}
              />
            ))}

            {/* Basic lands */}
            <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px solid #333' }}>
              <div style={{ padding: '0 12px 4px', display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ color: '#666', fontSize: 10, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1 }}>
                  Basic Lands
                </span>
                {!isSubmitted && nonLandCount > 0 && (
                  <button
                    onClick={() => suggestLands(state, nonLandCount, setLandCount)}
                    style={{
                      padding: '2px 8px',
                      fontSize: 10,
                      backgroundColor: '#444',
                      color: '#4fc3f7',
                      border: '1px solid #555',
                      borderRadius: 4,
                      cursor: 'pointer',
                    }}
                  >
                    Suggest
                  </button>
                )}
              </div>
              {state.basicLands.map((land) => (
                <LandRow
                  key={land.name}
                  land={land}
                  count={state.landCounts[land.name] || 0}
                  onIncrement={() => !isSubmitted && setLandCount(land.name, (state.landCounts[land.name] || 0) + 1)}
                  onDecrement={() => !isSubmitted && setLandCount(land.name, (state.landCounts[land.name] || 0) - 1)}
                  onHover={handleHover}
                  disabled={isSubmitted}
                />
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Card preview on hover - positioned near cursor */}
      {hoveredCard && !responsive.isMobile && (
        <CardPreview card={hoveredCard} pos={hoverPos} />
      )}

      {/* Submitted overlay */}
      {isSubmitted && (
        <div
          style={{
            position: 'absolute',
            bottom: 24,
            left: '50%',
            transform: 'translateX(-50%)',
            backgroundColor: 'rgba(76, 175, 80, 0.9)',
            color: 'white',
            padding: '12px 24px',
            borderRadius: 8,
            fontWeight: 600,
            fontSize: responsive.fontSize.large,
          }}
        >
          Deck submitted! Waiting for opponent...
        </div>
      )}
    </div>
  )
}

/**
 * Small stat display for deck analytics.
 */
function DeckStat({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
      <span style={{ color, fontSize: 14, fontWeight: 700 }}>{value}</span>
      <span style={{ color: '#888', fontSize: 10 }}>{label}</span>
    </div>
  )
}

/**
 * Card in the pool (available to add to deck).
 */
function PoolCard({
  card,
  count,
  onClick,
  onHover,
  disabled,
}: {
  card: SealedCardInfo
  count: number
  onClick: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  disabled: boolean
}) {
  const cardWidth = 110
  const cardHeight = Math.round(cardWidth * 1.4)
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'small')

  return (
    <div
      onClick={disabled ? undefined : onClick}
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        position: 'relative',
        width: cardWidth,
        height: cardHeight,
        borderRadius: 6,
        overflow: 'hidden',
        cursor: disabled ? 'default' : 'pointer',
        border: '2px solid #444',
        opacity: disabled ? 0.6 : 1,
        transition: 'all 0.15s',
      }}
      onMouseOver={(e) => {
        if (!disabled) {
          e.currentTarget.style.border = '2px solid #4fc3f7'
          e.currentTarget.style.transform = 'scale(1.05)'
        }
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.border = '2px solid #444'
        e.currentTarget.style.transform = 'scale(1)'
      }}
    >
      <img
        src={imageUrl}
        alt={card.name}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
        }}
      />
      {count > 1 && (
        <div
          style={{
            position: 'absolute',
            bottom: 4,
            right: 4,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            color: '#4fc3f7',
            borderRadius: 4,
            padding: '2px 6px',
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          x{count}
        </div>
      )}
    </div>
  )
}

/**
 * Compact deck list row with card image strip.
 */
function DeckListRow({
  card,
  count,
  onClick,
  onHover,
  disabled,
}: {
  card: SealedCardInfo
  count: number
  onClick: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  disabled: boolean
}) {
  const cmc = getCmc(card)

  return (
    <div
      onClick={disabled ? undefined : onClick}
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        display: 'flex',
        alignItems: 'center',
        height: 28,
        padding: '0 8px 0 0',
        cursor: disabled ? 'default' : 'pointer',
        opacity: disabled ? 0.8 : 1,
        position: 'relative',
        overflow: 'hidden',
        borderBottom: '1px solid #2a2a2a',
      }}
      onMouseOver={(e) => {
        if (!disabled) e.currentTarget.style.backgroundColor = 'rgba(79, 195, 247, 0.1)'
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.backgroundColor = 'transparent'
      }}
    >
      {/* Count */}
      <span
        style={{
          width: 28,
          textAlign: 'center',
          color: '#4fc3f7',
          fontWeight: 600,
          fontSize: 12,
          flexShrink: 0,
        }}
      >
        {count}
      </span>
      {/* Card name */}
      <span style={{
        color: '#ddd',
        fontSize: 12,
        flex: 1,
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
      }}>
        {card.name}
      </span>
      {/* Mana cost */}
      <span style={{ marginLeft: 4, flexShrink: 0 }}>
        {card.manaCost ? <ManaCost cost={card.manaCost} size={12} /> : <span style={{ color: '#666', fontSize: 10 }}>({cmc})</span>}
      </span>
    </div>
  )
}

/**
 * Land row in the deck list with +/- buttons.
 */
function LandRow({
  land,
  count,
  onIncrement,
  onDecrement,
  onHover,
  disabled,
}: {
  land: SealedCardInfo
  count: number
  onIncrement: () => void
  onDecrement: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  disabled: boolean
}) {
  return (
    <div
      onMouseEnter={(e) => onHover(land, e)}
      onMouseMove={(e) => onHover(land, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        display: 'flex',
        alignItems: 'center',
        height: 28,
        padding: '0 8px',
        borderBottom: '1px solid #2a2a2a',
      }}
    >
      {/* Count */}
      <span style={{ width: 28, textAlign: 'center', color: '#a1887f', fontWeight: 600, fontSize: 12, flexShrink: 0 }}>
        {count}
      </span>
      {/* Name */}
      <span style={{ color: '#aaa', fontSize: 12, flex: 1 }}>{land.name}</span>
      {/* Buttons */}
      <div style={{ display: 'flex', gap: 2, flexShrink: 0 }}>
        <button
          onClick={disabled ? undefined : onDecrement}
          disabled={disabled || count <= 0}
          style={{
            width: 20,
            height: 20,
            borderRadius: 3,
            border: 'none',
            backgroundColor: count > 0 && !disabled ? '#555' : '#333',
            color: count > 0 && !disabled ? 'white' : '#555',
            cursor: count > 0 && !disabled ? 'pointer' : 'not-allowed',
            fontWeight: 600,
            fontSize: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 0,
          }}
        >
          -
        </button>
        <button
          onClick={disabled ? undefined : onIncrement}
          disabled={disabled}
          style={{
            width: 20,
            height: 20,
            borderRadius: 3,
            border: 'none',
            backgroundColor: disabled ? '#333' : '#4caf50',
            color: disabled ? '#555' : 'white',
            cursor: disabled ? 'not-allowed' : 'pointer',
            fontWeight: 600,
            fontSize: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 0,
          }}
        >
          +
        </button>
      </div>
    </div>
  )
}

/**
 * Section header for rarity grouping.
 */
function RaritySectionHeader({ rarity, count }: { rarity: string; count: number }) {
  const colors: Record<string, string> = {
    MYTHIC: '#ff8b00',
    RARE: '#ffd700',
    UNCOMMON: '#c0c0c0',
    COMMON: '#888888',
  }

  const labels: Record<string, string> = {
    MYTHIC: 'Mythic Rare',
    RARE: 'Rare',
    UNCOMMON: 'Uncommon',
    COMMON: 'Common',
  }

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        marginBottom: 6,
        paddingBottom: 4,
        borderBottom: `2px solid ${colors[rarity] || '#888'}`,
      }}
    >
      <span style={{ color: colors[rarity] || '#888', fontWeight: 600, fontSize: 13 }}>
        {labels[rarity] || rarity}
      </span>
      <span style={{ color: '#666', fontSize: 11 }}>({count})</span>
    </div>
  )
}

/**
 * Card preview on hover, positioned near the cursor.
 * Shows rulings after hovering for 1 second.
 */
function CardPreview({ card, pos }: { card: SealedCardInfo; pos: { x: number; y: number } | null }) {
  const [showRulings, setShowRulings] = useState(false)
  const [lastCardName, setLastCardName] = useState<string | null>(null)

  // Show rulings after hovering for 1 second
  useEffect(() => {
    if (card.name !== lastCardName) {
      setLastCardName(card.name)
      setShowRulings(false)
    }

    const timer = setTimeout(() => {
      setShowRulings(true)
    }, 1000)

    return () => clearTimeout(timer)
  }, [card.name, lastCardName])

  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'large')
  const previewWidth = 250
  const previewHeight = Math.round(previewWidth * 1.4)
  const hasRulings = card.rulings && card.rulings.length > 0

  // Position the preview near the cursor but keep it on screen
  let top = 80
  let left = 20
  if (pos) {
    // Place to the right of cursor, or to the left if too close to right edge
    const margin = 20
    if (pos.x + previewWidth + margin + 20 < window.innerWidth) {
      left = pos.x + margin
    } else {
      left = pos.x - previewWidth - margin
    }
    // Vertically centered on cursor, clamped to viewport
    top = Math.max(10, Math.min(pos.y - previewHeight / 2, window.innerHeight - previewHeight - 10))
  }

  return (
    <div
      style={{
        position: 'fixed',
        top,
        left,
        pointerEvents: 'none',
        zIndex: 1001,
        transition: 'top 0.05s, left 0.05s',
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
      }}
    >
      <div
        style={{
          width: previewWidth,
          height: previewHeight,
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.8)',
        }}
      >
        <img
          src={imageUrl}
          alt={card.name}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      </div>

      {/* Rulings panel - appears after 1 second of hovering */}
      {showRulings && hasRulings && (
        <div style={cardPreviewStyles.rulings}>
          <div style={cardPreviewStyles.rulingsHeader}>Rulings</div>
          {card.rulings!.map((ruling, index) => (
            <div key={index} style={cardPreviewStyles.ruling}>
              <div style={cardPreviewStyles.rulingDate}>{ruling.date}</div>
              <div style={cardPreviewStyles.rulingText}>{ruling.text}</div>
            </div>
          ))}
        </div>
      )}

      {/* Rulings indicator - shows immediately if card has rulings */}
      {!showRulings && hasRulings && (
        <div style={cardPreviewStyles.rulingsHint}>
          Hold to see rulings...
        </div>
      )}
    </div>
  )
}

const cardPreviewStyles = {
  rulings: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.92)',
    padding: 12,
    borderRadius: 8,
    border: '1px solid rgba(100, 150, 255, 0.3)',
    maxWidth: 320,
    maxHeight: 300,
    overflowY: 'auto' as const,
  },
  rulingsHeader: {
    color: '#6699ff',
    fontWeight: 700,
    fontSize: 13,
    textTransform: 'uppercase' as const,
    letterSpacing: 1,
    borderBottom: '1px solid rgba(100, 150, 255, 0.2)',
    paddingBottom: 6,
  },
  ruling: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: 2,
  },
  rulingDate: {
    color: '#888888',
    fontSize: 11,
    fontStyle: 'italic' as const,
  },
  rulingText: {
    color: '#dddddd',
    fontSize: 12,
    lineHeight: 1.4,
  },
  rulingsHint: {
    color: '#666666',
    fontSize: 11,
    fontStyle: 'italic' as const,
    textAlign: 'center' as const,
    padding: '4px 8px',
  },
}

// Constants

const COLOR_FILTER_OPTIONS = [
  { key: 'W', label: 'White' },
  { key: 'U', label: 'Blue' },
  { key: 'B', label: 'Black' },
  { key: 'R', label: 'Red' },
  { key: 'G', label: 'Green' },
  { key: 'C', label: 'Colorless' },
]

const TYPE_FILTER_OPTIONS = [
  { key: 'creature', label: 'Creature' },
  { key: 'instant', label: 'Instant' },
  { key: 'sorcery', label: 'Sorcery' },
  { key: 'enchantment', label: 'Enchantment' },
  { key: 'artifact', label: 'Artifact' },
]

const MANA_COLORS: Record<string, string> = {
  W: '#f9faf4',
  U: '#0e68ab',
  B: '#6a6a6a',
  R: '#d32f2f',
  G: '#388e3c',
}

// Helper functions

function getCardColors(card: SealedCardInfo): Set<string> {
  const cost = card.manaCost || ''
  const colors = new Set<string>()
  if (cost.includes('W')) colors.add('W')
  if (cost.includes('U')) colors.add('U')
  if (cost.includes('B')) colors.add('B')
  if (cost.includes('R')) colors.add('R')
  if (cost.includes('G')) colors.add('G')
  return colors
}

/**
 * Detect which colors of mana a card can produce, based on available card info.
 * Checks land subtypes in typeLine and "Add {X}" patterns in oracleText.
 */
function detectManaProduction(card: SealedCardInfo): string[] {
  const colors: string[] = []
  const typeLine = card.typeLine.toLowerCase()
  const text = (card.oracleText || '').toLowerCase()

  // Check basic land subtypes in typeLine (e.g., "Land — Plains Forest")
  if (typeLine.includes('plains')) colors.push('W')
  if (typeLine.includes('island')) colors.push('U')
  if (typeLine.includes('swamp')) colors.push('B')
  if (typeLine.includes('mountain')) colors.push('R')
  if (typeLine.includes('forest')) colors.push('G')

  // Check oracle text for mana production ("Add {G}", "add {R}{R}", etc.)
  if (text.includes('add')) {
    if (text.includes('{w}')) colors.push('W')
    if (text.includes('{u}')) colors.push('U')
    if (text.includes('{b}')) colors.push('B')
    if (text.includes('{r}')) colors.push('R')
    if (text.includes('{g}')) colors.push('G')
    if (text.includes('any color')) colors.push('W', 'U', 'B', 'R', 'G')
  }

  return [...new Set(colors)]
}

function suggestLands(
  state: DeckBuildingState,
  spellCount: number,
  setLandCount: (name: string, count: number) => void,
) {
  const MIN_DECK_SIZE = 40
  const COLORS = ['W', 'U', 'B', 'R', 'G'] as const
  const colorToLand: Record<string, string> = { W: 'Plains', U: 'Island', B: 'Swamp', R: 'Mountain', G: 'Forest' }
  const basicLandNames = new Set(state.basicLands.map((l) => l.name))

  // Step 1: Categorize deck cards — identify non-basic lands and mana sources
  let nonBasicLandCount = 0
  const existingSources: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }

  for (const cardName of state.deck) {
    const info = state.cardPool.find((c) => c.name === cardName)
    if (!info) continue

    const isLand = info.typeLine.toLowerCase().includes('land') && !basicLandNames.has(info.name)
    const producedColors = detectManaProduction(info)

    if (isLand) {
      nonBasicLandCount++
      // Non-basic land: full credit as a mana source
      for (const c of producedColors) existingSources[c] = (existingSources[c] ?? 0) + 1.0
    } else if (producedColors.length > 0) {
      // Mana dork / mana-producing non-land: half credit
      for (const c of producedColors) existingSources[c] = (existingSources[c] ?? 0) + 0.5
    }
  }

  // Step 2: Calculate target basic lands, accounting for non-basic lands and min deck size
  const actualSpellCount = spellCount - nonBasicLandCount
  // Standard ratio: 17 lands per 23 spells, minus non-basic lands already in deck
  const ratioBasedLands = Math.round(actualSpellCount * 17 / 23) - nonBasicLandCount
  // Ensure deck reaches minimum size
  const minBasedLands = MIN_DECK_SIZE - spellCount
  const targetBasicLands = Math.max(ratioBasedLands, minBasedLands, 0)
  if (targetBasicLands === 0) {
    for (const land of state.basicLands) setLandCount(land.name, 0)
    return
  }

  // Step 3: Count weighted color demand from all cards in deck
  // Multi-pip costs weigh more heavily (per Frank Karsten's analysis):
  //   1 pip = 1.0, 2 pips = 2.5, 3 pips = 4.5, etc.
  //   Formula: weight = pips + 0.5 * (pips - 1) for pips >= 1
  const demand: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }
  for (const cardName of state.deck) {
    const info = state.cardPool.find((c) => c.name === cardName)
    if (!info) continue
    const cost = info.manaCost || ''
    const pipsPerColor: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0 }
    const matches = cost.match(/\{([^}]+)\}/g) || []
    for (const match of matches) {
      const inner = match.slice(1, -1)
      if (inner in pipsPerColor) pipsPerColor[inner] = (pipsPerColor[inner] ?? 0) + 1
    }
    for (const c of COLORS) {
      const pips = pipsPerColor[c] ?? 0
      if (pips > 0) {
        demand[c] = (demand[c] ?? 0) + pips + 0.5 * (pips - 1)
      }
    }
  }

  const totalDemand = Object.values(demand).reduce((a, b) => a + b, 0)

  if (totalDemand === 0) {
    // Colorless deck — give all to first available land type
    const availableLands = state.basicLands.map((l) => l.name)
    for (const land of availableLands) setLandCount(land, 0)
    if (availableLands.length > 0) setLandCount(availableLands[0]!, targetBasicLands)
    return
  }

  // Step 4: Adjust demand based on existing mana sources (non-basic lands, dorks)
  // Convert sources to demand-units so they're comparable, then subtract
  const targetTotalLands = targetBasicLands + nonBasicLandCount
  const sourceScale = targetTotalLands > 0 ? totalDemand / targetTotalLands : 0
  const adjustedDemand: Record<string, number> = {}
  for (const c of COLORS) {
    adjustedDemand[c] = Math.max(0, (demand[c] ?? 0) - (existingSources[c] ?? 0) * sourceScale)
  }
  const totalAdjustedDemand = Object.values(adjustedDemand).reduce((a, b) => a + b, 0)

  // If existing sources cover everything, fall back to raw demand distribution
  const distributionDemand = totalAdjustedDemand > 0 ? adjustedDemand : demand
  const distributionTotal = totalAdjustedDemand > 0 ? totalAdjustedDemand : totalDemand

  // Step 5: Distribute basic lands proportionally to demand
  const landCounts: Record<string, number> = {}
  let assigned = 0
  const entries = COLORS.filter((c) => (distributionDemand[c] ?? 0) > 0).sort(
    (a, b) => (distributionDemand[b] ?? 0) - (distributionDemand[a] ?? 0),
  )

  for (const color of entries) {
    const landName = colorToLand[color]
    if (!landName) continue
    const share = Math.round(((distributionDemand[color] ?? 0) / distributionTotal) * targetBasicLands)
    landCounts[landName] = share
    assigned += share
  }

  // Fix rounding errors — adjust the largest share
  if (assigned !== targetBasicLands && entries.length > 0) {
    const topLand = entries[0] ? colorToLand[entries[0]] : undefined
    if (topLand && landCounts[topLand] != null) {
      landCounts[topLand] = (landCounts[topLand] ?? 0) + targetBasicLands - assigned
    }
  }

  // Apply — reset all to 0 first, then set computed values
  for (const land of state.basicLands) {
    setLandCount(land.name, landCounts[land.name] ?? 0)
  }
}

function matchesTypeFilter(card: SealedCardInfo, filter: string): boolean {
  const typeLine = card.typeLine.toLowerCase()
  return typeLine.includes(filter)
}

function getColorOrder(card: SealedCardInfo): number {
  const cost = card.manaCost || ''
  if (cost.includes('W')) return 1
  if (cost.includes('U')) return 2
  if (cost.includes('B')) return 3
  if (cost.includes('R')) return 4
  if (cost.includes('G')) return 5
  if (cost === '' || cost.match(/^\{[0-9X]+\}$/)) return 6
  return 7
}

function getCmc(card: SealedCardInfo): number {
  const cost = card.manaCost || ''
  let cmc = 0
  const matches = cost.match(/\{([^}]+)\}/g) || []
  for (const match of matches) {
    const inner = match.slice(1, -1)
    const num = parseInt(inner, 10)
    if (!isNaN(num)) {
      cmc += num
    } else if (inner !== 'X') {
      cmc += 1
    }
  }
  return cmc
}

function getRarityOrder(card: SealedCardInfo): number {
  switch (card.rarity.toUpperCase()) {
    case 'MYTHIC':
      return 1
    case 'RARE':
      return 2
    case 'UNCOMMON':
      return 3
    case 'COMMON':
      return 4
    default:
      return 5
  }
}
