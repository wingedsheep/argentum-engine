import { useState, useMemo, useCallback, useEffect } from 'react'
import { useGameStore, type DraftState } from '../../store/gameStore'
import type { SealedCardInfo, LobbySettings } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'
import { ManaCost } from '../ui/ManaSymbols'

/**
 * Draft Pick overlay for draft mode.
 * Displays the current pack for picking and tracks picked cards.
 */
export function DraftPickOverlay() {
  const lobbyState = useGameStore((state) => state.lobbyState)
  const draftState = lobbyState?.draftState

  if (!draftState || lobbyState?.state !== 'DRAFTING') return null

  return <DraftPicker draftState={draftState} settings={lobbyState.settings} />
}

function DraftPicker({ draftState, settings }: { draftState: DraftState; settings: LobbySettings }) {
  const responsive = useResponsive()
  const makePick = useGameStore((s) => s.makePick)
  const leaveLobby = useGameStore((s) => s.leaveLobby)
  const stopLobby = useGameStore((s) => s.stopLobby)
  const lobbyState = useGameStore((s) => s.lobbyState)
  const isHost = lobbyState?.isHost ?? false

  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [selectedCards, setSelectedCards] = useState<string[]>([])
  // On mobile, sidebar is hidden by default; on desktop it's always shown
  const [showPickedCards, setShowPickedCards] = useState(!responsive.isMobile)

  // How many cards to pick this round
  const picksRequired = draftState.picksPerRound

  const handleHover = useCallback((card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard(card)
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }, [])

  const handleCardClick = useCallback((cardName: string) => {
    setSelectedCards((prev) => {
      if (prev.includes(cardName)) {
        // Deselect
        return prev.filter((c) => c !== cardName)
      } else if (prev.length < picksRequired) {
        // Select if we haven't reached the limit
        return [...prev, cardName]
      }
      // Already at limit, don't add more
      return prev
    })
  }, [picksRequired])

  const handleConfirmPick = useCallback(() => {
    if (selectedCards.length === picksRequired) {
      makePick(selectedCards)
      setSelectedCards([])
    }
  }, [selectedCards, picksRequired, makePick])

  // Timer warning threshold
  const timerWarning = draftState.timeRemaining <= 10

  // Group picked cards by color for sidebar, sorted by CMC within each color
  const pickedByColor = useMemo(() => {
    const groups: Record<string, SealedCardInfo[]> = {
      W: [], U: [], B: [], R: [], G: [], C: [], M: [],
    }
    for (const card of draftState.pickedCards) {
      const colors = getCardColors(card)
      if (colors.size === 0) {
        groups['C']!.push(card)
      } else if (colors.size > 1) {
        groups['M']!.push(card)
      } else {
        const color = [...colors][0]!
        groups[color]!.push(card)
      }
    }
    // Sort each color group by CMC, then by name
    for (const color of Object.keys(groups)) {
      groups[color]!.sort((a, b) => {
        const cmcDiff = getCmc(a) - getCmc(b)
        if (cmcDiff !== 0) return cmcDiff
        return a.name.localeCompare(b.name)
      })
    }
    return groups
  }, [draftState.pickedCards])

  // Analytics for picked cards
  const pickedAnalytics = useMemo(() => {
    let creatures = 0
    let spells = 0
    const curve: Record<number, number> = {}

    for (const card of draftState.pickedCards) {
      const isCreature = card.typeLine.toLowerCase().includes('creature')
      if (isCreature) {
        creatures++
      } else {
        spells++
      }
      const cmc = Math.min(getCmc(card), 7)
      curve[cmc] = (curve[cmc] || 0) + 1
    }

    return { creatures, spells, curve }
  }, [draftState.pickedCards])

  const totalPicked = draftState.pickedCards.length
  const totalPacks = settings.boosterCount
  const totalPicks = totalPacks * 15 // packs * cards per pack

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
          padding: responsive.isMobile ? '8px 12px' : '12px 24px',
          backgroundColor: '#222',
          borderBottom: '1px solid #444',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 12,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 16 : 22 }}>
            Draft - {settings.setName}
          </h2>
          <PackPickIndicator
            packNumber={draftState.packNumber}
            pickNumber={draftState.pickNumber}
            totalPacks={totalPacks}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* Pass direction indicator */}
          <PassDirectionIndicator direction={draftState.passDirection} />

          {/* Timer - only show when player has a pack to pick from */}
          {draftState.currentPack.length > 0 ? (
            <Timer seconds={draftState.timeRemaining} warning={timerWarning} />
          ) : (
            <div
              style={{
                padding: '6px 14px',
                backgroundColor: 'rgba(255, 152, 0, 0.15)',
                borderRadius: 6,
                color: '#ff9800',
                fontWeight: 600,
                fontSize: 14,
              }}
            >
              Waiting...
            </div>
          )}

          {/* Picked count */}
          <div
            style={{
              padding: '6px 14px',
              backgroundColor: '#333',
              borderRadius: 6,
              color: '#4fc3f7',
              fontWeight: 600,
              fontSize: responsive.fontSize.normal,
            }}
          >
            {totalPicked} / {totalPicks}
          </div>

          {/* Toggle picked cards sidebar - mobile only */}
          {responsive.isMobile && (
            <button
              onClick={() => setShowPickedCards(!showPickedCards)}
              style={{
                padding: '6px 14px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: showPickedCards ? '#4fc3f7' : '#444',
                color: showPickedCards ? '#000' : '#ccc',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
              }}
            >
              Pool
            </button>
          )}

          {/* Leave/Stop button */}
          {isHost ? (
            <button
              onClick={stopLobby}
              style={{
                padding: '6px 14px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#c0392b',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
              }}
            >
              Stop Draft
            </button>
          ) : (
            <button
              onClick={leaveLobby}
              style={{
                padding: '6px 14px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#c0392b',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer',
              }}
            >
              Leave
            </button>
          )}
        </div>
      </div>

      {/* Main content */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Pack cards area */}
        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
          }}
        >
          {/* Waiting indicator */}
          {draftState.waitingForPlayers.length > 0 && (
            <WaitingIndicator players={draftState.waitingForPlayers} />
          )}

          {/* Pack cards grid */}
          <div
            style={{
              flex: 1,
              overflow: 'auto',
              padding: responsive.isMobile ? 12 : 24,
              display: 'flex',
              justifyContent: 'center',
              alignItems: draftState.currentPack.length > 0 ? 'flex-start' : 'center',
            }}
          >
            {draftState.currentPack.length > 0 ? (
              <div
                style={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: responsive.isMobile ? 8 : 12,
                  justifyContent: 'center',
                  maxWidth: 1200,
                }}
              >
                {draftState.currentPack.map((card) => (
                  <PackCard
                    key={card.name}
                    card={card}
                    isSelected={selectedCards.includes(card.name)}
                    onClick={() => handleCardClick(card.name)}
                    onHover={handleHover}
                    responsive={responsive}
                  />
                ))}
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 16,
                  color: '#888',
                }}
              >
                <div
                  style={{
                    width: 48,
                    height: 48,
                    border: '3px solid #444',
                    borderTopColor: '#ff9800',
                    borderRadius: '50%',
                    animation: 'spin 1s linear infinite',
                  }}
                />
                <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
                <div style={{ fontSize: 18, fontWeight: 500 }}>
                  Waiting for next pack...
                </div>
                <div style={{ fontSize: 14, color: '#555' }}>
                  Other players are still making their picks
                </div>
              </div>
            )}
          </div>

          {/* Confirm pick button - only show when there's a pack */}
          {draftState.currentPack.length > 0 && (
            <div
              style={{
                padding: responsive.isMobile ? '12px' : '16px 24px',
                backgroundColor: '#222',
                borderTop: '1px solid #444',
                display: 'flex',
                justifyContent: 'center',
              }}
            >
              <button
                onClick={handleConfirmPick}
                disabled={selectedCards.length !== picksRequired}
                style={{
                  padding: responsive.isMobile ? '12px 32px' : '14px 48px',
                  fontSize: responsive.isMobile ? 16 : 18,
                  backgroundColor: selectedCards.length === picksRequired ? '#4caf50' : '#555',
                  color: 'white',
                  border: 'none',
                  borderRadius: 8,
                  cursor: selectedCards.length === picksRequired ? 'pointer' : 'not-allowed',
                  fontWeight: 600,
                  transition: 'background-color 0.15s',
                }}
              >
                {selectedCards.length === picksRequired
                  ? `Pick ${selectedCards.join(' & ')}`
                  : picksRequired > 1
                    ? `Select ${picksRequired} cards (${selectedCards.length}/${picksRequired})`
                    : 'Select a card'}
              </button>
            </div>
          )}
        </div>

        {/* Picked cards sidebar - always visible on desktop */}
        {(showPickedCards || !responsive.isMobile) && (
          <PickedCardsSidebar
            pickedByColor={pickedByColor}
            analytics={pickedAnalytics}
            totalPicked={totalPicked}
            onHover={handleHover}
            responsive={responsive}
          />
        )}
      </div>

      {/* Card preview on hover */}
      {hoveredCard && !responsive.isMobile && (
        <CardPreview card={hoveredCard} pos={hoverPos} />
      )}
    </div>
  )
}

