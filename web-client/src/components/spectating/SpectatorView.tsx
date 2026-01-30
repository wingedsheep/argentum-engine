import { useEffect, useState, useCallback, useRef } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { SpectatorPlayerState, SpectatorCardInfo, SpectatorCombatState } from '../../types/messages'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl, getScryfallFallbackUrl } from '../../utils/cardImages'

interface Point {
  x: number
  y: number
}

interface ArrowData {
  key: string
  start: Point
  end: Point
  color: string
}

/**
 * SVG arrow component with curved path and arrowhead.
 */
function Arrow({ start, end, color }: { start: Point; end: Point; color: string }) {
  const midX = (start.x + end.x) / 2
  const midY = (start.y + end.y) / 2
  const dx = end.x - start.x
  const dy = end.y - start.y
  const distance = Math.sqrt(dx * dx + dy * dy)

  // Arc height based on distance
  const arcHeight = Math.min(distance * 0.2, 60)
  const controlX = midX
  const controlY = midY - arcHeight

  const pathD = `M ${start.x} ${start.y} Q ${controlX} ${controlY} ${end.x} ${end.y}`

  // Calculate arrowhead direction
  const tangentX = end.x - controlX
  const tangentY = end.y - controlY
  const tangentLen = Math.sqrt(tangentX * tangentX + tangentY * tangentY)
  const normX = tangentX / tangentLen
  const normY = tangentY / tangentLen

  const arrowSize = 12
  const arrowAngle = Math.PI / 6

  const cos = Math.cos(arrowAngle)
  const sin = Math.sin(arrowAngle)

  const arrow1X = end.x - arrowSize * (normX * cos + normY * sin)
  const arrow1Y = end.y - arrowSize * (normY * cos - normX * sin)
  const arrow2X = end.x - arrowSize * (normX * cos - normY * sin)
  const arrow2Y = end.y - arrowSize * (normY * cos + normX * sin)

  const arrowheadD = `M ${end.x} ${end.y} L ${arrow1X} ${arrow1Y} M ${end.x} ${end.y} L ${arrow2X} ${arrow2Y}`

  return (
    <g>
      <path d={pathD} fill="none" stroke={color} strokeWidth={8} strokeOpacity={0.3} strokeLinecap="round" />
      <path d={pathD} fill="none" stroke={color} strokeWidth={3} strokeOpacity={0.9} strokeLinecap="round" />
      <path d={arrowheadD} fill="none" stroke={color} strokeWidth={3} strokeOpacity={0.9} strokeLinecap="round" strokeLinejoin="round" />
    </g>
  )
}

/**
 * Get the center of an element by its spectator card ID.
 */
function getSpectatorCardCenter(cardId: string): Point | null {
  const element = document.querySelector(`[data-spectator-card-id="${cardId}"]`)
  if (!element) return null
  const rect = element.getBoundingClientRect()
  return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 }
}

/**
 * Get the center of a player life display.
 */
function getSpectatorPlayerCenter(playerId: string): Point | null {
  const element = document.querySelector(`[data-spectator-life="${playerId}"]`)
  if (!element) return null
  const rect = element.getBoundingClientRect()
  return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 }
}

// Life change animation duration
const ANIMATION_DURATION = 800

/**
 * Spectator view for watching games during tournament byes.
 * Shows both players' boards with proper zones, life totals, and game phase.
 */
