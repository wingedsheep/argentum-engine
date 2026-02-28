import { useState, useMemo, useCallback, useRef, useEffect } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { GridDraftState } from '../../store/slices/types'
import type { SealedCardInfo, LobbySettings } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'
import { ManaCost } from '../ui/ManaSymbols'

/**
 * Grid Draft overlay for 2-3 player Grid Draft mode.
 * Shows a 3x3 card grid, row/column selection, and picked cards sidebar.
 */
export function GridDraftOverlay() {
  const lobbyState = useGameStore((state) => state.lobbyState)
  const gridState = lobbyState?.gridDraftState

  if (!gridState || lobbyState?.state !== 'DRAFTING') return null

  return <GridDrafter gridState={gridState} settings={lobbyState.settings} />
}

type SelectionType = 'ROW_0' | 'ROW_1' | 'ROW_2' | 'COL_0' | 'COL_1' | 'COL_2'

/** Get grid indices for a row/column selection */
function getSelectionIndices(selection: SelectionType): number[] {
  switch (selection) {
    case 'ROW_0': return [0, 1, 2]
    case 'ROW_1': return [3, 4, 5]
    case 'ROW_2': return [6, 7, 8]
    case 'COL_0': return [0, 3, 6]
    case 'COL_1': return [1, 4, 7]
    case 'COL_2': return [2, 5, 8]
  }
}

/** Get display label for a selection */
function getSelectionLabel(selection: SelectionType): string {
  switch (selection) {
    case 'ROW_0': return 'Row 1'
    case 'ROW_1': return 'Row 2'
    case 'ROW_2': return 'Row 3'
    case 'COL_0': return 'Col 1'
    case 'COL_1': return 'Col 2'
    case 'COL_2': return 'Col 3'
  }
}