function PackPickIndicator({ packNumber, pickNumber, totalPacks }: { packNumber: number; pickNumber: number; totalPacks: number }) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '4px 12px',
        backgroundColor: '#333',
        borderRadius: 6,
      }}
    >
      <span style={{ color: '#888', fontSize: 13 }}>Pack</span>
      <span style={{ color: '#4fc3f7', fontWeight: 700, fontSize: 16 }}>{packNumber}</span>
      <span style={{ color: '#666', fontSize: 12 }}>/ {totalPacks}</span>
      <span style={{ color: '#555' }}>|</span>
      <span style={{ color: '#888', fontSize: 13 }}>Pick</span>
      <span style={{ color: '#4fc3f7', fontWeight: 700, fontSize: 16 }}>{pickNumber}</span>
      <span style={{ color: '#666', fontSize: 12 }}>/ 15</span>
    </div>
  )
}

function PassDirectionIndicator({ direction }: { direction: 'LEFT' | 'RIGHT' }) {
  const isLeft = direction === 'LEFT'
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 6,
        padding: '4px 10px',
        backgroundColor: isLeft ? 'rgba(255, 152, 0, 0.15)' : 'rgba(33, 150, 243, 0.15)',
        borderRadius: 6,
        border: `1px solid ${isLeft ? '#ff9800' : '#2196f3'}`,
      }}
    >
      <span style={{ fontSize: 16 }}>{isLeft ? '←' : '→'}</span>
      <span style={{ color: isLeft ? '#ff9800' : '#2196f3', fontSize: 12, fontWeight: 600 }}>
        Pass {direction}
      </span>
    </div>
  )
}

