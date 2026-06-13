import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { selectGameState, selectViewingPlayerId, useViewedOpponent } from '@/store/selectors.ts'
import { seatColor } from '@/styles/seatColors'
import type { EntityId } from '@/types'
import { Step, ZoneType } from '@/types'

interface Point {
  x: number
  y: number
}

interface ArrowProps {
  start: Point
  end: Point
  color: string
  dashed?: boolean
}

interface ArrowData {
  start: Point
  end: Point
  blockerId: EntityId
  /** 1-based damage order position (1 = first to receive damage). Undefined if not yet ordered. */
  damageOrder?: number
  /** Damage assigned to this blocker. Undefined if not yet assigned. */
  damageAmount?: number
}

interface AttackerArrowData {
  start: Point
  end: Point
  attackerId: EntityId
  /** Defender seat color in multiplayer; defaults to combat red. */
  color?: string
}

/**
 * Multiplayer: attacks against an off-screen defender bundle into one arrow from
 * the attacker group's centroid to that defender's rail chip, with a count badge.
 */
interface BundledArrowData {
  start: Point
  end: Point
  color: string
  count: number
  defenderId: EntityId
}

interface AttackIndicatorData {
  x: number
  y: number
  direction: 'up' | 'down'
  attackerId: EntityId
  /** Defender seat color in multiplayer; defaults to combat red. */
  color?: string
}

/**
 * SVG arrow component with curved path and arrowhead.
 */
function Arrow({ start, end, color, dashed = false }: ArrowProps) {
  // Calculate control point for quadratic bezier (arc upward)
  const midX = (start.x + end.x) / 2
  const midY = (start.y + end.y) / 2
  const dx = end.x - start.x
  const dy = end.y - start.y
  const distance = Math.sqrt(dx * dx + dy * dy)

  // Arc height based on distance, curving upward (negative Y in SVG)
  const arcHeight = Math.min(distance * 0.2, 60)
  const controlX = midX
  const controlY = midY - arcHeight

  // Path for quadratic bezier curve
  const pathD = `M ${start.x} ${start.y} Q ${controlX} ${controlY} ${end.x} ${end.y}`

  // Calculate arrowhead direction from the curve tangent at the end
  // For quadratic bezier, tangent at t=1 is: 2(P2 - P1) where P1 is control, P2 is end
  const tangentX = end.x - controlX
  const tangentY = end.y - controlY
  const tangentLen = Math.sqrt(tangentX * tangentX + tangentY * tangentY)
  const normX = tangentX / tangentLen
  const normY = tangentY / tangentLen

  // Arrowhead size
  const arrowSize = 12
  const arrowAngle = Math.PI / 6 // 30 degrees

  // Calculate arrowhead points
  const cos = Math.cos(arrowAngle)
  const sin = Math.sin(arrowAngle)

  const arrow1X = end.x - arrowSize * (normX * cos + normY * sin)
  const arrow1Y = end.y - arrowSize * (normY * cos - normX * sin)
  const arrow2X = end.x - arrowSize * (normX * cos - normY * sin)
  const arrow2Y = end.y - arrowSize * (normY * cos + normX * sin)

  const arrowheadD = `M ${end.x} ${end.y} L ${arrow1X} ${arrow1Y} M ${end.x} ${end.y} L ${arrow2X} ${arrow2Y}`

  return (
    <g>
      {/* Glow effect */}
      <path
        d={pathD}
        fill="none"
        stroke={color}
        strokeWidth={8}
        strokeOpacity={0.3}
        strokeLinecap="round"
      />
      {/* Main path */}
      <path
        d={pathD}
        fill="none"
        stroke={color}
        strokeWidth={3}
        strokeOpacity={0.9}
        strokeLinecap="round"
        strokeDasharray={dashed ? '8,4' : undefined}
      />
      {/* Arrowhead */}
      <path
        d={arrowheadD}
        fill="none"
        stroke={color}
        strokeWidth={3}
        strokeOpacity={0.9}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </g>
  )
}

/**
 * Get the center position of a card element by its ID.
 */
function getCardCenter(cardId: EntityId): Point | null {
  const element = document.querySelector(`[data-card-id="${cardId}"]`)
  if (!element) return null

  const rect = element.getBoundingClientRect()
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
}