function GridDrafter({ gridState, settings }: { gridState: GridDraftState; settings: LobbySettings }) {
  const responsive = useResponsive()
  const gridDraftPick = useGameStore((s) => s.gridDraftPick)
  const leaveLobby = useGameStore((s) => s.leaveLobby)
  const stopLobby = useGameStore((s) => s.stopLobby)
  const lobbyState = useGameStore((s) => s.lobbyState)
  const isHost = lobbyState?.isHost ?? false

  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [hoveredSelection, setHoveredSelection] = useState<SelectionType | null>(null)
  const [showPickedCards, setShowPickedCards] = useState(!responsive.isMobile)
  const [viewingOpponent, setViewingOpponent] = useState<string | null>(null)

  const handleHover = useCallback((card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard(card)
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }, [])

  // Animation: detect when cards are picked and animate them out
  const prevGridRef = useRef<readonly (SealedCardInfo | null)[]>(gridState.grid)
  const prevGridNumberRef = useRef(gridState.gridNumber)
  const gridAppearedAtRef = useRef(0)
  const [animatingCards, setAnimatingCards] = useState<{
    indices: number[]
    cards: Map<number, SealedCardInfo>
    selection: SelectionType | null
    phase: 'highlight' | 'fadeout'
  } | null>(null)

  useEffect(() => {
    const prevGrid = prevGridRef.current
    const newGrid = gridState.grid
    const isNewGrid = gridState.gridNumber !== prevGridNumberRef.current
    prevGridRef.current = newGrid
    prevGridNumberRef.current = gridState.gridNumber

    // Record when a new grid appears so we can delay the first pick animation
    if (isNewGrid) {
      gridAppearedAtRef.current = Date.now()
    }

    // Find indices where cards disappeared (were non-null, now null)
    const removedIndices: number[] = []
    const removedCards = new Map<number, SealedCardInfo>()
    for (let i = 0; i < 9; i++) {
      const prev = prevGrid[i]
      if (prev && !newGrid[i]) {
        removedIndices.push(i)
        removedCards.set(i, prev)
      }
    }

    if (removedIndices.length === 0) return

    // Determine which selection was made based on the removed indices
    const indexSet = new Set(removedIndices)
    let detectedSelection: SelectionType | null = null
    const selections: SelectionType[] = ['ROW_0', 'ROW_1', 'ROW_2', 'COL_0', 'COL_1', 'COL_2']
    for (const sel of selections) {
      const selIndices = getSelectionIndices(sel)
      // Check if the removed indices contain all non-null cards from this selection
      const selCards = selIndices.filter((i) => prevGrid[i] != null)
      if (selCards.length > 0 && selCards.every((i) => indexSet.has(i))) {
        detectedSelection = sel
        break
      }
    }

    // Delay animation after a new grid so players can see the full grid first
    const timeSinceNewGrid = Date.now() - gridAppearedAtRef.current
    const NEW_GRID_PAUSE = 1200
    const animDelay = timeSinceNewGrid < NEW_GRID_PAUSE
      ? NEW_GRID_PAUSE - timeSinceNewGrid
      : 0

    const startAnimation = () => {
      // Start highlight phase
      setAnimatingCards({
        indices: removedIndices,
        cards: removedCards,
        selection: detectedSelection,
        phase: 'highlight',
      })
    }

    const delayTimer = animDelay > 0 ? setTimeout(startAnimation, animDelay) : null
    if (animDelay === 0) startAnimation()

    // Transition to fadeout
    const fadeTimer = setTimeout(() => {
      setAnimatingCards((prev) =>
        prev ? { ...prev, phase: 'fadeout' } : null,
      )
    }, animDelay + 600)

    // Clear animation
    const clearTimer = setTimeout(() => {
      setAnimatingCards(null)
    }, animDelay + 1400)

    return () => {
      if (delayTimer) clearTimeout(delayTimer)
      clearTimeout(fadeTimer)
      clearTimeout(clearTimer)
    }
  }, [gridState.grid, gridState.gridNumber])

  const timerWarning = gridState.timeRemaining <= 10
  const isMobile = responsive.isMobile

  const availableSet = useMemo(
    () => new Set(gridState.availableSelections),
    [gridState.availableSelections],
  )

  // Indices highlighted by the hovered row/column
  const highlightedIndices = useMemo(() => {
    if (!hoveredSelection) return new Set<number>()
    return new Set(getSelectionIndices(hoveredSelection))
  }, [hoveredSelection])

  // Group picked cards by color for sidebar
  const pickedByColor = useMemo(() => {
    const groups: Record<string, Array<{ card: SealedCardInfo; count: number }>> = {
      W: [], U: [], B: [], R: [], G: [], M: [], C: [],
    }
    const colorCounts: Record<string, Map<string, { card: SealedCardInfo; count: number }>> = {
      W: new Map(), U: new Map(), B: new Map(), R: new Map(), G: new Map(), M: new Map(), C: new Map(),
    }

    for (const card of gridState.pickedCards) {
      const color = getCardColor(card)
      const colorMap = colorCounts[color]
      if (!colorMap) continue
      const existing = colorMap.get(card.name)
      if (existing) {
        existing.count++
      } else {
        colorMap.set(card.name, { card, count: 1 })
      }
    }

    for (const [color, map] of Object.entries(colorCounts)) {
      groups[color] = Array.from(map.values())
    }

    return groups
  }, [gridState.pickedCards])

  const creatureCount = gridState.pickedCards.filter((c: SealedCardInfo) => c.typeLine.includes('Creature')).length
  const spellCount = gridState.pickedCards.length - creatureCount

  const handlePick = useCallback((selection: SelectionType) => {
    if (!gridState.isYourTurn || !availableSet.has(selection)) return
    gridDraftPick(selection)
    setHoveredSelection(null)
  }, [gridState.isYourTurn, availableSet, gridDraftPick])

  const cardSize = isMobile ? 100 : 140

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'linear-gradient(135deg, #1a1a2e 0%, #162e1e 50%, #0f4630 100%)',
      display: 'flex', flexDirection: 'column',
      overflow: 'hidden', zIndex: 100,
      fontFamily: "'Segoe UI', system-ui, -apple-system, sans-serif",
    }}>
      {/* Header */}
      <div style={{
        padding: isMobile ? '8px 12px' : '10px 20px',
        borderBottom: '1px solid rgba(255,255,255,0.1)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        background: 'rgba(0,0,0,0.3)',
        flexShrink: 0,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div style={{
            background: 'linear-gradient(135deg, #22c55e, #16a34a)',
            padding: '4px 12px', borderRadius: 4,
            fontWeight: 700, fontSize: 13, letterSpacing: '0.05em',
            textTransform: 'uppercase', color: '#fff',
          }}>
            Grid Draft
          </div>
          <span style={{ color: 'rgba(255,255,255,0.5)', fontSize: 13 }}>
            {settings.setNames.join(' + ')}
          </span>
          <span style={{ color: 'rgba(255,255,255,0.4)', fontSize: 12 }}>
            Grid #{gridState.gridNumber}
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* Timer */}
          <div style={{
            background: timerWarning ? 'rgba(233,69,96,0.3)' : 'rgba(255,255,255,0.1)',
            padding: '4px 12px', borderRadius: 4,
            fontWeight: 600, fontSize: 14,
            color: timerWarning ? '#e94560' : 'rgba(255,255,255,0.7)',
            fontVariantNumeric: 'tabular-nums',
            animation: timerWarning ? 'pulse 1s infinite' : undefined,
          }}>
            {gridState.timeRemaining}s
          </div>

          {/* Deck remaining */}
          <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: 13 }}>
            Deck: {gridState.mainDeckRemaining}
          </div>

          {/* Picked count */}
          <div style={{ color: 'rgba(255,255,255,0.5)', fontSize: 13 }}>
            Picked: {gridState.pickedCards.length}
          </div>

          {isMobile && (
            <button
              onClick={() => setShowPickedCards((v) => !v)}
              style={{
                background: 'rgba(255,255,255,0.1)', border: 'none', borderRadius: 4,
                padding: '4px 10px', color: '#fff', cursor: 'pointer', fontSize: 12,
              }}
            >
              {showPickedCards ? 'Grid' : `Pool (${gridState.pickedCards.length})`}
            </button>
          )}

          {isHost ? (
            <button
              onClick={stopLobby}
              onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#e74c3c' }}
              onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = '#c0392b' }}
              style={{
                backgroundColor: '#c0392b', color: 'white',
                padding: '4px 14px', fontSize: 13,
                border: 'none', borderRadius: 6, cursor: 'pointer',
                transition: 'background-color 0.15s',
              }}
            >
              Stop Draft
            </button>
          ) : (
            <button
              onClick={leaveLobby}
              onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#e74c3c' }}
              onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = '#c0392b' }}
              style={{
                backgroundColor: '#c0392b', color: 'white',
                padding: '4px 14px', fontSize: 13,
                border: 'none', borderRadius: 6, cursor: 'pointer',
                transition: 'background-color 0.15s',
              }}
            >
              Leave
            </button>
          )}
        </div>
      </div>

      {/* Main content */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Left: Grid area */}
        {(!isMobile || !showPickedCards) && (
          <div style={{
            flex: 1, display: 'flex', flexDirection: 'column',
            alignItems: 'center',
            padding: isMobile ? 12 : 20, overflow: 'auto',
          }}>
            {/* Spacer to vertically center content when it doesn't overflow */}
            <div style={{ flex: '1 0 0' }} />
            {/* Turn indicator */}
            <div style={{
              textAlign: 'center', marginBottom: 12,
              color: gridState.isYourTurn ? '#4ade80' : 'rgba(255,255,255,0.5)',
              fontSize: 16, fontWeight: 600,
            }}>
              {gridState.isYourTurn
                ? 'Your turn — pick a row or column'
                : `Waiting for ${gridState.activePlayerName}...`}
            </div>

            {/* Last action + last picked cards */}
            {gridState.lastAction && (
              <div style={{
                textAlign: 'center', marginBottom: 12,
                color: 'rgba(255,255,255,0.4)', fontSize: 13,
                fontStyle: 'italic',
              }}>
                {gridState.lastAction}
                {gridState.lastPickedCards.length > 0 && (
                  <span style={{ fontStyle: 'normal', marginLeft: 6 }}>
                    ({gridState.lastPickedCards.map((c) => c.name).join(', ')})
                  </span>
                )}
              </div>
            )}

            {/* Player order */}
            <div style={{
              display: 'flex', gap: 12, marginBottom: 16,
              justifyContent: 'center',
            }}>
              {gridState.playerOrder.map((name: string, idx: number) => {
                const isYou = !(name in gridState.totalPickedByOthers)
                const isOpponent = !isYou
                const opponentCount = gridState.totalPickedByOthers[name]
                const isCurrent = idx === gridState.currentPickerIndex

                return (
                  <div
                    key={name}
                    onClick={isOpponent ? () => setViewingOpponent(name) : undefined}
                    onMouseEnter={isOpponent ? (e) => {
                      e.currentTarget.style.background = isCurrent
                        ? 'rgba(74,222,128,0.3)'
                        : 'rgba(255,255,255,0.12)'
                    } : undefined}
                    onMouseLeave={isOpponent ? (e) => {
                      e.currentTarget.style.background = isCurrent
                        ? 'rgba(74,222,128,0.2)'
                        : 'rgba(255,255,255,0.05)'
                    } : undefined}
                    style={{
                      padding: '3px 10px', borderRadius: 4,
                      fontSize: 12, fontWeight: 600,
                      background: isCurrent
                        ? 'rgba(74,222,128,0.2)'
                        : 'rgba(255,255,255,0.05)',
                      color: isCurrent
                        ? '#4ade80'
                        : 'rgba(255,255,255,0.4)',
                      border: isCurrent
                        ? '1px solid rgba(74,222,128,0.4)'
                        : '1px solid rgba(255,255,255,0.1)',
                      cursor: isOpponent ? 'pointer' : 'default',
                      transition: 'background 0.15s',
                    }}
                  >
                    {name}{isYou ? ' (You)' : ''}
                    {isOpponent && opponentCount != null && (
                      <span style={{ marginLeft: 6, opacity: 0.7 }}>
                        — {opponentCount} cards
                      </span>
                    )}
                  </div>
                )
              })}
            </div>

            {/* 3x3 Grid with row/column headers */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
              {/* Column headers */}
              <div style={{ display: 'flex', gap: 0 }}>
                {/* Empty corner cell */}
                <div style={{ width: 40 }} />
                {(['COL_0', 'COL_1', 'COL_2'] as const).map((col) => {
                  const isAvailable = gridState.isYourTurn && availableSet.has(col)
                  const isHovered = hoveredSelection === col
                  const isAnimPick = animatingCards?.selection === col && animatingCards.phase === 'highlight'
                  return (
                    <button
                      key={col}
                      onClick={() => handlePick(col)}
                      onMouseEnter={() => isAvailable && setHoveredSelection(col)}
                      onMouseLeave={() => setHoveredSelection(null)}
                      disabled={!isAvailable}
                      style={{
                        width: cardSize + 8,
                        height: 28,
                        border: 'none',
                        background: isAnimPick
                          ? 'rgba(74,222,128,0.4)'
                          : isHovered
                            ? 'rgba(74,222,128,0.3)'
                            : isAvailable
                              ? 'rgba(255,255,255,0.08)'
                              : 'transparent',
                        color: isAnimPick ? '#fff' : isAvailable ? '#4ade80' : 'rgba(255,255,255,0.2)',
                        fontSize: 11, fontWeight: 600,
                        cursor: isAvailable ? 'pointer' : 'default',
                        borderRadius: '4px 4px 0 0',
                        textTransform: 'uppercase',
                        letterSpacing: '0.05em',
                        transition: 'all 0.3s',
                        boxShadow: isAnimPick ? '0 0 12px rgba(74,222,128,0.4)' : 'none',
                      }}
                    >
                      {getSelectionLabel(col)}
                    </button>
                  )
                })}
              </div>

              {/* Grid rows */}
              {([0, 1, 2] as const).map((rowIdx) => {
                const rowKey = `ROW_${rowIdx}` as SelectionType
                const isAvailable = gridState.isYourTurn && availableSet.has(rowKey)
                const isRowHovered = hoveredSelection === rowKey
                const isAnimRowPick = animatingCards?.selection === rowKey && animatingCards.phase === 'highlight'

                return (
                  <div key={rowIdx} style={{ display: 'flex', gap: 0 }}>
                    {/* Row header */}
                    <button
                      onClick={() => handlePick(rowKey)}
                      onMouseEnter={() => isAvailable && setHoveredSelection(rowKey)}
                      onMouseLeave={() => setHoveredSelection(null)}
                      disabled={!isAvailable}
                      style={{
                        width: 40,
                        height: cardSize * 1.4 + 8,
                        border: 'none',
                        background: isAnimRowPick
                          ? 'rgba(74,222,128,0.4)'
                          : isRowHovered
                            ? 'rgba(74,222,128,0.3)'
                            : isAvailable
                              ? 'rgba(255,255,255,0.08)'
                              : 'transparent',
                        color: isAnimRowPick ? '#fff' : isAvailable ? '#4ade80' : 'rgba(255,255,255,0.2)',
                        fontSize: 11, fontWeight: 600,
                        cursor: isAvailable ? 'pointer' : 'default',
                        borderRadius: '4px 0 0 4px',
                        writingMode: 'vertical-lr',
                        textTransform: 'uppercase',
                        letterSpacing: '0.05em',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        transition: 'all 0.3s',
                        boxShadow: isAnimRowPick ? '0 0 12px rgba(74,222,128,0.4)' : 'none',
                      }}
                    >
                      {getSelectionLabel(rowKey)}
                    </button>

                    {/* Row cards */}
                    {[0, 1, 2].map((colIdx) => {
                      const gridIdx = rowIdx * 3 + colIdx
                      const card = gridState.grid[gridIdx] ?? undefined
                      const isHighlighted = highlightedIndices.has(gridIdx)
                      const animCard = animatingCards?.cards.get(gridIdx)
                      const animPhase = animatingCards?.phase ?? null
                      const isAnimating = animatingCards != null && animatingCards.indices.includes(gridIdx)

                      return (
                        <div
                          key={colIdx}
                          style={{
                            width: cardSize + 8,
                            height: cardSize * 1.4 + 8,
                            padding: 4,
                            position: 'relative',
                          }}
                        >
                          {card ? (
                            <GridCard
                              card={card}
                              isHighlighted={isHighlighted}
                              onHover={handleHover}
                              width={cardSize}
                            />
                          ) : animCard ? (
                            /* Show the picked card with animation */
                            <div style={{
                              position: 'relative',
                              width: cardSize,
                              height: cardSize * 1.4,
                            }}>
                              {/* Highlight glow behind card */}
                              <div style={{
                                position: 'absolute', inset: -4,
                                borderRadius: 10,
                                background: animPhase === 'highlight'
                                  ? 'rgba(74,222,128,0.25)'
                                  : 'transparent',
                                boxShadow: animPhase === 'highlight'
                                  ? '0 0 20px rgba(74,222,128,0.4), inset 0 0 20px rgba(74,222,128,0.15)'
                                  : 'none',
                                transition: 'all 0.3s ease-out',
                              }} />
                              {/* The card itself fading/shrinking out */}
                              <div style={{
                                opacity: animPhase === 'highlight' ? 1 : 0,
                                transform: animPhase === 'highlight'
                                  ? 'scale(1.05)'
                                  : 'scale(0.8) translateY(-20px)',
                                transition: animPhase === 'highlight'
                                  ? 'transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s'
                                  : 'transform 0.7s ease-in, opacity 0.6s ease-in',
                              }}>
                                <GridCard
                                  card={animCard}
                                  isHighlighted={true}
                                  onHover={() => {}}
                                  width={cardSize}
                                />
                              </div>
                            </div>
                          ) : (
                            <div style={{
                              width: cardSize, height: cardSize * 1.4,
                              borderRadius: 6,
                              border: '1px dashed rgba(255,255,255,0.1)',
                              background: isAnimating ? 'rgba(74,222,128,0.05)' : 'rgba(0,0,0,0.15)',
                              transition: 'background 0.5s',
                            }} />
                          )}
                        </div>
                      )
                    })}
                  </div>
                )
              })}
            </div>

            {/* Spacer to vertically center content when it doesn't overflow */}
            <div style={{ flex: '1 0 0' }} />
          </div>
        )}

        {/* Right: Picked cards sidebar */}
        {(!isMobile || showPickedCards) && (
          <div style={{
            width: isMobile ? '100%' : 280,
            borderLeft: isMobile ? 'none' : '1px solid rgba(255,255,255,0.1)',
            background: 'rgba(0,0,0,0.2)',
            display: 'flex', flexDirection: 'column',
            overflow: 'hidden',
          }}>
            {/* Sidebar header */}
            <div style={{
              padding: '10px 14px',
              borderBottom: '1px solid rgba(255,255,255,0.1)',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            }}>
              <span style={{ color: 'rgba(255,255,255,0.7)', fontWeight: 600, fontSize: 13 }}>
                Picked Cards ({gridState.pickedCards.length})
              </span>
              <span style={{ color: 'rgba(255,255,255,0.4)', fontSize: 12 }}>
                {creatureCount}C / {spellCount}S
              </span>
            </div>

            {/* Sidebar content */}
            <div style={{ flex: 1, overflow: 'auto', padding: '8px 10px' }}>
              {Object.entries(pickedByColor).map(([color, cards]) => {
                if (cards.length === 0) return null
                return (
                  <div key={color} style={{ marginBottom: 8 }}>
                    <div style={{
                      fontSize: 11, fontWeight: 600, textTransform: 'uppercase',
                      color: getColorName(color).color,
                      letterSpacing: '0.05em', marginBottom: 4,
                      display: 'flex', alignItems: 'center', gap: 4,
                    }}>
                      <span style={{
                        width: 8, height: 8, borderRadius: '50%',
                        background: getColorName(color).color,
                        display: 'inline-block',
                      }} />
                      {getColorName(color).name} ({cards.reduce((sum, c) => sum + c.count, 0)})
                    </div>
                    {cards.map(({ card, count }) => (
                      <div
                        key={card.name}
                        onMouseEnter={(e) => handleHover(card, e)}
                        onMouseMove={(e) => handleHover(card, e)}
                        onMouseLeave={() => handleHover(null)}
                        style={{
                          padding: '3px 8px',
                          fontSize: 12,
                          color: 'rgba(255,255,255,0.7)',
                          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                          borderRadius: 3,
                          cursor: 'pointer',
                        }}
                      >
                        <span style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
                          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {count > 1 ? `${count}x ` : ''}{card.name}
                          </span>
                        </span>
                        {card.manaCost && (
                          <span style={{ flexShrink: 0, marginLeft: 4 }}>
                            <ManaCost cost={card.manaCost} size={12} />
                          </span>
                        )}
                      </div>
                    ))}
                  </div>
                )
              })}
              {gridState.pickedCards.length === 0 && (
                <div style={{
                  textAlign: 'center', color: 'rgba(255,255,255,0.3)',
                  padding: 20, fontSize: 13,
                }}>
                  No cards picked yet
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Opponent cards overlay */}
      {viewingOpponent && (
        <OpponentCardsOverlay
          playerName={viewingOpponent}
          cards={gridState.pickedCardsByOthers[viewingOpponent] ?? []}
          onClose={() => setViewingOpponent(null)}
          onHover={handleHover}
        />
      )}

      {/* Card preview on hover */}
      {hoveredCard && hoverPos && (
        <CardPreview card={hoveredCard} position={hoverPos} />
      )}

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }
        @keyframes gridPickGlow {
          0% { box-shadow: 0 0 8px rgba(74,222,128,0.2); }
          50% { box-shadow: 0 0 24px rgba(74,222,128,0.5); }
          100% { box-shadow: 0 0 8px rgba(74,222,128,0.2); }
        }
      `}</style>
    </div>
  )
}

/**
 * Overlay showing an opponent's picked cards.
 */
function OpponentCardsOverlay({ playerName, cards, onClose, onHover }: {
  playerName: string
  cards: readonly SealedCardInfo[]
  onClose: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
}) {
  const colorGroups = useMemo(() => {
    const groups: Record<string, Array<{ card: SealedCardInfo; count: number }>> = {
      W: [], U: [], B: [], R: [], G: [], M: [], C: [],
    }
    const colorCounts: Record<string, Map<string, { card: SealedCardInfo; count: number }>> = {
      W: new Map(), U: new Map(), B: new Map(), R: new Map(), G: new Map(), M: new Map(), C: new Map(),
    }

    for (const card of cards) {
      const color = getCardColor(card)
      const colorMap = colorCounts[color]
      if (!colorMap) continue
      const existing = colorMap.get(card.name)
      if (existing) {
        existing.count++
      } else {
        colorMap.set(card.name, { card, count: 1 })
      }
    }

    for (const [color, map] of Object.entries(colorCounts)) {
      groups[color] = Array.from(map.values())
    }

    return groups
  }, [cards])

  const creatureCount = cards.filter((c) => c.typeLine.includes('Creature')).length
  const spellCount = cards.length - creatureCount

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
        background: 'rgba(0,0,0,0.7)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        zIndex: 150,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: 'linear-gradient(135deg, #1a1a2e 0%, #162e1e 50%, #0f4630 100%)',
          borderRadius: 10,
          border: '1px solid rgba(255,255,255,0.15)',
          boxShadow: '0 16px 48px rgba(0,0,0,0.5)',
          width: 360,
          maxHeight: '80vh',
          display: 'flex', flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        {/* Header */}
        <div style={{
          padding: '14px 18px',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          background: 'rgba(0,0,0,0.2)',
        }}>
          <div>
            <div style={{ color: '#fff', fontWeight: 700, fontSize: 15 }}>
              {playerName}'s Cards
            </div>
            <div style={{ color: 'rgba(255,255,255,0.4)', fontSize: 12, marginTop: 2 }}>
              {cards.length} cards — {creatureCount}C / {spellCount}S
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'rgba(255,255,255,0.1)', border: 'none', borderRadius: 4,
              width: 28, height: 28, display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: 'rgba(255,255,255,0.6)', cursor: 'pointer', fontSize: 16,
            }}
          >
            ×
          </button>
        </div>

        {/* Banner */}
        <div style={{
          padding: '8px 18px',
          background: 'rgba(74,222,128,0.1)',
          borderBottom: '1px solid rgba(74,222,128,0.15)',
          color: 'rgba(74,222,128,0.8)',
          fontSize: 12,
          fontWeight: 600,
          textAlign: 'center',
        }}>
          Viewing opponent's picked cards
        </div>

        {/* Card list */}
        <div style={{ flex: 1, overflow: 'auto', padding: '10px 14px' }}>
          {Object.entries(colorGroups).map(([color, colorCards]) => {
            if (colorCards.length === 0) return null
            return (
              <div key={color} style={{ marginBottom: 10 }}>
                <div style={{
                  fontSize: 11, fontWeight: 600, textTransform: 'uppercase',
                  color: getColorName(color).color,
                  letterSpacing: '0.05em', marginBottom: 4,
                  display: 'flex', alignItems: 'center', gap: 4,
                }}>
                  <span style={{
                    width: 8, height: 8, borderRadius: '50%',
                    background: getColorName(color).color,
                    display: 'inline-block',
                  }} />
                  {getColorName(color).name} ({colorCards.reduce((sum, c) => sum + c.count, 0)})
                </div>
                {colorCards.map(({ card, count }) => (
                  <div
                    key={card.name}
                    onMouseEnter={(e) => onHover(card, e)}
                    onMouseMove={(e) => onHover(card, e)}
                    onMouseLeave={() => onHover(null)}
                    style={{
                      padding: '3px 8px',
                      fontSize: 12,
                      color: 'rgba(255,255,255,0.7)',
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      borderRadius: 3,
                      cursor: 'pointer',
                    }}
                  >
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {count > 1 ? `${count}x ` : ''}{card.name}
                      </span>
                    </span>
                    {card.manaCost && (
                      <span style={{ flexShrink: 0, marginLeft: 4 }}>
                        <ManaCost cost={card.manaCost} size={12} />
                      </span>
                    )}
                  </div>
                ))}
              </div>
            )
          })}
          {cards.length === 0 && (
            <div style={{
              textAlign: 'center', color: 'rgba(255,255,255,0.3)',
              padding: 20, fontSize: 13,
            }}>
              No cards picked yet
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

/**
 * Single card in the grid.
 */
function GridCard({ card, isHighlighted, onHover, width }: {
  card: SealedCardInfo
  isHighlighted: boolean
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  width: number
}) {
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'normal')

  return (
    <div
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        width,
        height: width * 1.4,
        borderRadius: 6,
        overflow: 'hidden',
        border: isHighlighted
          ? '2px solid #4ade80'
          : '1px solid rgba(255,255,255,0.15)',
        background: 'rgba(0,0,0,0.3)',
        cursor: 'pointer',
        transition: 'all 0.15s',
        boxShadow: isHighlighted ? '0 0 12px rgba(74,222,128,0.3)' : undefined,
        transform: isHighlighted ? 'scale(1.03)' : undefined,
      }}
    >
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={card.name}
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
          loading="lazy"
        />
      ) : (
        <div style={{
          padding: 6,
          height: '100%',
          display: 'flex', flexDirection: 'column',
        }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
            marginBottom: 2,
          }}>
            <span style={{ fontSize: 10, fontWeight: 600, color: '#fff', flex: 1 }}>
              {card.name}
            </span>
            {card.manaCost && (
              <ManaCost cost={card.manaCost} size={10} />
            )}
          </div>
          <div style={{ fontSize: 9, color: 'rgba(255,255,255,0.5)', marginBottom: 2 }}>
            {card.typeLine}
          </div>
          {card.oracleText && (
            <div style={{ fontSize: 9, color: 'rgba(255,255,255,0.6)', flex: 1, overflow: 'hidden' }}>
              {card.oracleText}
            </div>
          )}
          {card.power != null && card.toughness != null && (
            <div style={{
              fontSize: 10, fontWeight: 700, color: '#fff',
              textAlign: 'right', marginTop: 2,
            }}>
              {card.power}/{card.toughness}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

/**
 * Card preview popup on hover.
 */
function CardPreview({ card, position }: { card: SealedCardInfo; position: { x: number; y: number } }) {
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'large')
  const previewWidth = 250
  const previewHeight = 350
  const margin = 20

  // Position to the side of cursor, avoiding going off-screen
  const left = position.x + previewWidth + margin + 20 < window.innerWidth
    ? position.x + margin
    : position.x - previewWidth - margin
  const top = Math.max(10, Math.min(position.y - previewHeight / 2, window.innerHeight - previewHeight - 10))

  return (
    <div style={{
      position: 'fixed', left, top,
      width: previewWidth,
      zIndex: 200,
      pointerEvents: 'none',
      borderRadius: 8,
      overflow: 'hidden',
      boxShadow: '0 8px 32px rgba(0,0,0,0.6)',
      border: '1px solid rgba(255,255,255,0.2)',
      transition: 'top 0.05s, left 0.05s',
    }}>
      {imageUrl ? (
        <img src={imageUrl} alt={card.name} style={{ width: '100%', display: 'block' }} />
      ) : (
        <div style={{
          background: '#1a1a2e', padding: 16,
          minHeight: previewHeight,
        }}>
          <div style={{ fontWeight: 700, color: '#fff', fontSize: 14, marginBottom: 4 }}>
            {card.name}
          </div>
          {card.manaCost && (
            <div style={{ marginBottom: 8 }}>
              <ManaCost cost={card.manaCost} size={14} />
            </div>
          )}
          <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: 12, marginBottom: 8 }}>
            {card.typeLine}
          </div>
          {card.oracleText && (
            <div style={{ color: 'rgba(255,255,255,0.8)', fontSize: 12, lineHeight: 1.4 }}>
              {card.oracleText}
            </div>
          )}
          {card.power != null && card.toughness != null && (
            <div style={{ fontWeight: 700, color: '#fff', fontSize: 14, textAlign: 'right', marginTop: 8 }}>
              {card.power}/{card.toughness}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

/** Get the primary color category for a card. */
function getCardColor(card: SealedCardInfo): string {
  if (!card.manaCost) return 'C'
  const colors: string[] = []
  if (card.manaCost.includes('{W}')) colors.push('W')
  if (card.manaCost.includes('{U}')) colors.push('U')
  if (card.manaCost.includes('{B}')) colors.push('B')
  if (card.manaCost.includes('{R}')) colors.push('R')
  if (card.manaCost.includes('{G}')) colors.push('G')
  if (colors.length === 0) return 'C'
  if (colors.length > 1) return 'M'
  return colors[0] ?? 'C'
}

/** Get display info for a color category. */
function getColorName(color: string): { name: string; color: string } {
  switch (color) {
    case 'W': return { name: 'White', color: '#f5e6c8' }
    case 'U': return { name: 'Blue', color: '#6eb5ff' }
    case 'B': return { name: 'Black', color: '#b3a3c4' }
    case 'R': return { name: 'Red', color: '#ff6b6b' }
    case 'G': return { name: 'Green', color: '#6bcb77' }
    case 'M': return { name: 'Multicolor', color: '#ffd93d' }
    case 'C': return { name: 'Colorless', color: '#aaa' }
    default: return { name: 'Other', color: '#888' }
  }
}