function Timer({ seconds, warning }: { seconds: number; warning: boolean }) {
  const [pulse, setPulse] = useState(false)

  useEffect(() => {
    if (warning) {
      const interval = setInterval(() => {
        setPulse((p) => !p)
      }, 500)
      return () => clearInterval(interval)
    }
    setPulse(false)
  }, [warning])

  const formatTime = (s: number) => {
    const mins = Math.floor(s / 60)
    const secs = s % 60
    return mins > 0 ? `${mins}:${secs.toString().padStart(2, '0')}` : `${secs}s`
  }

  return (
    <div
      style={{
        padding: '6px 14px',
        backgroundColor: warning ? (pulse ? '#c0392b' : '#8e2a21') : '#333',
        borderRadius: 6,
        color: warning ? 'white' : '#ccc',
        fontWeight: 600,
        fontSize: 16,
        fontFamily: 'monospace',
        minWidth: 60,
        textAlign: 'center',
        transition: 'background-color 0.2s',
      }}
    >
      {formatTime(seconds)}
    </div>
  )
}

function WaitingIndicator({ players }: { players: readonly string[] }) {
  return (
    <div
      style={{
        padding: '8px 16px',
        backgroundColor: 'rgba(255, 152, 0, 0.1)',
        borderBottom: '1px solid rgba(255, 152, 0, 0.3)',
        display: 'flex',
        alignItems: 'center',
        gap: 8,
      }}
    >
      <span style={{ color: '#ff9800', fontSize: 13 }}>Waiting for:</span>
      <span style={{ color: '#ffcc80', fontSize: 13 }}>
        {players.join(', ')}
      </span>
    </div>
  )
}