export function SpectatorView() {
  const spectatingState = useGameStore((state) => state.spectatingState)
  const stopSpectating = useGameStore((state) => state.stopSpectating)
  const responsive = useResponsive()

  // Track previous life totals for animations
  const prevLifeRef = useRef<{ p1: number; p2: number } | null>(null)
  const [lifeAnimations, setLifeAnimations] = useState<LifeAnimation[]>([])

  const { player1, player2, currentPhase, activePlayerId, priorityPlayerId, combat } = spectatingState || {}

  // Detect life changes and create animations
  useEffect(() => {
    if (!player1 || !player2) return

    const prev = prevLifeRef.current
    if (prev) {
      const animations: LifeAnimation[] = []

      if (prev.p1 !== player1.life) {
        const diff = player1.life - prev.p1
        animations.push({
          id: `p1-${Date.now()}`,
          playerId: player1.playerId,
          amount: Math.abs(diff),
          isGain: diff > 0,
          startTime: Date.now(),
        })
      }

      if (prev.p2 !== player2.life) {
        const diff = player2.life - prev.p2
        animations.push({
          id: `p2-${Date.now()}`,
          playerId: player2.playerId,
          amount: Math.abs(diff),
          isGain: diff > 0,
          startTime: Date.now(),
        })
      }

      if (animations.length > 0) {
        setLifeAnimations((prev) => [...prev, ...animations])
      }
    }

    prevLifeRef.current = { p1: player1.life, p2: player2.life }
  }, [player1?.life, player2?.life, player1?.playerId, player2?.playerId])

  const removeAnimation = useCallback((id: string) => {
    setLifeAnimations((prev) => prev.filter((a) => a.id !== id))
  }, [])

  if (!spectatingState) return null

  // Show loading state while waiting for first state update
  if (!player1 || !player2) {
    return (
      <div style={containerStyle}>
        <div style={{ textAlign: 'center', color: 'white' }}>
          <div style={spinnerStyle} />
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          <h2 style={{ margin: '16px 0 8px 0', fontSize: 20 }}>
            Connecting to match...
          </h2>
          <p style={{ color: '#666', margin: 0, fontSize: 14 }}>
            {spectatingState.player1Name} vs {spectatingState.player2Name}
          </p>
          <button onClick={stopSpectating} style={backButtonStyle}>
            ← Back to Overview
          </button>
        </div>
      </div>
    )
  }

  const formatPhase = (phase: string | null) => {
    if (!phase) return 'Unknown'
    return phase.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())
  }

  // Get stack (same for both players, take from player1)
  const stack = player1.stack || []

  return (
    <div style={containerStyle}>
      {/* Header with back button and game info */}
      <div style={headerStyle}>
        <button onClick={stopSpectating} style={backButtonStyle}>
          ← Back to Overview
        </button>
        <div style={{ textAlign: 'center', flex: 1 }}>
          <div style={{ color: '#888', fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            Spectating
          </div>
          <div style={{ color: '#4fc3f7', fontSize: 14, marginTop: 4 }}>
            {formatPhase(currentPhase ?? null)}
          </div>
        </div>
        <div style={{ width: 120 }} />
      </div>

      {/* Main content */}
      <div style={boardContainerStyle}>
        {/* Player 1 (top - opponent perspective) */}
        <PlayerBoard
          player={player1}
          isActive={activePlayerId === player1.playerId}
          hasPriority={priorityPlayerId === player1.playerId}
          isTop
          responsive={responsive}
        />

        {/* Center divider with life totals */}
        <div style={centerDividerStyle}>
          <LifeDisplay
            playerId={player1.playerId}
            life={player1.life}
            playerName={player1.playerName}
            isLeft
          />

          <div style={{ color: '#444', fontSize: 20, fontWeight: 600 }}>vs</div>

          <LifeDisplay
            playerId={player2.playerId}
            life={player2.life}
            playerName={player2.playerName}
          />
        </div>

        {/* Player 2 (bottom - player perspective) */}
        <PlayerBoard
          player={player2}
          isActive={activePlayerId === player2.playerId}
          hasPriority={priorityPlayerId === player2.playerId}
          isTop={false}
          responsive={responsive}
        />
      </div>

      {/* Stack display (floating left side) */}
      {stack.length > 0 && <StackDisplay stack={stack} />}

      {/* Life change animations */}
      {lifeAnimations.map((anim) => (
        <LifeChangeAnimation
          key={anim.id}
          animation={anim}
          onComplete={() => removeAnimation(anim.id)}
        />
      ))}

      {/* Targeting and combat arrows */}
      <SpectatorArrows player1={player1} player2={player2} combat={combat ?? null} />
    </div>
  )
}

interface LifeAnimation {
  id: string
  playerId: string
  amount: number
  isGain: boolean
  startTime: number
}

/**
 * Life change animation number.
 */
function LifeChangeAnimation({
  animation,
  onComplete,
}: {
  animation: LifeAnimation
  onComplete: () => void
}) {
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    const startDelay = Math.max(0, animation.startTime - Date.now())

    const startAnimation = () => {
      const startTime = Date.now()

      const animate = () => {
        const elapsed = Date.now() - startTime
        const newProgress = Math.min(1, elapsed / ANIMATION_DURATION)
        setProgress(newProgress)

        if (newProgress < 1) {
          requestAnimationFrame(animate)
        } else {
          setTimeout(onComplete, 50)
        }
      }

      requestAnimationFrame(animate)
    }

    const timeoutId = setTimeout(startAnimation, startDelay)
    return () => clearTimeout(timeoutId)
  }, [animation.startTime, onComplete])

  // Find the life display element
  const getPosition = () => {
    const el = document.querySelector(`[data-spectator-life="${animation.playerId}"]`)
    if (el) {
      const rect = el.getBoundingClientRect()
      return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 }
    }
    return { x: window.innerWidth / 2, y: window.innerHeight / 2 }
  }

  const { x, y } = getPosition()
  const offsetY = -60 * progress
  const opacity = progress < 0.2 ? progress * 5 : progress > 0.7 ? (1 - progress) * 3.33 : 1
  const scale = 1 + 0.3 * Math.sin(progress * Math.PI)

  const color = animation.isGain ? '#33ff33' : '#ff3333'
  const glowColor = animation.isGain ? 'rgba(0, 255, 0, 0.8)' : 'rgba(255, 0, 0, 0.8)'
  const prefix = animation.isGain ? '+' : '-'

  return (
    <div
      style={{
        position: 'fixed',
        left: x,
        top: y + offsetY,
        transform: `translate(-50%, -50%) scale(${scale})`,
        opacity,
        zIndex: 10001,
        pointerEvents: 'none',
        fontFamily: 'Impact, Arial Black, sans-serif',
        fontSize: 36,
        fontWeight: 'bold',
        color,
        textShadow: `0 0 10px ${glowColor}, 0 0 20px ${glowColor}, 2px 2px 4px rgba(0, 0, 0, 0.8)`,
      }}
    >
      {prefix}{animation.amount}
    </div>
  )
}