/**
 * Get the center position of a player's life display.
 */
function getPlayerLifeCenter(playerId: EntityId): Point | null {
  const element = document.querySelector(`[data-life-id="${playerId}"]`)
  if (!element) return null

  const rect = element.getBoundingClientRect()
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
}

/**
 * Get the edge positions of a card element for attack indicators.
 */
function getCardEdgeCenter(cardId: EntityId): { topCenter: Point; bottomCenter: Point; centerY: number } | null {
  const element = document.querySelector(`[data-card-id="${cardId}"]`)
  if (!element) return null

  const rect = element.getBoundingClientRect()
  return {
    topCenter: { x: rect.left + rect.width / 2, y: rect.top },
    bottomCenter: { x: rect.left + rect.width / 2, y: rect.bottom },
    centerY: rect.top + rect.height / 2,
  }
}

/**
 * Animated attack direction indicator — triangles streaming toward the defender.
 * Combat red in 2-player; the defender's seat color once assigned in multiplayer
 * (the per-attacker "who is this hitting" chevron).
 */
function AttackIndicator({ x, y, direction, color = '#ff4444' }: { x: number; y: number; direction: 'up' | 'down'; color?: string }) {
  const halfWidth = 10
  const triHeight = 12
  const dir = direction === 'up' ? -1 : 1
  const baseY = y + dir * 6
  const tipY = baseY + dir * triHeight
  const points = `${x - halfWidth},${baseY} ${x},${tipY} ${x + halfWidth},${baseY}`

  return (
    <g>
      {/* Glow */}
      <polygon
        points={points}
        fill={color}
        fillOpacity={0.15}
        stroke={color}
        strokeWidth={5}
        strokeOpacity={0.2}
        strokeLinejoin="round"
      />
      {/* Filled triangle */}
      <polygon
        points={points}
        fill={color}
        fillOpacity={0.85}
        stroke={color}
        strokeWidth={1}
        strokeOpacity={0.6}
        strokeLinejoin="round"
      />
    </g>
  )
}

/** Is this viewport point horizontally on-screen (i.e., not on a slid-away board)? */
function isOnScreen(p: Point): boolean {
  return p.x >= -4 && p.x <= window.innerWidth + 4
}

/**
 * Combat arrows overlay - draws arrows between blockers and attackers.
 *
 * Shows arrows in three scenarios:
 * 1. During declare blockers phase (for the defending player) - uses local combatState
 * 2. For the attacking player during declare blockers - uses opponentBlockerAssignments (real-time sync)
 * 3. After blockers are declared (for both players) - uses server-sent gameState.combat
 */
// Combat steps where blocker arrows should be visible
const COMBAT_STEPS = new Set([
  Step.BEGIN_COMBAT,
  Step.DECLARE_ATTACKERS,
  Step.DECLARE_BLOCKERS,
  Step.FIRST_STRIKE_COMBAT_DAMAGE,
  Step.COMBAT_DAMAGE,
  Step.END_COMBAT,
])