function PackCard({
  card,
  isSelected,
  onClick,
  onHover,
  responsive,
}: {
  card: SealedCardInfo
  isSelected: boolean
  onClick: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  responsive: ReturnType<typeof useResponsive>
}) {
  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'normal')

  return (
    <div
      onClick={onClick}
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        position: 'relative',
        width: cardWidth,
        height: cardHeight,
        borderRadius: 8,
        overflow: 'hidden',
        cursor: 'pointer',
        border: isSelected ? '3px solid #4caf50' : '3px solid transparent',
        boxShadow: isSelected
          ? '0 0 20px rgba(76, 175, 80, 0.5)'
          : '0 4px 12px rgba(0, 0, 0, 0.4)',
        transform: isSelected ? 'scale(1.05)' : 'scale(1)',
        transition: 'all 0.15s',
      }}
      onMouseOver={(e) => {
        if (!isSelected) {
          e.currentTarget.style.transform = 'scale(1.03)'
          e.currentTarget.style.boxShadow = '0 8px 24px rgba(0, 0, 0, 0.6)'
        }
      }}
      onMouseOut={(e) => {
        if (!isSelected) {
          e.currentTarget.style.transform = 'scale(1)'
          e.currentTarget.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.4)'
        }
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
      {isSelected && (
        <div
          style={{
            position: 'absolute',
            top: 8,
            right: 8,
            backgroundColor: '#4caf50',
            borderRadius: '50%',
            width: 24,
            height: 24,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontWeight: 700,
            fontSize: 14,
          }}
        >
          ✓
        </div>
      )}
    </div>
  )
}

