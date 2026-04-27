import { useState, useMemo, useCallback, useEffect } from 'react'
import { useGameStore, type DraftState } from '@/store/gameStore.ts'
import type { SealedCardInfo, LobbySettings } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { ManaCost } from '../ui/ManaSymbols'
import { HoverCardPreview } from '../ui/HoverCardPreview'
import { useDfcHoverFlip } from '../ui/useDfcHoverFlip'
import { SetSynergiesButton } from './SetSynergiesOverlay'

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
  const playerId = useGameStore((s) => s.playerId)
  const isHost = lobbyState?.isHost ?? false

  // Build ordered player list in pack-passing order with pack counts
  const playerOrder = useMemo(() => {
    const players = lobbyState?.players ?? []
    if (players.length < 2 || !playerId) return null

    const myIndex = players.findIndex((p) => p.playerId === playerId)
    if (myIndex === -1) return null

    // Order players in passing direction starting from "you"
    // LEFT passes from i to i+1, RIGHT passes from i to i-1
    const n = players.length
    const step = draftState.passDirection === 'LEFT' ? 1 : -1
    const ordered: Array<{ name: string; isYou: boolean; packCount: number }> = []

    for (let i = 0; i < n; i++) {
      const idx = ((myIndex + i * step) % n + n) % n
      const p = players[idx]!
      ordered.push({
        name: p.playerName,
        isYou: p.playerId === playerId,
        packCount: draftState.playerPackCounts[p.playerName] ?? 0,
      })
    }

    return ordered
  }, [lobbyState?.players, playerId, draftState.passDirection, draftState.playerPackCounts])

  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [selectedCards, setSelectedCards] = useState<string[]>([])
  // On mobile, sidebar is hidden by default; on desktop it's always shown
  const [showPickedCards, setShowPickedCards] = useState(!responsive.isMobile)

  // How many cards to pick this round
  const picksRequired = draftState.picksPerRound

  const dfc = useDfcHoverFlip(hoveredCard)
  const resetDfcFlip = dfc.resetFlip

  const handleHover = useCallback((card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard((prev) => {
      if (prev?.name !== card?.name) resetDfcFlip()
      return card
    })
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }, [resetDfcFlip])

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

  // Auto-submit selected cards when timer expires (before server auto-picks first N)
  useEffect(() => {
    if (draftState.timeRemaining === 0 && draftState.currentPack.length > 0) {
      // Fill remaining slots with unselected cards from the pack
      const picks = [...selectedCards]
      if (picks.length < picksRequired) {
        for (const card of draftState.currentPack) {
          if (!picks.includes(card.name) && picks.length < picksRequired) {
            picks.push(card.name)
          }
        }
      }
      if (picks.length > 0) {
        makePick(picks)
        setSelectedCards([])
      }
    }
  }, [draftState.timeRemaining, draftState.currentPack, selectedCards, picksRequired, makePick])

  // Timer warning threshold
  const timerWarning = draftState.timeRemaining <= 10

  // Group picked cards by color for sidebar, with counts for duplicates
  const pickedByColor = useMemo(() => {
    const groups: Record<string, Array<{ card: SealedCardInfo; count: number }>> = {
      W: [], U: [], B: [], R: [], G: [], M: [], C: [],
    }
    // Count occurrences of each card by name within each color
    const colorCounts: Record<string, Map<string, { card: SealedCardInfo; count: number }>> = {
      W: new Map(), U: new Map(), B: new Map(), R: new Map(), G: new Map(), M: new Map(), C: new Map(),
    }
    for (const card of draftState.pickedCards) {
      const colors = getCardColors(card)
      let colorKey: string
      if (colors.size === 0) {
        colorKey = 'C'
      } else if (colors.size > 1) {
        colorKey = 'M'
      } else {
        colorKey = [...colors][0]!
      }
      const existing = colorCounts[colorKey]!.get(card.name)
      if (existing) {
        existing.count++
      } else {
        colorCounts[colorKey]!.set(card.name, { card, count: 1 })
      }
    }
    // Convert maps to arrays and sort by CMC, then by name
    for (const color of Object.keys(groups)) {
      groups[color] = Array.from(colorCounts[color]!.values()).sort((a, b) => {
        const cmcDiff = getCmc(a.card) - getCmc(b.card)
        if (cmcDiff !== 0) return cmcDiff
        return a.card.name.localeCompare(b.card.name)
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

    const creatureTypes = getCreatureSubtypes(draftState.pickedCards)

    return { creatures, spells, curve, creatureTypes }
  }, [draftState.pickedCards])

  // Group current pack by rarity for display
  const packByRarity = useMemo(() => {
    const groups: Record<string, SealedCardInfo[]> = {
      MYTHIC: [], RARE: [], UNCOMMON: [], COMMON: [],
    }
    for (const card of draftState.currentPack) {
      const rarity = card.rarity.toUpperCase()
      const rarityKey = ['MYTHIC', 'RARE', 'UNCOMMON'].includes(rarity) ? rarity : 'COMMON'
      groups[rarityKey]!.push(card)
    }
    return groups
  }, [draftState.currentPack])

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
            Draft - {settings.setNames.join(' + ')}
          </h2>
          <PackPickIndicator
            packNumber={draftState.packNumber}
            pickNumber={draftState.pickNumber}
            totalPacks={totalPacks}
          />
          <SetSynergiesButton setCodes={settings.setCodes} cardPool={draftState.pickedCards} />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* Pack flow indicator */}
          <PassDirectionIndicator playerOrder={playerOrder} passDirection={draftState.passDirection} />

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
          {/* Queued packs indicator */}
          {draftState.queuedPacks > 0 && draftState.currentPack.length > 0 && (
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
              <span style={{ color: '#ff9800', fontSize: 13 }}>
                {draftState.queuedPacks} more {draftState.queuedPacks === 1 ? 'pack' : 'packs'} queued
              </span>
            </div>
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
                  alignItems: 'flex-start',
                  maxWidth: 1200,
                }}
              >
                {(['MYTHIC', 'RARE', 'UNCOMMON', 'COMMON'] as const).flatMap((rarity) => {
                  const cards = packByRarity[rarity]
                  if (!cards || cards.length === 0) return []
                  return cards.map((card) => (
                    <PackCard
                      key={card.name}
                      card={card}
                      rarity={rarity}
                      isSelected={selectedCards.includes(card.name)}
                      onClick={() => handleCardClick(card.name)}
                      onHover={handleHover}
                      responsive={responsive}
                    />
                  ))
                })}
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
        <HoverCardPreview
          name={dfc.displayName ?? hoveredCard.name}
          imageUri={dfc.displayImageUri ?? hoveredCard.imageUri}
          pos={hoverPos}
          rulings={hoveredCard.rulings}
          overlay={dfc.hint}
        />
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

function PassDirectionIndicator({
  playerOrder,
  passDirection,
}: {
  playerOrder: Array<{ name: string; isYou: boolean; packCount: number }> | null
  passDirection: 'LEFT' | 'RIGHT'
}) {
  if (!playerOrder) return null

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 0,
        padding: '4px 8px',
        backgroundColor: '#2a2a2a',
        borderRadius: 6,
        border: '1px solid #444',
      }}
    >
      {/* Direction label */}
      <span
        style={{
          color: '#666',
          fontSize: 10,
          fontWeight: 600,
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
          marginRight: 8,
          whiteSpace: 'nowrap',
        }}
      >
        Pass {passDirection === 'LEFT' ? 'L' : 'R'}
      </span>

      {/* Player nodes in passing order */}
      {playerOrder.map((player, i) => {
        // 0 packs = idle/waiting, 1 = choosing, 2+ = packs queuing up
        const isIdle = player.packCount === 0
        const hasQueue = player.packCount > 1

        return (
          <div key={player.name} style={{ display: 'flex', alignItems: 'center' }}>
            {/* Arrow between players */}
            {i > 0 && (
              <span style={{ color: '#555', fontSize: 11, padding: '0 2px', flexShrink: 0 }}>
                {'\u203A'}
              </span>
            )}

            {/* Player node */}
            <div
              style={{
                padding: '2px 8px',
                borderRadius: 4,
                backgroundColor: player.isYou
                  ? 'rgba(79, 195, 247, 0.15)'
                  : isIdle
                    ? 'rgba(255, 255, 255, 0.02)'
                    : 'rgba(255, 255, 255, 0.04)',
                border: player.isYou
                  ? '1px solid rgba(79, 195, 247, 0.3)'
                  : '1px solid transparent',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
                opacity: isIdle && !player.isYou ? 0.5 : 1,
              }}
            >
              {/* Pack stack indicator */}
              <PackStackIcon count={player.packCount} />

              <span
                style={{
                  color: player.isYou
                    ? '#4fc3f7'
                    : isIdle
                      ? '#666'
                      : hasQueue
                        ? '#ff9800'
                        : '#8bc34a',
                  fontSize: 11,
                  fontWeight: player.isYou ? 700 : 600,
                  maxWidth: 80,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {player.isYou ? 'You' : player.name}
              </span>
            </div>
          </div>
        )
      })}

      {/* Wrap-around arrow back to first player */}
      <span style={{ color: '#555', fontSize: 11, padding: '0 2px', flexShrink: 0 }}>
        {'\u21BB'}
      </span>
    </div>
  )
}

/** Visual indicator showing pack count as stacked card icons. */
function PackStackIcon({ count }: { count: number }) {
  if (count === 0) {
    // Empty — no pack
    return (
      <div
        style={{
          width: 8,
          height: 10,
          borderRadius: 1,
          border: '1px dashed #555',
          flexShrink: 0,
        }}
      />
    )
  }

  // Show stacked cards for count >= 1
  return (
    <div style={{ position: 'relative', width: 8 + Math.min(count - 1, 3) * 2, height: 12, flexShrink: 0 }}>
      {Array.from({ length: Math.min(count, 4) }, (_, i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            left: i * 2,
            top: i === 0 ? 0 : 1,
            width: 8,
            height: 10,
            borderRadius: 1,
            backgroundColor: count > 1 ? '#ff9800' : '#4caf50',
            border: '1px solid rgba(0,0,0,0.3)',
          }}
        />
      ))}
      {count > 1 && (
        <span
          style={{
            position: 'absolute',
            right: -2,
            top: -4,
            fontSize: 8,
            fontWeight: 700,
            color: '#ff9800',
            lineHeight: 1,
          }}
        >
          {count}
        </span>
      )}
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


function PackCard({
  card,
  rarity,
  isSelected,
  onClick,
  onHover,
  responsive,
}: {
  card: SealedCardInfo
  rarity: 'MYTHIC' | 'RARE' | 'UNCOMMON' | 'COMMON'
  isSelected: boolean
  onClick: () => void
  onHover: (card: SealedCardInfo | null, e?: React.MouseEvent) => void
  responsive: ReturnType<typeof useResponsive>
}) {
  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'normal')

  const rarityColors: Record<string, string> = {
    MYTHIC: '#ff8b00',
    RARE: '#ffd700',
    UNCOMMON: '#c0c0c0',
    COMMON: '#555',
  }

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
      {/* Rarity indicator strip at bottom */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          height: 4,
          backgroundColor: rarityColors[rarity],
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
  pickedByColor: Record<string, Array<{ card: SealedCardInfo; count: number }>>
  analytics: { creatures: number; spells: number; curve: Record<number, number>; creatureTypes: Array<{ type: string; count: number; legendaryCount: number }> }
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

        {/* Creature types */}
        {analytics.creatureTypes.length > 0 && (
          <div style={{ marginTop: 8 }}>
            <div style={{ color: '#666', fontSize: 9, marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Top Creature Types
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
              {analytics.creatureTypes.slice(0, 6).map(({ type, count, legendaryCount }) => (
                <span
                  key={type}
                  style={{
                    padding: '1px 6px',
                    backgroundColor: '#333',
                    borderRadius: 3,
                    fontSize: 9,
                    color: '#bbb',
                    border: '1px solid #444',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {type} <span style={{ color: '#8bc34a', fontWeight: 600 }}>{count}</span>
                  {legendaryCount > 0 && <span style={{ color: '#ffd54f', fontWeight: 600, fontSize: 9 }} title={`${legendaryCount} legendary`}>{'\u2605'}{legendaryCount}</span>}
                </span>
              ))}
            </div>
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
            const entries = pickedByColor[color] ?? []
            if (entries.length === 0) return null
            const totalInColor = entries.reduce((sum, e) => sum + e.count, 0)
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
                    {colorNames[color]} ({totalInColor})
                  </span>
                </div>
                {entries.map(({ card, count }) => (
                  <PickedCardRow
                    key={card.name}
                    card={card}
                    count={count}
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
  count,
  onHover,
}: {
  card: SealedCardInfo
  count: number
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
      {count > 1 && (
        <span
          style={{
            color: '#4fc3f7',
            fontSize: 11,
            fontWeight: 600,
            marginRight: 6,
            minWidth: 16,
          }}
        >
          {count}x
        </span>
      )}
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

function getCreatureSubtypes(cards: readonly SealedCardInfo[]): Array<{ type: string; count: number; legendaryCount: number }> {
  const counts = new Map<string, number>()
  const legendaryCounts = new Map<string, number>()
  for (const card of cards) {
    if (!card.typeLine.toLowerCase().includes('creature')) continue
    const dashIndex = card.typeLine.indexOf('\u2014')
    const hyphenIndex = card.typeLine.indexOf(' - ')
    const splitIndex = dashIndex !== -1 ? dashIndex : hyphenIndex
    if (splitIndex === -1) continue
    const isLegendary = card.typeLine.toLowerCase().includes('legendary')
    const subtypePart = card.typeLine.slice(splitIndex + (dashIndex !== -1 ? 1 : 3)).trim()
    for (const subtype of subtypePart.split(/\s+/)) {
      const trimmed = subtype.trim()
      if (trimmed) {
        counts.set(trimmed, (counts.get(trimmed) || 0) + 1)
        if (isLegendary) {
          legendaryCounts.set(trimmed, (legendaryCounts.get(trimmed) || 0) + 1)
        }
      }
    }
  }
  return Array.from(counts.entries())
    .map(([type, count]) => ({ type, count, legendaryCount: legendaryCounts.get(type) ?? 0 }))
    .sort((a, b) => b.count - a.count)
}