/**
 * Life display with player name.
 */
function LifeDisplay({
  playerId,
  life,
  playerName,
  isLeft = false,
}: {
  playerId: string
  life: number
  playerName: string
  isLeft?: boolean
}) {
  return (
    <div
      data-spectator-life={playerId}
      style={{
        display: 'flex',
        flexDirection: isLeft ? 'row' : 'row-reverse',
        alignItems: 'center',
        gap: 12,
      }}
    >
      <div
        style={{
          width: 56,
          height: 56,
          borderRadius: '50%',
          backgroundColor: life <= 5 ? '#3a1a1a' : '#1a2a3a',
          border: `3px solid ${life <= 5 ? '#ff4444' : '#3a6a9a'}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 24,
          fontWeight: 700,
          color: life <= 5 ? '#ff4444' : '#fff',
          boxShadow: life <= 5 ? '0 0 15px rgba(255, 68, 68, 0.4)' : 'none',
        }}
      >
        {life}
      </div>
      <div style={{ textAlign: isLeft ? 'left' : 'right' }}>
        <div style={{ fontSize: 16, fontWeight: 600, color: '#fff' }}>{playerName}</div>
      </div>
    </div>
  )
}

/**
 * Individual player board display with zones.
 */
function PlayerBoard({
  player,
  isActive,
  hasPriority,
  isTop,
  responsive,
}: {
  player: SpectatorPlayerState
  isActive: boolean
  hasPriority: boolean
  isTop: boolean
  responsive: ReturnType<typeof useResponsive>
}) {
  // Separate cards by type
  const lands = player.battlefield.filter((c) => c.cardTypes.includes('LAND'))
  const creatures = player.battlefield.filter((c) => c.cardTypes.includes('CREATURE'))
  const otherPermanents = player.battlefield.filter(
    (c) => !c.cardTypes.includes('LAND') && !c.cardTypes.includes('CREATURE')
  )

  return (
    <div style={{
      ...playerBoardStyle,
      borderColor: isActive ? '#4fc3f7' : '#222',
    }}>
      {/* Player info header */}
      <div style={playerInfoStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontWeight: 600, fontSize: 14, color: '#ccc' }}>
            {player.playerName}
          </span>
          {isActive && <span style={activeTagStyle}>Active</span>}
          {hasPriority && <span style={priorityTagStyle}>Priority</span>}
        </div>
        <div style={{ display: 'flex', gap: 16, color: '#666', fontSize: 12 }}>
          <span title="Hand">🃏 {player.handSize}</span>
          <span title="Library">📚 {player.librarySize}</span>
          <span title="Graveyard">💀 {player.graveyard.length}</span>
        </div>
      </div>

      {/* Battlefield zones */}
      <div style={battlefieldContainerStyle}>
        {isTop ? (
          // Top player: lands first (top), then creatures (bottom)
          <>
            <ZoneRow
              label="Lands"
              cards={lands}
              responsive={responsive}
              emptyText="No lands"
            />
            <div style={zoneDividerStyle} />
            <ZoneRow
              label="Creatures"
              cards={creatures}
              responsive={responsive}
              emptyText="No creatures"
            />
            {otherPermanents.length > 0 && (
              <>
                <div style={zoneDividerStyle} />
                <ZoneRow
                  label="Other"
                  cards={otherPermanents}
                  responsive={responsive}
                  emptyText=""
                />
              </>
            )}
          </>
        ) : (
          // Bottom player: creatures first (top), then lands (bottom)
          <>
            {otherPermanents.length > 0 && (
              <>
                <ZoneRow
                  label="Other"
                  cards={otherPermanents}
                  responsive={responsive}
                  emptyText=""
                />
                <div style={zoneDividerStyle} />
              </>
            )}
            <ZoneRow
              label="Creatures"
              cards={creatures}
              responsive={responsive}
              emptyText="No creatures"
            />
            <div style={zoneDividerStyle} />
            <ZoneRow
              label="Lands"
              cards={lands}
              responsive={responsive}
              emptyText="No lands"
            />
          </>
        )}
      </div>

      {/* Graveyard preview */}
      {player.graveyard.length > 0 && (
        <div style={graveyardPreviewStyle}>
          <span style={{ color: '#555', fontSize: 11, marginRight: 8 }}>Graveyard:</span>
          {player.graveyard.slice(-5).map((card, i) => (
            <span key={card.entityId} style={{ color: '#777', fontSize: 11 }}>
              {i > 0 ? ', ' : ''}{card.name}
            </span>
          ))}
          {player.graveyard.length > 5 && (
            <span style={{ color: '#555', fontSize: 11 }}> +{player.graveyard.length - 5} more</span>
          )}
        </div>
      )}
    </div>
  )
}

/**
 * A row of cards in a zone.
 */
function ZoneRow({
  label,
  cards,
  responsive,
  emptyText,
}: {
  label: string
  cards: readonly SpectatorCardInfo[]
  responsive: ReturnType<typeof useResponsive>
  emptyText: string
}) {
  if (cards.length === 0 && !emptyText) return null

  return (
    <div style={zoneRowStyle}>
      <div style={zoneLabelStyle}>{label}</div>
      <div style={cardRowStyle}>
        {cards.length === 0 ? (
          emptyText && <div style={{ color: '#333', fontSize: 11, padding: '8px 0' }}>{emptyText}</div>
        ) : (
          cards.map((card) => (
            <SpectatorCard key={card.entityId} card={card} responsive={responsive} />
          ))
        )}
      </div>
    </div>
  )
}

/**
 * Card display for spectator view.
 */
function SpectatorCard({
  card,
  responsive,
}: {
  card: SpectatorCardInfo
  responsive: ReturnType<typeof useResponsive>
}) {
  const cardWidth = responsive.isMobile ? 55 : 70
  const cardHeight = Math.round(cardWidth * 1.4)

  const handleImageError = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const img = e.currentTarget
    const fallbackUrl = getScryfallFallbackUrl(card.name, 'normal')
    if (!img.src.includes('api.scryfall.com')) {
      img.src = fallbackUrl
    }
  }

  return (
    <div
      data-spectator-card-id={card.entityId}
      style={{
        width: cardWidth,
        height: card.isTapped ? cardWidth : cardHeight,
        borderRadius: 4,
        overflow: 'hidden',
        transform: card.isTapped ? 'rotate(90deg)' : 'none',
        margin: card.isTapped ? `${(cardHeight - cardWidth) / 2}px 4px` : '0 2px',
        position: 'relative',
        flexShrink: 0,
        boxShadow: '0 2px 6px rgba(0,0,0,0.5)',
        border: card.isAttacking ? '2px solid #ff4444' : '1px solid #333',
      }}
      title={card.name}
    >
      {card.imageUri ? (
        <img
          src={getCardImageUrl(card.name, card.imageUri, 'small')}
          alt={card.name}
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          onError={handleImageError}
        />
      ) : (
        <div style={cardPlaceholderStyle}>
          <span style={{ fontSize: 8, color: '#888', textAlign: 'center', overflow: 'hidden' }}>
            {card.name}
          </span>
        </div>
      )}

      {/* P/T overlay for creatures */}
      {card.power !== null && card.toughness !== null && (
        <div style={ptOverlayStyle}>
          <span style={{ color: '#fff', fontWeight: 600 }}>
            {card.power}/{card.toughness - card.damage}
          </span>
          {card.damage > 0 && (
            <span style={{ color: '#ff4444', fontSize: 9, marginLeft: 2 }}>
              ({card.damage})
            </span>
          )}
        </div>
      )}

      {/* Tapped overlay */}
      {card.isTapped && <div style={tappedOverlayStyle} />}
    </div>
  )
}

/**
 * Stack display - shows spells/abilities waiting to resolve.
 */
function StackDisplay({
  stack,
}: {
  stack: readonly SpectatorCardInfo[]
}) {
  return (
    <div style={stackContainerStyle}>
      <div style={stackHeaderStyle}>Stack ({stack.length})</div>
      <div style={stackItemsStyle}>
        {stack.map((card, index) => (
          <div
            key={card.entityId}
            style={{
              ...stackItemStyle,
              zIndex: stack.length - index,
              transform: `translateY(${index * 4}px)`,
            }}
            title={card.name}
          >
            <img
              src={getCardImageUrl(card.name, card.imageUri, 'small')}
              alt={card.name}
              style={stackItemImageStyle}
              onError={(e) => {
                const img = e.currentTarget
                const fallback = getScryfallFallbackUrl(card.name, 'small')
                if (!img.src.includes('api.scryfall.com')) {
                  img.src = fallback
                }
              }}
            />
            <div style={{ color: '#ccc', fontSize: 10, marginTop: 2, maxWidth: 60, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', textAlign: 'center' }}>
              {card.name}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

/**
 * Arrows overlay for targeting and combat.
 */
function SpectatorArrows({
  player1,
  player2,
  combat,
}: {
  player1: SpectatorPlayerState | null
  player2: SpectatorPlayerState | null
  combat: SpectatorCombatState | null
}) {
  const [arrows, setArrows] = useState<ArrowData[]>([])

  useEffect(() => {
    if (!player1 || !player2) {
      setArrows([])
      return
    }

    const updateArrows = () => {
      const newArrows: ArrowData[] = []

      // Add targeting arrows (orange) for spells/abilities on stack
      for (const card of [...player1.stack, ...player2.stack]) {
        if (!card.targets || card.targets.length === 0) continue

        const sourcePos = getSpectatorCardCenter(card.entityId)
        if (!sourcePos) continue

        card.targets.forEach((target, i) => {
          let targetPos: Point | null = null

          switch (target.type) {
            case 'Player':
              targetPos = getSpectatorPlayerCenter(target.playerId)
              break
            case 'Permanent':
              targetPos = getSpectatorCardCenter(target.entityId)
              break
            case 'Spell':
              targetPos = getSpectatorCardCenter(target.spellEntityId)
              break
          }

          if (targetPos) {
            newArrows.push({
              key: `target-${card.entityId}-${i}`,
              start: sourcePos,
              end: targetPos,
              color: '#ff8800', // Orange for targeting
            })
          }
        })
      }

      // Add combat arrows (blue for attackers, red for blockers)
      if (combat) {
        for (const attacker of combat.attackers) {
          const attackerPos = getSpectatorCardCenter(attacker.creatureId)
          if (!attackerPos) continue

          // Arrow from attacker to defender (if no blockers)
          if (attacker.blockedBy.length === 0) {
            const defenderPos = getSpectatorPlayerCenter(combat.defendingPlayerId)
            if (defenderPos) {
              newArrows.push({
                key: `attack-${attacker.creatureId}`,
                start: attackerPos,
                end: defenderPos,
                color: '#4488ff', // Blue for attacking
              })
            }
          }

          // Arrows from blockers to attacker (red)
          for (const blockerId of attacker.blockedBy) {
            const blockerPos = getSpectatorCardCenter(blockerId)
            if (blockerPos) {
              newArrows.push({
                key: `block-${blockerId}-${attacker.creatureId}`,
                start: blockerPos,
                end: attackerPos,
                color: '#ff4444', // Red for blocking
              })
            }
          }
        }
      }

      setArrows(newArrows)
    }

    updateArrows()
    const interval = setInterval(updateArrows, 100)
    return () => clearInterval(interval)
  }, [player1, player2, combat])

  if (arrows.length === 0) return null

  return (
    <svg
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 1700,
      }}
    >
      {arrows.map(({ key, start, end, color }) => (
        <Arrow key={key} start={start} end={end} color={color} />
      ))}
    </svg>
  )
}

// ============================================================================
// Styles
// ============================================================================

const containerStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: '#0a0a12',
  display: 'flex',
  flexDirection: 'column',
  zIndex: 1500,
}

const headerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  padding: '10px 16px',
  borderBottom: '1px solid #1a1a25',
  backgroundColor: '#0d0d15',
  flexShrink: 0,
}

const backButtonStyle: React.CSSProperties = {
  padding: '8px 16px',
  fontSize: 13,
  backgroundColor: 'transparent',
  color: '#888',
  border: '1px solid #333',
  borderRadius: 6,
  cursor: 'pointer',
}

const boardContainerStyle: React.CSSProperties = {
  flex: 1,
  display: 'flex',
  flexDirection: 'column',
  padding: '8px 12px',
  gap: 6,
  overflow: 'hidden',
}

const playerBoardStyle: React.CSSProperties = {
  flex: 1,
  backgroundColor: '#0f0f18',
  borderRadius: 8,
  border: '2px solid #222',
  display: 'flex',
  flexDirection: 'column',
  overflow: 'hidden',
  transition: 'border-color 0.2s',
  minHeight: 0,
}

const playerInfoStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '8px 12px',
  backgroundColor: '#0a0a12',
  borderBottom: '1px solid #1a1a25',
  flexShrink: 0,
}

const activeTagStyle: React.CSSProperties = {
  fontSize: 9,
  fontWeight: 600,
  color: '#4fc3f7',
  backgroundColor: 'rgba(79, 195, 247, 0.15)',
  padding: '2px 6px',
  borderRadius: 4,
  textTransform: 'uppercase',
}

const priorityTagStyle: React.CSSProperties = {
  fontSize: 9,
  fontWeight: 600,
  color: '#ffc107',
  backgroundColor: 'rgba(255, 193, 7, 0.15)',
  padding: '2px 6px',
  borderRadius: 4,
  textTransform: 'uppercase',
}

const battlefieldContainerStyle: React.CSSProperties = {
  flex: 1,
  display: 'flex',
  flexDirection: 'column',
  padding: '4px 8px',
  overflow: 'auto',
  minHeight: 0,
}

const zoneRowStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 8,
  padding: '4px 0',
}

const zoneLabelStyle: React.CSSProperties = {
  color: '#444',
  fontSize: 10,
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  width: 60,
  flexShrink: 0,
}

const cardRowStyle: React.CSSProperties = {
  display: 'flex',
  flexWrap: 'wrap',
  gap: 4,
  alignItems: 'center',
}

const zoneDividerStyle: React.CSSProperties = {
  width: '100%',
  height: 1,
  backgroundColor: '#1a1a25',
  margin: '2px 0',
}

const centerDividerStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  gap: 24,
  padding: '8px 0',
  flexShrink: 0,
}

const graveyardPreviewStyle: React.CSSProperties = {
  padding: '6px 12px',
  backgroundColor: '#08080c',
  borderTop: '1px solid #1a1a25',
  overflow: 'hidden',
  whiteSpace: 'nowrap',
  textOverflow: 'ellipsis',
  flexShrink: 0,
}

const cardPlaceholderStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
  backgroundColor: '#1a1a2e',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: 4,
  boxSizing: 'border-box',
}

const ptOverlayStyle: React.CSSProperties = {
  position: 'absolute',
  bottom: 2,
  right: 2,
  backgroundColor: 'rgba(0, 0, 0, 0.85)',
  padding: '1px 4px',
  borderRadius: 3,
  fontSize: 10,
}

const tappedOverlayStyle: React.CSSProperties = {
  position: 'absolute',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(0, 0, 0, 0.3)',
}

const stackContainerStyle: React.CSSProperties = {
  position: 'fixed',
  left: 12,
  top: '50%',
  transform: 'translateY(-50%)',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  padding: '8px 10px',
  backgroundColor: 'rgba(100, 50, 150, 0.25)',
  borderRadius: 8,
  border: '1px solid rgba(150, 100, 200, 0.4)',
  zIndex: 1600,
  maxHeight: '50vh',
  overflowY: 'auto',
}

const stackHeaderStyle: React.CSSProperties = {
  color: '#b088d0',
  fontWeight: 600,
  fontSize: 11,
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  marginBottom: 6,
}

const stackItemsStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  position: 'relative',
}

const stackItemStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  position: 'relative',
}

const stackItemImageStyle: React.CSSProperties = {
  width: 50,
  height: 70,
  objectFit: 'cover',
  borderRadius: 3,
  boxShadow: '0 2px 6px rgba(0, 0, 0, 0.5)',
}

const spinnerStyle: React.CSSProperties = {
  width: 40,
  height: 40,
  border: '3px solid #333',
  borderTopColor: '#888',
  borderRadius: '50%',
  animation: 'spin 1s linear infinite',
  margin: '0 auto',
}