function PickedCardsSidebar({
  pickedByColor,
  analytics,
  totalPicked,
  onHover,
  responsive,
}: {
  pickedByColor: Record<string, SealedCardInfo[]>
  analytics: { creatures: number; spells: number; curve: Record<number, number> }
  totalPicked: number
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  responsive: ReturnType<typeof useResponsive>
}) {
  const colorOrder = ['W', 'U', 'B', 'R', 'G', 'M', 'C']
  const colorNames: Record<string, string> = {
    W: 'White', U: 'Blue', B: 'Black', R: 'Red', G: 'Green', M: 'Multicolor', C: 'Colorless',
  }
  const colorStyles: Record<string, string> = {
    W: '#f9faf4', U: '#0e68ab', B: '#6a6a6a', R: '#d32f2f', G: '#388e3c', M: '#ffd700', C: '#888',
  }

  const maxCurveCount = Math.max(1, ...Object.values(analytics.curve))

  return (
    <div
      style={{
        width: responsive.isMobile ? '100%' : 280,
        backgroundColor: '#1e1e1e',
        borderLeft: '1px solid #444',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      {/* Header with stats */}
      <div
        style={{
          padding: '10px 14px',
          backgroundColor: '#252525',
          borderBottom: '1px solid #333',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <span style={{ color: '#ccc', fontSize: 14, fontWeight: 600 }}>
            Card Pool
          </span>
          <span style={{ color: '#4fc3f7', fontSize: 13, fontWeight: 600 }}>
            {totalPicked} cards
          </span>
        </div>

        {/* Creature/Spell counts */}
        <div style={{ display: 'flex', gap: 16, marginBottom: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ color: '#8bc34a', fontSize: 14, fontWeight: 700 }}>{analytics.creatures}</span>
            <span style={{ color: '#666', fontSize: 10 }}>Creatures</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ color: '#4fc3f7', fontSize: 14, fontWeight: 700 }}>{analytics.spells}</span>
            <span style={{ color: '#666', fontSize: 10 }}>Spells</span>
          </div>
        </div>

        {/* Mana curve */}
        {totalPicked > 0 && (
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 2, height: 32 }}>
            {[0, 1, 2, 3, 4, 5, 6, 7].map((cmc) => {
              const count = analytics.curve[cmc] || 0
              const height = maxCurveCount > 0 ? (count / maxCurveCount) * 24 : 0
              return (
                <div key={cmc} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1, minWidth: 0 }}>
                  {count > 0 && (
                    <span style={{ color: '#888', fontSize: 8, marginBottom: 1 }}>{count}</span>
                  )}
                  <div
                    style={{
                      width: '100%',
                      maxWidth: 16,
                      height: Math.max(height, count > 0 ? 2 : 0),
                      backgroundColor: count > 0 ? '#4fc3f7' : 'transparent',
                      borderRadius: '1px 1px 0 0',
                    }}
                  />
                  <span style={{ color: '#555', fontSize: 8, marginTop: 1 }}>
                    {cmc >= 7 ? '7+' : cmc}
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Card list by color */}
      <div style={{ flex: 1, overflow: 'auto', padding: '8px 0' }}>
        {totalPicked === 0 ? (
          <div style={{ padding: '24px 14px', textAlign: 'center', color: '#555', fontSize: 12 }}>
            Pick cards to build your pool
          </div>
        ) : (
          colorOrder.map((color) => {
            const cards = pickedByColor[color] ?? []
            if (cards.length === 0) return null
            return (
              <div key={color} style={{ marginBottom: 8 }}>
                <div
                  style={{
                    padding: '4px 14px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                  }}
                >
                  <div
                    style={{
                      width: 10,
                      height: 10,
                      borderRadius: '50%',
                      backgroundColor: colorStyles[color],
                    }}
                  />
                  <span style={{ color: '#888', fontSize: 10, fontWeight: 600, textTransform: 'uppercase' }}>
                    {colorNames[color]} ({cards.length})
                  </span>
                </div>
                {cards.map((card) => (
                  <PickedCardRow
                    key={card.name}
                    card={card}
                    onHover={onHover}
                  />
                ))}
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}

function PickedCardRow({
  card,
  onHover,
}: {
  card: SealedCardInfo
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
}) {
  return (
    <div
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={() => onHover(null)}
      style={{
        display: 'flex',
        alignItems: 'center',
        height: 26,
        padding: '0 14px',
        borderBottom: '1px solid #2a2a2a',
      }}
      onMouseOver={(e) => {
        e.currentTarget.style.backgroundColor = 'rgba(79, 195, 247, 0.1)'
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.backgroundColor = 'transparent'
      }}
    >
      <span
        style={{
          color: '#ddd',
          fontSize: 12,
          flex: 1,
          whiteSpace: 'nowrap',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
        }}
      >
        {card.name}
      </span>
      {card.manaCost && (
        <span style={{ marginLeft: 4, flexShrink: 0 }}>
          <ManaCost cost={card.manaCost} size={11} />
        </span>
      )}
    </div>
  )
}

function CardPreview({ card, pos }: { card: SealedCardInfo; pos: { x: number; y: number } | null }) {
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'large')
  const previewWidth = 280
  const previewHeight = Math.round(previewWidth * 1.4)

  let top = 80
  let left = 20
  if (pos) {
    const margin = 20
    if (pos.x + previewWidth + margin + 20 < window.innerWidth) {
      left = pos.x + margin
    } else {
      left = pos.x - previewWidth - margin
    }
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
    </div>
  )
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