export function CombatArrows() {
  const combatState = useGameStore((state) => state.combatState)
  const gameState = useGameStore(selectGameState)
  const gameStateCombat = gameState?.combat
  const currentStep = gameState?.currentStep
  const cards = gameState?.cards
  const players = gameState?.players
  const viewingPlayerId = useGameStore(selectViewingPlayerId)
  const viewedOpponent = useViewedOpponent()
  const viewedOpponentId = viewedOpponent?.playerId ?? null
  const isMulti = (players?.length ?? 0) > 2
  const opponentAttackerTargets = useGameStore((state) => state.opponentAttackerTargets)
  const opponentBlockerAssignments = useGameStore((state) => state.opponentBlockerAssignments)
  const draggingBlockerId = useGameStore((state) => state.draggingBlockerId)
  const draggingAttackerId = useGameStore((state) => state.draggingAttackerId)
  const isSpectating = useGameStore((state) => state.spectatingState !== null)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const [mousePos, setMousePos] = useState<Point | null>(null)
  const [arrows, setArrows] = useState<ArrowData[]>([])
  const [attackerArrows, setAttackerArrows] = useState<AttackerArrowData[]>([])
  const [bundledArrows, setBundledArrows] = useState<BundledArrowData[]>([])
  const [attackIndicators, setAttackIndicators] = useState<AttackIndicatorData[]>([])

  // Check if we're still in combat phase
  const isInCombatPhase = currentStep && COMBAT_STEPS.has(currentStep as Step)

  // Check if we're selecting damage order (hide blocker arrows during this UI)
  const isSelectingDamageOrder = pendingDecision?.type === 'OrderObjectsDecision' &&
    pendingDecision?.context?.phase === 'COMBAT'

  // Hide all arrows during full-screen overlay decisions (e.g., ChooseColorDecision)
  // But keep arrows visible for combat trigger YesNo decisions (e.g., Gustcloak Savior)
  const hasOverlayDecision = pendingDecision != null &&
    pendingDecision.type !== 'ChooseTargetsDecision' &&
    !(pendingDecision.type === 'SelectCardsDecision' && pendingDecision.useTargetingUI) &&
    !(pendingDecision.type === 'YesNoDecision' && pendingDecision.context.triggeringEntityId)

  // Track mouse/touch position during drag (blocker or attacker)
  useEffect(() => {
    if (!draggingBlockerId && !draggingAttackerId) {
      setMousePos(null)
      return
    }

    const handleMouseMove = (e: MouseEvent) => {
      setMousePos({ x: e.clientX, y: e.clientY })
    }

    const handleTouchMove = (e: TouchEvent) => {
      const touch = e.touches[0]
      if (touch) {
        setMousePos({ x: touch.clientX, y: touch.clientY })
      }
    }

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('touchmove', handleTouchMove)
    return () => {
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('touchmove', handleTouchMove)
    }
  }, [draggingBlockerId, draggingAttackerId])

  // Check if we're in active declare blockers mode (local state)
  const isDeclaringBlockers = combatState?.mode === 'declareBlockers'

  // Update arrow positions when assignments change
  useEffect(() => {
    // Determine which blocker assignments to use:
    // 1. If we're actively declaring blockers, use local combatState
    // 2. If we're the attacker and opponent is assigning blockers, use opponentBlockerAssignments
    // 3. Otherwise, use server-sent combat data (if blockers have been declared)

    const updateArrows = () => {
      const newArrows: ArrowData[] = []

      // Seat-identity color for a defending player ("whose seat is this hitting").
      // Attacks against the viewing player stay combat red, as do all 2-player arrows.
      const seatColorOf = (defenderId: EntityId): string => {
        if (!isMulti || !players || defenderId === viewingPlayerId) return '#ff4444'
        const idx = players.findIndex((p) => p.playerId === defenderId)
        return idx >= 0 ? seatColor(idx).base : '#ff4444'
      }

      // Card anchor that survives the multiplayer board strip: a card on a
      // slid-away opponent board resolves to its controller's rail chip instead
      // of an off-viewport point (which would paint arrows across the screen edge).
      const resolvePos = (cardId: EntityId): Point | null => {
        const p = getCardCenter(cardId)
        if (!p) return null
        if (!isMulti || isOnScreen(p)) return p
        const controller = cards?.[cardId]?.controllerId
        if (!controller) return p
        return getPlayerLifeCenter(controller) ?? p
      }

      // Skip blocker arrows during damage order selection (that UI shows blockers separately)
      if (isSelectingDamageOrder) {
        setArrows([])
        // Still compute attacker arrows below
      } else if (isDeclaringBlockers && combatState) {
        // Use local blocker assignments (real-time feedback during declaration)
        for (const [blockerIdStr, attackerIds] of Object.entries(combatState.blockerAssignments)) {
          const blockerId = blockerIdStr as EntityId
          const blockerPos = resolvePos(blockerId)

          for (const attackerId of attackerIds) {
            const attackerPos = resolvePos(attackerId)

            if (blockerPos && attackerPos) {
              newArrows.push({
                start: blockerPos,
                end: attackerPos,
                blockerId,
              })
            }
          }
        }
      } else if (opponentBlockerAssignments && Object.keys(opponentBlockerAssignments).length > 0 && isInCombatPhase) {
        // Use opponent's real-time blocker assignments (for attacking player, only during combat)
        for (const [blockerIdStr, attackerIds] of Object.entries(opponentBlockerAssignments)) {
          const blockerId = blockerIdStr as EntityId
          const blockerCard = cards?.[blockerId]
          const blockerOnBattlefield = blockerCard?.zone?.zoneType === ZoneType.BATTLEFIELD

          if (!blockerOnBattlefield) continue

          for (const attackerId of attackerIds) {
            const attackerCard = cards?.[attackerId]
            const attackerOnBattlefield = attackerCard?.zone?.zoneType === ZoneType.BATTLEFIELD

            if (!attackerOnBattlefield) continue

            const blockerPos = resolvePos(blockerId)
            const attackerPos = resolvePos(attackerId)

            if (blockerPos && attackerPos) {
              newArrows.push({
                start: blockerPos,
                end: attackerPos,
                blockerId,
              })
            }
          }
        }
      } else if (gameStateCombat && gameStateCombat.blockers.length > 0 && isInCombatPhase) {
        // Build lookup maps for damage order and assignments from attacker data
        const blockerDamageOrder = new Map<EntityId, number>()
        const blockerDamageAmount = new Map<EntityId, number>()
        for (const attacker of gameStateCombat.attackers) {
          if (attacker.damageAssignmentOrder) {
            attacker.damageAssignmentOrder.forEach((blockerId, index) => {
              blockerDamageOrder.set(blockerId, index + 1)
            })
          }
          if (attacker.damageAssignments) {
            for (const [targetId, amount] of Object.entries(attacker.damageAssignments)) {
              blockerDamageAmount.set(targetId as EntityId, amount)
            }
          }
        }

        // Use server-sent combat data (shows to both players after blockers declared, only during combat)
        for (const blocker of gameStateCombat.blockers) {
          // Only draw arrows for creatures still on the battlefield
          // (creatures that died during combat damage should not have arrows)
          const blockerCard = cards?.[blocker.creatureId]
          const attackerCard = cards?.[blocker.blockingAttacker]
          const blockerOnBattlefield = blockerCard?.zone?.zoneType === ZoneType.BATTLEFIELD
          const attackerOnBattlefield = attackerCard?.zone?.zoneType === ZoneType.BATTLEFIELD

          if (!blockerOnBattlefield || !attackerOnBattlefield) {
            continue
          }

          const blockerPos = resolvePos(blocker.creatureId)
          const attackerPos = resolvePos(blocker.blockingAttacker)

          if (blockerPos && attackerPos) {
            const arrow: ArrowData = {
              start: blockerPos,
              end: attackerPos,
              blockerId: blocker.creatureId,
            }
            const order = blockerDamageOrder.get(blocker.creatureId)
            if (order != null) arrow.damageOrder = order
            const amount = blockerDamageAmount.get(blocker.creatureId)
            if (amount != null) arrow.damageAmount = amount
            newArrows.push(arrow)
          }
        }
      }

      setArrows(newArrows)

      // Compute attacker arrows (visible to all players and spectators during combat).
      // 2-player: only when planeswalkers exist — red triangle indicators suffice
      // otherwise. Multiplayer: always — "whose spell, at whom" needs the arrows.
      // Attacks against a defender whose board is slid away bundle into one arrow
      // per defender, from the attacker group's centroid to their rail chip.
      const newAttackerArrows: AttackerArrowData[] = []
      const bundleAcc = new Map<EntityId, { starts: Point[]; end: Point; count: number }>()

      const pushAttackArrow = (attackerId: EntityId, targetId: EntityId) => {
        const attackerPos = getCardCenter(attackerId)
        if (!attackerPos) return
        // The target is a planeswalker (a card) or a player; the defending player
        // is the planeswalker's controller (CR 802.2a) or the player themself.
        const targetCard = cards?.[targetId]
        const defenderId = targetCard ? targetCard.controllerId : targetId
        if (isMulti && defenderId !== viewingPlayerId && defenderId !== viewedOpponentId) {
          // Off-screen defender → bundle to their rail chip.
          const chip = getPlayerLifeCenter(defenderId)
          if (!chip) return
          const acc = bundleAcc.get(defenderId) ?? { starts: [], end: chip, count: 0 }
          acc.starts.push(attackerPos)
          acc.count++
          bundleAcc.set(defenderId, acc)
          return
        }
        const cardPos = targetCard ? getCardCenter(targetId) : null
        const targetPos = (cardPos && (!isMulti || isOnScreen(cardPos)) ? cardPos : null)
          ?? getPlayerLifeCenter(defenderId)
        if (!targetPos) return
        newAttackerArrows.push({
          start: attackerPos,
          end: targetPos,
          attackerId,
          color: seatColorOf(defenderId),
        })
      }

      const hasPlaneswalkerOnBattlefield = cards && Object.values(cards).some(
        (card) => card.zone?.zoneType === ZoneType.BATTLEFIELD && card.cardTypes.includes('PLANESWALKER'),
      )
      if ((hasPlaneswalkerOnBattlefield || isMulti) && gameStateCombat && gameStateCombat.attackers.length > 0) {
        for (const attacker of gameStateCombat.attackers) {
          // Check if attacker is still on battlefield
          const attackerCard = cards?.[attacker.creatureId]
          if (attackerCard?.zone?.zoneType !== ZoneType.BATTLEFIELD) {
            continue
          }
          pushAttackArrow(
            attacker.creatureId,
            attacker.attackingTarget.type === 'Player'
              ? attacker.attackingTarget.playerId
              : attacker.attackingTarget.permanentId,
          )
        }
      }

      // Compute local attacker→target arrows during declare attackers phase (always show — explicit user intent)
      if (combatState?.mode === 'declareAttackers' && combatState.attackerTargets) {
        for (const [attackerIdStr, targetId] of Object.entries(combatState.attackerTargets)) {
          const attackerId = attackerIdStr as EntityId
          // Only show if this attacker is still selected
          if (!combatState.selectedAttackers.includes(attackerId)) continue
          pushAttackArrow(attackerId, targetId)
        }
      }

      // Compute opponent's real-time attacker target arrows (for defending player and spectators, always show)
      if (opponentAttackerTargets && opponentAttackerTargets.selectedAttackers.length > 0 && !gameStateCombat) {
        for (const [attackerIdStr, targetId] of Object.entries(opponentAttackerTargets.attackerTargets)) {
          const attackerId = attackerIdStr as EntityId
          if (!opponentAttackerTargets.selectedAttackers.includes(attackerId)) continue
          pushAttackArrow(attackerId, targetId)
        }
      }
      setAttackerArrows(newAttackerArrows)
      setBundledArrows(
        Array.from(bundleAcc.entries()).map(([defenderId, acc]) => ({
          defenderId,
          count: acc.count,
          end: acc.end,
          start: {
            x: acc.starts.reduce((s, p) => s + p.x, 0) / acc.starts.length,
            y: acc.starts.reduce((s, p) => s + p.y, 0) / acc.starts.length,
          },
          color: seatColorOf(defenderId),
        })),
      )

      // Compute attack direction indicators (red triangles)
      const newIndicators: AttackIndicatorData[] = []
      const viewportCenterY = window.innerHeight / 2

      // Seat color for an indicator given the attack's target id (player or planeswalker).
      const indicatorColorFor = (targetId: EntityId | undefined): string => {
        if (!targetId) return '#ff4444'
        const targetCard = cards?.[targetId]
        return seatColorOf(targetCard ? targetCard.controllerId : targetId)
      }

      if (combatState?.mode === 'declareAttackers' && combatState.selectedAttackers.length > 0) {
        // During declare attackers: show for locally selected attackers. 2-player skips
        // attackers with explicit (planeswalker) targets — they have arrows; multiplayer
        // keeps them as the seat-colored "who is this hitting" chevron on the card.
        for (const attackerId of combatState.selectedAttackers) {
          const targetId = combatState.attackerTargets[attackerId]
          if (targetId && !isMulti) continue
          const edge = getCardEdgeCenter(attackerId)
          if (edge) {
            const direction = edge.centerY > viewportCenterY ? 'up' : 'down'
            const pos = direction === 'up' ? edge.topCenter : edge.bottomCenter
            newIndicators.push({ x: pos.x, y: pos.y, direction, attackerId, color: indicatorColorFor(targetId) })
          }
        }
      } else if (opponentAttackerTargets && opponentAttackerTargets.selectedAttackers.length > 0 && !gameStateCombat) {
        // Opponent's real-time attacker selections (for defending player and spectators)
        for (const attackerId of opponentAttackerTargets.selectedAttackers) {
          const targetId = opponentAttackerTargets.attackerTargets[attackerId]
          if (targetId && !isMulti) continue
          const edge = getCardEdgeCenter(attackerId)
          if (edge) {
            const direction = edge.centerY > viewportCenterY ? 'up' : 'down'
            const pos = direction === 'up' ? edge.topCenter : edge.bottomCenter
            newIndicators.push({ x: pos.x, y: pos.y, direction, attackerId, color: indicatorColorFor(targetId) })
          }
        }
      } else if (gameStateCombat && gameStateCombat.attackers.length > 0) {
        // During other combat phases: show for all confirmed attackers
        for (const attacker of gameStateCombat.attackers) {
          const attackerCard = cards?.[attacker.creatureId]
          if (attackerCard?.zone?.zoneType !== ZoneType.BATTLEFIELD) continue

          const edge = getCardEdgeCenter(attacker.creatureId)
          if (edge) {
            const direction = edge.centerY > viewportCenterY ? 'up' : 'down'
            const pos = direction === 'up' ? edge.topCenter : edge.bottomCenter
            newIndicators.push({
              x: pos.x,
              y: pos.y,
              direction,
              attackerId: attacker.creatureId,
              color: indicatorColorFor(
                attacker.attackingTarget.type === 'Player'
                  ? attacker.attackingTarget.playerId
                  : attacker.attackingTarget.permanentId,
              ),
            })
          }
        }
      }
      setAttackIndicators(newIndicators)
    }

    // Update immediately and on animation frames for smooth updates
    updateArrows()
    const interval = setInterval(updateArrows, 100)
    return () => clearInterval(interval)
  }, [combatState, gameStateCombat, opponentAttackerTargets, opponentBlockerAssignments, isDeclaringBlockers, isInCombatPhase, cards, isSpectating, isSelectingDamageOrder, players, isMulti, viewedOpponentId, viewingPlayerId])

  // Don't render during full-screen overlay decisions
  if (hasOverlayDecision) {
    return null
  }

  // Don't render if no arrows to show (only show during combat phase)
  const hasBlockers = isDeclaringBlockers ||
    (opponentBlockerAssignments && Object.keys(opponentBlockerAssignments).length > 0 && isInCombatPhase) ||
    (gameStateCombat && gameStateCombat.blockers.length > 0 && isInCombatPhase)
  const hasAttackers = gameStateCombat && gameStateCombat.attackers.length > 0
  const hasSelectedAttackers = combatState?.mode === 'declareAttackers' && combatState.selectedAttackers.length > 0
  const hasOpponentAttackers = opponentAttackerTargets && opponentAttackerTargets.selectedAttackers.length > 0
  if (!hasBlockers && !hasAttackers && !hasSelectedAttackers && !hasOpponentAttackers && !draggingBlockerId && !draggingAttackerId) {
    return null
  }

  // Get dragging arrow if applicable (blocker or attacker)
  const draggingArrow = (() => {
    if (draggingBlockerId && mousePos) {
      const blockerPos = getCardCenter(draggingBlockerId)
      if (blockerPos) return { start: blockerPos, end: mousePos, color: '#88ccff' }
    }
    if (draggingAttackerId && mousePos) {
      const attackerPos = getCardCenter(draggingAttackerId)
      if (attackerPos) return { start: attackerPos, end: mousePos, color: '#ff8888' }
    }
    return null
  })()

  return (
    <svg
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 2000, // Above spectator container (1500)
      }}
    >
      {/* Attack direction indicators (triangles pointing toward the defender,
          seat-colored once assigned in multiplayer) */}
      {attackIndicators.map(({ x, y, direction, attackerId, color }) => (
        <AttackIndicator
          key={`indicator-${attackerId}`}
          x={x}
          y={y}
          direction={direction}
          {...(color ? { color } : {})}
        />
      ))}

      {/* Attacker arrows (attackers to the defending player / planeswalker) */}
      {attackerArrows.map(({ start, end, attackerId, color }) => (
        <Arrow
          key={`attacker-${attackerId}`}
          start={start}
          end={end}
          color={color ?? '#ff4444'}
        />
      ))}

      {/* Bundled attack arrows to off-screen defenders' rail chips, with a
          creature-count badge near the chip end */}
      {bundledArrows.map(({ start, end, color, count, defenderId }) => {
        // Badge on the bezier at t=0.7 (closer to the chip)
        const midX = (start.x + end.x) / 2
        const midY = (start.y + end.y) / 2
        const dist = Math.hypot(end.x - start.x, end.y - start.y)
        const controlY = midY - Math.min(dist * 0.2, 60)
        const t = 0.7
        const mt = 1 - t
        const badgeX = mt * mt * start.x + 2 * mt * t * midX + t * t * end.x
        const badgeY = mt * mt * start.y + 2 * mt * t * controlY + t * t * end.y
        return (
          <g key={`bundle-${defenderId}`}>
            <Arrow start={start} end={end} color={color} />
            <circle cx={badgeX} cy={badgeY} r={12} fill="#000000" fillOpacity={0.85} stroke={color} strokeWidth={1.5} />
            <text
              x={badgeX}
              y={badgeY + 4.5}
              textAnchor="middle"
              fill={color}
              fontSize={13}
              fontWeight={700}
              fontFamily="system-ui, sans-serif"
              style={{ pointerEvents: 'none' }}
            >
              {count}
            </text>
          </g>
        )
      })}

      {/* Blocker assignments */}
      {arrows.map(({ start, end, blockerId, damageOrder, damageAmount }) => (
        <g key={`blocker-${blockerId}`}>
          <Arrow
            start={start}
            end={end}
            color="#4488ff"
          />
          {/* Damage order and assignment badges near the blocker (creature receiving damage) */}
          {(damageOrder != null || damageAmount != null) && (() => {
            const midX = (start.x + end.x) / 2
            const midY = (start.y + end.y) / 2
            const dx = end.x - start.x
            const dy = end.y - start.y
            const dist = Math.sqrt(dx * dx + dy * dy)
            const arcHeight = Math.min(dist * 0.2, 60)
            const controlX = midX
            const controlY = midY - arcHeight
            // Position on the bezier curve at t=0.25 (closer to blocker/start)
            const t = 0.25
            const mt = 1 - t
            const badgeX = mt * mt * start.x + 2 * mt * t * controlX + t * t * end.x
            const badgeY = mt * mt * start.y + 2 * mt * t * controlY + t * t * end.y

            if (damageAmount != null) {
              // Show damage assignment: "order: amount" or just "amount"
              const label = damageOrder != null ? `#${damageOrder}: ${damageAmount} dmg` : `${damageAmount} dmg`
              const textWidth = label.length * 7 + 12
              return (
                <g>
                  <rect
                    x={badgeX - textWidth / 2}
                    y={badgeY - 11}
                    width={textWidth}
                    height={22}
                    rx={11}
                    fill="#000000"
                    fillOpacity={0.85}
                    stroke="#dc2626"
                    strokeWidth={1.5}
                  />
                  <text
                    x={badgeX}
                    y={badgeY + 4}
                    textAnchor="middle"
                    fill="#f87171"
                    fontSize={12}
                    fontWeight={700}
                    fontFamily="system-ui, sans-serif"
                    style={{ pointerEvents: 'none' }}
                  >
                    {label}
                  </text>
                </g>
              )
            } else if (damageOrder != null) {
              // Show just the damage order number
              return (
                <g>
                  <circle
                    cx={badgeX}
                    cy={badgeY}
                    r={13}
                    fill="#000000"
                    fillOpacity={0.85}
                    stroke="#f59e0b"
                    strokeWidth={1.5}
                  />
                  <text
                    x={badgeX}
                    y={badgeY + 5}
                    textAnchor="middle"
                    fill="#fbbf24"
                    fontSize={14}
                    fontWeight={700}
                    fontFamily="system-ui, sans-serif"
                    style={{ pointerEvents: 'none' }}
                  >
                    {damageOrder}
                  </text>
                </g>
              )
            }
            return null
          })()}
        </g>
      ))}

      {/* Dragging arrow (blocker or attacker) */}
      {draggingArrow && (
        <Arrow
          start={draggingArrow.start}
          end={draggingArrow.end}
          color={draggingArrow.color}
          dashed
        />
      )}
    </svg>
  )
}
