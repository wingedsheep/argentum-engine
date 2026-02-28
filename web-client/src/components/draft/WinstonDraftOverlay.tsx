import { useState, useMemo, useCallback, useRef, useEffect } from 'react'
import { useGameStore, type WinstonDraftState } from '../../store/gameStore'
import type { SealedCardInfo, LobbySettings } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'
import { ManaCost } from '../ui/ManaSymbols'

/**
 * Winston Draft overlay for 2-player Winston Draft mode.
 * Shows 3 piles, current pile contents when examining, take/skip buttons,
 * and a picked cards sidebar.
 */
export function WinstonDraftOverlay() {
  const lobbyState = useGameStore((state) => state.lobbyState)
  const winstonState = lobbyState?.winstonDraftState

  if (!winstonState || lobbyState?.state !== 'DRAFTING') return null

  return <WinstonDrafter winstonState={winstonState} settings={lobbyState.settings} />
}

function WinstonDrafter({ winstonState, settings }: { winstonState: WinstonDraftState; settings: LobbySettings }) {
  const responsive = useResponsive()
  const winstonTakePile = useGameStore((s) => s.winstonTakePile)
  const winstonSkipPile = useGameStore((s) => s.winstonSkipPile)
  const leaveLobby = useGameStore((s) => s.leaveLobby)
  const stopLobby = useGameStore((s) => s.stopLobby)
  const lobbyState = useGameStore((s) => s.lobbyState)
  const isHost = lobbyState?.isHost ?? false

  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [showPickedCards, setShowPickedCards] = useState(!responsive.isMobile)
  const [viewingOpponent, setViewingOpponent] = useState(false)

  // Pick animation state
  const [pickAnimation, setPickAnimation] = useState<{ cards: SealedCardInfo[]; phase: 'enter' | 'exit' } | null>(null)
  const prevPickedCountRef = useRef(winstonState.pickedCards.length)
  const wasYourTurnRef = useRef(winstonState.isYourTurn)

  // Detect newly picked cards (blind pick or pile take)
  useEffect(() => {
    const prevCount = prevPickedCountRef.current
    const currentCount = winstonState.pickedCards.length
    const wasYourTurn = wasYourTurnRef.current

    prevPickedCountRef.current = currentCount
    wasYourTurnRef.current = winstonState.isYourTurn

    // If we just picked cards (was our turn, count increased)
    if (wasYourTurn && currentCount > prevCount) {
      const newCards = winstonState.pickedCards.slice(prevCount)
      if (newCards.length > 0) {
        setPickAnimation({ cards: [...newCards], phase: 'enter' })
        // Transition to exit after a short display period
        const exitTimer = setTimeout(() => setPickAnimation((a) => a ? { ...a, phase: 'exit' } : null), 1500)
        const clearTimer = setTimeout(() => setPickAnimation(null), 2200)
        return () => { clearTimeout(exitTimer); clearTimeout(clearTimer) }
      }
    }
  }, [winstonState.pickedCards.length, winstonState.isYourTurn])

  const handleHover = useCallback((card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard(card)
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }, [])

  const timerWarning = winstonState.timeRemaining <= 10

  // Group picked cards by color for sidebar
  const pickedByColor = useMemo(() => {
    const groups: Record<string, Array<{ card: SealedCardInfo; count: number }>> = {
      W: [], U: [], B: [], R: [], G: [], M: [], C: [],
    }
    const colorCounts: Record<string, Map<string, { card: SealedCardInfo; count: number }>> = {
      W: new Map(), U: new Map(), B: new Map(), R: new Map(), G: new Map(), M: new Map(), C: new Map(),
    }

    for (const card of winstonState.pickedCards) {
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
  }, [winstonState.pickedCards])

  const creatureCount = winstonState.pickedCards.filter((c) => c.typeLine.includes('Creature')).length
  const spellCount = winstonState.pickedCards.length - creatureCount

  const isMobile = responsive.isMobile

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
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
            background: 'linear-gradient(135deg, #e94560, #c23152)',
            padding: '4px 12px', borderRadius: 4,
            fontWeight: 700, fontSize: 13, letterSpacing: '0.05em',
            textTransform: 'uppercase', color: '#fff',
          }}>
            Winston Draft
          </div>
          <span style={{ color: 'rgba(255,255,255,0.5)', fontSize: 13 }}>
            {settings.setNames.join(' + ')}
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
            {winstonState.timeRemaining}s
          </div>

          {/* Deck remaining */}
          <div style={{
            color: 'rgba(255,255,255,0.5)', fontSize: 13,
          }}>
            Deck: {winstonState.mainDeckRemaining}
          </div>

          {/* Picked count */}
          <div style={{
            color: 'rgba(255,255,255,0.5)', fontSize: 13,
          }}>
            Picked: {winstonState.pickedCards.length}
          </div>

          {isMobile && (
            <button
              onClick={() => setShowPickedCards((v) => !v)}
              style={{
                background: 'rgba(255,255,255,0.1)', border: 'none', borderRadius: 4,
                padding: '4px 10px', color: '#fff', cursor: 'pointer', fontSize: 12,
              }}
            >
              {showPickedCards ? 'Piles' : `Pool (${winstonState.pickedCards.length})`}
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
        {/* Left: Piles area */}
        {(!isMobile || !showPickedCards) && (
          <div style={{
            flex: 1, display: 'flex', flexDirection: 'column',
            padding: isMobile ? 12 : 20, overflow: 'auto',
          }}>
            {/* Turn indicator */}
            <div style={{
              textAlign: 'center', marginBottom: 16,
              color: winstonState.isYourTurn ? '#4ade80' : 'rgba(255,255,255,0.5)',
              fontSize: 16, fontWeight: 600,
            }}>
              {winstonState.isYourTurn
                ? 'Your turn — examine and choose'
                : `Waiting for ${winstonState.activePlayerName}...`}
            </div>

            {/* Last action log + last picked cards */}
            {winstonState.lastAction && (
              <div style={{
                textAlign: 'center', marginBottom: 16,
                color: 'rgba(255,255,255,0.4)', fontSize: 13,
                fontStyle: 'italic',
              }}>
                {winstonState.lastAction}
                {winstonState.lastPickedCards.length > 0 && (
                  <span style={{ fontStyle: 'normal', marginLeft: 6 }}>
                    ({winstonState.lastPickedCards.map((c) => c.name).join(', ')})
                  </span>
                )}
              </div>
            )}

            {/* Opponent picked count — click to view known cards */}
            <div style={{
              display: 'flex', gap: 12, marginBottom: 16,
              justifyContent: 'center',
            }}>
              <div
                onClick={() => setViewingOpponent(true)}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.12)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'rgba(255,255,255,0.05)'
                }}
                style={{
                  padding: '3px 10px', borderRadius: 4,
                  fontSize: 12, fontWeight: 600,
                  background: 'rgba(255,255,255,0.05)',
                  color: 'rgba(255,255,255,0.4)',
                  border: '1px solid rgba(255,255,255,0.1)',
                  cursor: 'pointer',
                  transition: 'background 0.15s',
                }}
              >
                {winstonState.isYourTurn
                  ? 'Opponent'
                  : winstonState.activePlayerName}
                <span style={{ marginLeft: 6, opacity: 0.7 }}>
                  — {winstonState.totalPickedByOpponent} cards
                </span>
                {winstonState.knownOpponentCards.length > 0 && (
                  <span style={{ marginLeft: 4, opacity: 0.6 }}>
                    ({winstonState.knownOpponentCards.length} known)
                  </span>
                )}
              </div>
            </div>

            {/* Three piles */}
            <div style={{
              display: 'flex', gap: isMobile ? 12 : 24,
              justifyContent: 'center', alignItems: 'flex-start',
              marginBottom: 24,
            }}>
              {[0, 1, 2].map((pileIndex) => {
                const isCurrentPile = winstonState.isYourTurn && pileIndex === winstonState.currentPileIndex
                const pileSize = winstonState.pileSizes[pileIndex] ?? 0
                const isExaminable = isCurrentPile
                const isPastPile = winstonState.isYourTurn && pileIndex < winstonState.currentPileIndex

                return (
                  <div key={pileIndex} style={{
                    display: 'flex', flexDirection: 'column', alignItems: 'center',
                    opacity: isPastPile ? 0.4 : 1,
                    transition: 'opacity 0.2s',
                  }}>
                    {/* Pile label */}
                    <div style={{
                      fontSize: 12, fontWeight: 600, textTransform: 'uppercase',
                      letterSpacing: '0.08em', marginBottom: 8,
                      color: isExaminable ? '#4ade80' : 'rgba(255,255,255,0.5)',
                    }}>
                      Pile {pileIndex + 1}
                    </div>

                    {/* Pile visualization */}
                    <div style={{
                      width: isMobile ? 100 : 140,
                      height: isMobile ? 140 : 196,
                      borderRadius: 8,
                      border: isExaminable
                        ? '2px solid #4ade80'
                        : '2px solid rgba(255,255,255,0.15)',
                      background: pileSize === 0
                        ? 'rgba(255,255,255,0.03)'
                        : 'linear-gradient(135deg, #2a2a4a 0%, #1a1a3a 100%)',
                      display: 'flex', flexDirection: 'column',
                      alignItems: 'center', justifyContent: 'center',
                      position: 'relative',
                      boxShadow: isExaminable ? '0 0 20px rgba(74,222,128,0.2)' : undefined,
                      transition: 'all 0.2s',
                    }}>
                      {pileSize === 0 ? (
                        <span style={{ color: 'rgba(255,255,255,0.2)', fontSize: 12 }}>
                          Empty
                        </span>
                      ) : (
                        <>
                          {/* Card back stack visualization */}
                          {Array.from({ length: Math.min(pileSize, 5) }).map((_, i) => (
                            <div key={i} style={{
                              position: 'absolute',
                              top: 4 + i * 2,
                              left: 4 + i * 1,
                              right: 4 - i * 1,
                              bottom: 4 - i * 2,
                              borderRadius: 4,
                              background: `linear-gradient(135deg, ${i === Math.min(pileSize, 5) - 1 ? '#4a3a7a' : '#3a2a6a'} 0%, ${i === Math.min(pileSize, 5) - 1 ? '#2a1a5a' : '#1a0a4a'} 100%)`,
                              border: '1px solid rgba(255,255,255,0.1)',
                            }} />
                          ))}
                          {/* Card count */}
                          <div style={{
                            position: 'relative', zIndex: 1,
                            fontSize: 28, fontWeight: 700,
                            color: isExaminable ? '#4ade80' : 'rgba(255,255,255,0.7)',
                          }}>
                            {pileSize}
                          </div>
                          <div style={{
                            position: 'relative', zIndex: 1,
                            fontSize: 11, color: 'rgba(255,255,255,0.4)',
                          }}>
                            card{pileSize !== 1 ? 's' : ''}
                          </div>
                        </>
                      )}
                    </div>

                    {/* Current pile indicator */}
                    {isExaminable && (
                      <div style={{
                        marginTop: 8, fontSize: 11,
                        color: '#4ade80', fontWeight: 600,
                      }}>
                        Examining
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            {/* Current pile contents (if examining) */}
            {winstonState.isYourTurn && winstonState.currentPileCards && winstonState.currentPileCards.length > 0 && (
              <div style={{
                background: 'rgba(0,0,0,0.3)',
                borderRadius: 8,
                padding: 16,
                marginBottom: 16,
              }}>
                <div style={{
                  fontSize: 13, fontWeight: 600, color: 'rgba(255,255,255,0.6)',
                  marginBottom: 12, textTransform: 'uppercase', letterSpacing: '0.05em',
                }}>
                  Pile {winstonState.currentPileIndex + 1} Contents ({winstonState.currentPileCards.length} card{winstonState.currentPileCards.length !== 1 ? 's' : ''})
                </div>
                <div style={{
                  display: 'flex', flexWrap: 'wrap', gap: 8,
                  justifyContent: 'center',
                }}>
                  {winstonState.currentPileCards.map((card, i) => (
                    <PileCard
                      key={`${card.name}-${i}`}
                      card={card}
                      onHover={handleHover}
                      isMobile={isMobile}
                    />
                  ))}
                </div>
              </div>
            )}

            {/* Action buttons */}
            {winstonState.isYourTurn && (
              <div style={{
                display: 'flex', gap: 12, justifyContent: 'center',
                marginBottom: 16,
              }}>
                <button
                  onClick={winstonTakePile}
                  disabled={winstonState.pileSizes[winstonState.currentPileIndex] === 0}
                  style={{
                    background: 'linear-gradient(135deg, #4ade80, #22c55e)',
                    border: 'none', borderRadius: 6,
                    padding: '10px 24px',
                    color: '#000', fontWeight: 700, fontSize: 14,
                    cursor: winstonState.pileSizes[winstonState.currentPileIndex] === 0 ? 'not-allowed' : 'pointer',
                    opacity: winstonState.pileSizes[winstonState.currentPileIndex] === 0 ? 0.3 : 1,
                    textTransform: 'uppercase', letterSpacing: '0.05em',
                  }}
                >
                  Take Pile {winstonState.currentPileIndex + 1}
                </button>
                <button
                  onClick={winstonSkipPile}
                  style={{
                    background: 'rgba(255,255,255,0.1)',
                    border: '1px solid rgba(255,255,255,0.2)',
                    borderRadius: 6,
                    padding: '10px 24px',
                    color: '#fff', fontWeight: 600, fontSize: 14,
                    cursor: 'pointer',
                    textTransform: 'uppercase', letterSpacing: '0.05em',
                  }}
                >
                  {winstonState.currentPileIndex === 2
                    ? 'Skip (Blind Pick)'
                    : `Skip → Pile ${winstonState.currentPileIndex + 2}`}
                </button>
              </div>
            )}

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
                Picked Cards ({winstonState.pickedCards.length})
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
              {winstonState.pickedCards.length === 0 && (
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

      {/* Pick animation overlay */}
      {pickAnimation && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 160, pointerEvents: 'none',
          background: pickAnimation.phase === 'enter'
            ? 'rgba(0,0,0,0.4)' : 'rgba(0,0,0,0)',
          transition: 'background 0.3s',
        }}>
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            gap: 16,
            opacity: pickAnimation.phase === 'enter' ? 1 : 0,
            transform: pickAnimation.phase === 'enter' ? 'scale(1) translateY(0)' : 'scale(0.8) translateY(30px)',
            transition: 'opacity 0.5s cubic-bezier(0.16, 1, 0.3, 1), transform 0.5s cubic-bezier(0.16, 1, 0.3, 1)',
          }}>
            <div style={{
              fontSize: 14, fontWeight: 700, textTransform: 'uppercase',
              letterSpacing: '0.1em',
              color: '#4ade80',
              textShadow: '0 0 20px rgba(74,222,128,0.5)',
            }}>
              {pickAnimation.cards.length === 1 ? 'Card Picked' : `${pickAnimation.cards.length} Cards Picked`}
            </div>
            <div style={{
              display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap',
            }}>
              {pickAnimation.cards.map((card, i) => {
                const imageUrl = getCardImageUrl(card.name, card.imageUri, 'normal')
                return (
                  <div key={`${card.name}-${i}`} style={{
                    width: isMobile ? 130 : 180,
                    borderRadius: 8,
                    overflow: 'hidden',
                    boxShadow: '0 0 30px rgba(74,222,128,0.4), 0 8px 32px rgba(0,0,0,0.6)',
                    border: '2px solid rgba(74,222,128,0.6)',
                    animation: 'pickCardAppear 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards',
                    animationDelay: `${i * 80}ms`,
                    opacity: 0,
                  }}>
                    {imageUrl ? (
                      <img src={imageUrl} alt={card.name} style={{ width: '100%', display: 'block' }} />
                    ) : (
                      <div style={{
                        background: 'linear-gradient(135deg, #2a2a4a 0%, #1a1a3a 100%)',
                        padding: 12, minHeight: isMobile ? 180 : 250,
                        display: 'flex', flexDirection: 'column',
                      }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                          <span style={{ fontSize: 13, fontWeight: 700, color: '#fff' }}>{card.name}</span>
                          {card.manaCost && <ManaCost cost={card.manaCost} size={12} />}
                        </div>
                        <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', marginBottom: 6 }}>{card.typeLine}</div>
                        {card.oracleText && (
                          <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.7)', flex: 1 }}>{card.oracleText}</div>
                        )}
                        {card.power != null && card.toughness != null && (
                          <div style={{ fontSize: 13, fontWeight: 700, color: '#fff', textAlign: 'right', marginTop: 6 }}>
                            {card.power}/{card.toughness}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      )}

      {/* Opponent known cards overlay */}
      {viewingOpponent && (
        <OpponentKnownCardsOverlay
          opponentName={winstonState.isYourTurn ? 'Opponent' : winstonState.activePlayerName}
          knownCards={winstonState.knownOpponentCards}
          unknownCount={winstonState.unknownOpponentCardCount}
          onClose={() => setViewingOpponent(false)}
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
        @keyframes pickCardAppear {
          0% { opacity: 0; transform: scale(0.6) translateY(20px); }
          100% { opacity: 1; transform: scale(1) translateY(0); }
        }
      `}</style>
    </div>
  )
}

/**
 * Overlay showing opponent's cards that you have seen (examined in piles).
 */
function OpponentKnownCardsOverlay({ opponentName, knownCards, unknownCount, onClose, onHover }: {
  opponentName: string
  knownCards: readonly SealedCardInfo[]
  unknownCount: number
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

    for (const card of knownCards) {
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
  }, [knownCards])

  const totalKnown = knownCards.length

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
          background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
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
              {opponentName}'s Cards
            </div>
            <div style={{ color: 'rgba(255,255,255,0.4)', fontSize: 12, marginTop: 2 }}>
              {totalKnown} known / {unknownCount} unknown
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
          background: 'rgba(255,255,255,0.04)',
          borderBottom: '1px solid rgba(255,255,255,0.08)',
          color: 'rgba(255,255,255,0.4)',
          fontSize: 12,
          textAlign: 'center',
        }}>
          Showing opponent cards you've seen in piles
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
          {totalKnown === 0 && unknownCount === 0 && (
            <div style={{
              textAlign: 'center', color: 'rgba(255,255,255,0.3)',
              padding: 20, fontSize: 13,
            }}>
              No cards picked yet
            </div>
          )}
          {totalKnown === 0 && unknownCount > 0 && (
            <div style={{
              textAlign: 'center', color: 'rgba(255,255,255,0.3)',
              padding: 20, fontSize: 13,
            }}>
              No known cards — opponent picked {unknownCount} card{unknownCount !== 1 ? 's' : ''} from piles you haven't seen
            </div>
          )}
          {/* Unknown cards count */}
          {unknownCount > 0 && totalKnown > 0 && (
            <div style={{
              marginTop: 12,
              padding: '8px 12px',
              background: 'rgba(255,255,255,0.05)',
              borderRadius: 4,
              color: 'rgba(255,255,255,0.4)',
              fontSize: 12,
              textAlign: 'center',
            }}>
              + {unknownCount} unknown card{unknownCount !== 1 ? 's' : ''} from piles you haven't seen
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

/**
 * Single card in the pile display.
 */
function PileCard({ card, onHover, isMobile }: {
  card: SealedCardInfo
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  isMobile: boolean
}) {
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'normal')

  return (
    <div
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        width: isMobile ? 110 : 140,
        borderRadius: 6,
        overflow: 'hidden',
        border: '1px solid rgba(255,255,255,0.15)',
        background: 'rgba(0,0,0,0.3)',
        cursor: 'pointer',
        transition: 'transform 0.15s',
      }}
    >
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={card.name}
          style={{ width: '100%', display: 'block' }}
          loading="lazy"
        />
      ) : (
        <div style={{
          padding: 8,
          minHeight: isMobile ? 150 : 190,
          display: 'flex', flexDirection: 'column',
        }}>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
            marginBottom: 4,
          }}>
            <span style={{ fontSize: 11, fontWeight: 600, color: '#fff', flex: 1 }}>
              {card.name}
            </span>
            {card.manaCost && (
              <ManaCost cost={card.manaCost} size={11} />
            )}
          </div>
          <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', marginBottom: 4 }}>
            {card.typeLine}
          </div>
          {card.oracleText && (
            <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.6)', flex: 1 }}>
              {card.oracleText}
            </div>
          )}
          {card.power != null && card.toughness != null && (
            <div style={{
              fontSize: 11, fontWeight: 700, color: '#fff',
              textAlign: 'right', marginTop: 4,
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
