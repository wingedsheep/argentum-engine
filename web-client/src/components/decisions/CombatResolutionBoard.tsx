import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type {
  CombatResolutionDecision,
  DamageEdge,
  EntityId,
} from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { Keyword } from '@/types/enums.ts'
import { GameCard } from '@/components/game/card/GameCard.tsx'
import { ResponsiveContext } from '@/components/game/board'

/**
 * The combat resolution board (CR 510 / 702.22). Shows only the damage the local player gets to
 * assign — the sources whose edges they own ({@link DamageEdge.editableBy}). Per CR 510.1c the
 * attacker assigns its creatures' damage and the defender assigns its blockers' damage as separate
 * steps, so the opponent's half isn't rendered (it's chosen on their own board). Each owned edge is
 * pre-filled with the engine's lethal-first default and adjusted with +/- steppers. The server is
 * authoritative — the board only clamps to `[0, maximum]` and the per-source power budget;
 * CR 510.1c / 702.19b legality is enforced on submit by the engine.
 */
export function CombatResolutionBoard({ decision }: { decision: CombatResolutionDecision }) {
  const submit = useGameStore((s) => s.submitCombatResolutionDecision)
  const gameState = useGameStore((s) => s.gameState)
  const playerId = useGameStore((s) => s.playerId)
  const hoverCard = useGameStore((s) => s.hoverCard)
  const responsive = useResponsive()

  // Only the edges this player may assign. Group them by source, in emission order.
  const myEdges = decision.edges.filter((e) => e.editableBy === playerId)
  const sourceIds: EntityId[] = []
  for (const edge of myEdges) {
    if (!sourceIds.includes(edge.sourceId)) sourceIds.push(edge.sourceId)
  }

  const edgesBySource = (sourceId: EntityId) =>
    myEdges.filter((e) => e.sourceId === sourceId)

  const sourcePower = (sourceId: EntityId) =>
    decision.edges.filter((e) => e.sourceId === sourceId).reduce((m, e) => Math.max(m, e.maximum), 0)

  const sourceTotal = (sourceId: EntityId) =>
    decision.edges.filter((e) => e.sourceId === sourceId).reduce((sum, e) => sum + (amounts[e.id] ?? 0), 0)

  // When several of the player's sources pile onto one creature (gang blocks, bipartite blocks,
  // attacking bands) that creature appears as a target under multiple source rows. A single edge's
  // amount then doesn't tell the whole story, so we surface the *combined* damage all the player's
  // edges are assigning to that target and whether it collectively reaches lethal.
  const targetEdgeCount = (targetId: EntityId) =>
    myEdges.reduce((n, e) => (e.targetId === targetId ? n + 1 : n), 0)
  const combinedOnTarget = (targetId: EntityId) =>
    myEdges.reduce((sum, e) => (e.targetId === targetId ? sum + (amounts[e.id] ?? 0) : sum), 0)

  const ownsAnyEditable = decision.edges.some((e) => e.editableBy === playerId && e.maximum > 0)

  // Banding (CR 702.22). A creature has banding if its live keywords say so — neither the attacker
  // nor blocker payloads carry it, so read it off the masked client state.
  const hasBanding = (id: EntityId): boolean =>
    gameState?.cards[id]?.keywords?.includes(Keyword.BANDING) ?? false
  // A source's edges are "banding-inverted" for this player when banding handed them the division:
  // the order constraint is lifted (CR 702.22j/k) and they're a real combat edge, not a trample
  // drain. On defense this is how the defender ends up assigning an attacker's damage.
  const sourceBandingInverted = (sourceId: EntityId): boolean =>
    myEdges.some((e) => e.sourceId === sourceId && !e.orderConstrained && !e.isTrampleDrain)
  const bandingActive =
    decision.attackers.some((a) => a.bandId != null) ||
    [...decision.attackers, ...decision.blockers].some((c) => hasBanding(c.id))

  // Initial / reset assignment. Starts from the engine's lethal-first defaults, but for any source
  // whose division banding handed to this player (order lifted) we instead dump the source's whole
  // power onto its FIRST target and zero the rest — the canonical banding line: sponge everything
  // onto one creature. This covers both directions symmetrically:
  //   - Defense (CR 702.22j): you block with a banding creature, so you assign the attacker's
  //     damage → all onto your first blocker (and, as a bonus, nothing tramples through).
  //   - Offense (CR 702.22k): you attack with a band, so you assign each blocker's damage → all
  //     onto the first band member (the sponge); every blocker funnels onto it since blocking one
  //     band member blocks the whole band (CR 702.22h).
  // The player can still redistribute with the steppers.
  const defaultAmounts = (): Record<string, number> => {
    const result: Record<string, number> = Object.fromEntries(decision.edges.map((e) => [e.id, e.amount]))
    for (const sourceId of sourceIds) {
      if (!sourceBandingInverted(sourceId)) continue
      const sourceEdges = myEdges.filter((e) => e.sourceId === sourceId)
      const firstCombatEdge = sourceEdges.find((e) => !e.isTrampleDrain)
      if (!firstCombatEdge) continue
      const power = sourcePower(sourceId)
      for (const e of sourceEdges) {
        result[e.id] = e.id === firstCombatEdge.id ? Math.min(power, e.maximum) : 0
      }
    }
    return result
  }

  const [amounts, setAmounts] = useState<Record<string, number>>(defaultAmounts)

  const adjust = (edge: DamageEdge, delta: number) => {
    setAmounts((prev) => {
      const current = prev[edge.id] ?? 0
      const next = current + delta
      if (next < 0 || next > edge.maximum) return prev
      // Per-source budget: never assign more than the source's power across its edges.
      if (delta > 0 && sourceTotal(edge.sourceId) - current + next > sourcePower(edge.sourceId)) return prev
      return { ...prev, [edge.id]: next }
    })
  }

  const handleReset = () => setAmounts(defaultAmounts())

  const handleConfirm = () => {
    // Submit only the edges this player owns; the engine merges them with the rest.
    const owned = decision.edges.filter((e) => e.editableBy === playerId)
    submit(owned.map((e) => ({ edgeId: e.id, amount: amounts[e.id] ?? 0 })))
  }

  // ── Display helpers ─────────────────────────────────────────────────────
  const cardName = (id: EntityId): string => {
    const a = decision.attackers.find((x) => x.id === id)
    if (a) return a.name
    const b = decision.blockers.find((x) => x.id === id)
    if (b) return b.name
    const d = decision.defenders.find((x) => x.id === id)
    if (d) return d.name
    return gameState?.cards[id]?.name ?? 'Unknown'
  }
  const cardImage = (id: EntityId): string | null | undefined => gameState?.cards[id]?.imageUri
  const isPlayerTarget = (id: EntityId): boolean =>
    decision.defenders.some((d) => d.id === id && d.kind === 'PLAYER')

  const sourceLabel = (id: EntityId): string => {
    const a = decision.attackers.find((x) => x.id === id)
    if (a) return `${a.name}  ${a.power}/${a.toughness}`
    const b = decision.blockers.find((x) => x.id === id)
    if (b) return `${b.name}  ${b.power}/${b.toughness}`
    return cardName(id)
  }

  const cardW = responsive.isMobile ? 84 : 116
  const cardH = Math.round(cardW * 1.4)

  const targetVisual = (id: EntityId) => {
    if (isPlayerTarget(id)) {
      const defender = decision.defenders.find((d) => d.id === id)
      return (
        <div
          style={{
            width: cardW, height: cardH, borderRadius: 8, backgroundColor: '#1a1a2e',
            border: '2px solid #333', display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center', gap: 6,
          }}
        >
          <span style={{ color: '#888', fontSize: 26 }}>&#9823;</span>
          <span style={{ color: 'white', fontSize: responsive.fontSize.small }}>{defender?.name ?? 'Player'}</span>
          {defender?.lifeOrLoyaltyOrDefense != null && (
            <span style={{ color: '#888', fontSize: responsive.fontSize.small }}>
              Life: {defender.lifeOrLoyaltyOrDefense}
            </span>
          )}
        </div>
      )
    }
    // Reuse the battlefield card so combatants show their live P/T, counters and keyword-ability
    // icons exactly as they appear on the board, plus the built-in hover preview. Tap rotation is
    // suppressed so attackers stay upright in the modal's row layout instead of lying sideways.
    const clientCard = gameState?.cards[id]
    if (clientCard) {
      return (
        <GameCard
          card={clientCard}
          battlefield
          interactive={false}
          suppressTapRotation
          overrideWidth={cardW}
        />
      )
    }
    // Fallback: raw image if the card isn't in the masked client state for some reason.
    return (
      <img
        src={getCardImageUrl(cardName(id), cardImage(id))}
        alt={cardName(id)}
        onMouseEnter={(e) => hoverCard(id, { x: e.clientX, y: e.clientY })}
        onMouseLeave={() => hoverCard(null)}
        style={{ width: cardW, height: cardH, objectFit: 'cover', borderRadius: 8, border: '2px solid #333' }}
        onError={(e) => { e.currentTarget.style.display = 'none' }}
      />
    )
  }

  const stepBtn = (label: string, onClick: () => void, enabled: boolean, color: string) => (
    <button
      onClick={onClick}
      disabled={!enabled}
      style={{
        width: 30, height: 30, borderRadius: 6, border: 'none',
        backgroundColor: enabled ? color : '#333', color: enabled ? 'white' : '#666',
        fontSize: 18, fontWeight: 'bold', cursor: enabled ? 'pointer' : 'not-allowed',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}
    >
      {label}
    </button>
  )

  // Small purple tag marking a creature that has banding (CR 702.22), so it's obvious in the board
  // which creature is conferring the damage-division benefit.
  const bandingPill = () => (
    <span
      style={{
        padding: '0 5px', borderRadius: 4, backgroundColor: '#7e22ce', color: 'white',
        fontSize: 10, fontWeight: 700, lineHeight: '15px', whiteSpace: 'nowrap',
      }}
    >
      banding
    </span>
  )

  const edgeRow = (edge: DamageEdge) => {
    const amount = amounts[edge.id] ?? 0
    const isDrain = edge.isTrampleDrain
    const atLethal = !isDrain && edge.lethal > 0 && amount >= edge.lethal
    const arrow = isDrain ? 'trample →' : '→'
    // Combined-damage hint: only meaningful when this creature is targeted by more than one of
    // the player's edges (otherwise the per-edge amount already is the total).
    const sharedTarget = !isDrain && targetEdgeCount(edge.targetId) > 1
    const combined = sharedTarget ? combinedOnTarget(edge.targetId) : 0
    const combinedAtLethal = sharedTarget && edge.lethal > 0 && combined >= edge.lethal
    return (
      <div key={edge.id} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <span style={{ color: '#888', fontSize: responsive.fontSize.small, minWidth: 56, textAlign: 'right' }}>
          {arrow}
        </span>
        {targetVisual(edge.targetId)}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 5, maxWidth: 150 }}>
            <span style={{ color: 'white', fontSize: responsive.fontSize.small, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {cardName(edge.targetId)}
            </span>
            {hasBanding(edge.targetId) && bandingPill()}
          </div>
          {!isDrain && (
            <span style={{ color: atLethal ? '#4ade80' : '#f59e0b', fontSize: responsive.fontSize.small }}>
              lethal: {edge.lethal}{atLethal ? ' ✓' : ''}{edge.orderConstrained ? '' : ' (any order)'}
            </span>
          )}
          {sharedTarget && (
            <span style={{ color: combinedAtLethal ? '#4ade80' : '#f59e0b', fontSize: responsive.fontSize.small, fontWeight: 600 }}>
              combined: {combined}{edge.lethal > 0 ? ` / ${edge.lethal}` : ''}{combinedAtLethal ? ' ✓' : ''}
            </span>
          )}
          {isDrain && (
            <span style={{ color: '#60a5fa', fontSize: responsive.fontSize.small }}>overflow</span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginLeft: 'auto' }}>
          {stepBtn('-', () => adjust(edge, -1), amount > 0, '#dc2626')}
          <span style={{ color: 'white', fontSize: responsive.fontSize.large, fontWeight: 700, minWidth: 28, textAlign: 'center' }}>
            {amount}
          </span>
          {stepBtn('+', () => adjust(edge, +1), amount < edge.maximum && sourceTotal(edge.sourceId) < sourcePower(edge.sourceId), '#16a34a')}
        </div>
      </div>
    )
  }

  return (
    // The combat board mounts outside GameBoard's ResponsiveContext provider, so provide it here
    // for the GameCard instances rendered by targetVisual (they call useResponsiveContext()).
    <ResponsiveContext.Provider value={responsive}>
    <div
      style={{
        position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.92)',
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        gap: responsive.isMobile ? 12 : 18, padding: responsive.containerPadding,
        overflowY: 'auto', pointerEvents: 'auto', zIndex: 1000,
      }}
    >
      <div style={{ textAlign: 'center', marginTop: 8 }}>
        <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 18 : 24, fontWeight: 600 }}>
          Assign Combat Damage
        </h2>
        <p style={{ color: '#aaa', margin: '6px 0 0', fontSize: responsive.fontSize.normal }}>
          {decision.firstStrike ? 'First Strike Damage' : 'Combat Damage'}
        </p>
        {bandingActive && (
          <p style={{ color: '#a855f7', margin: '4px 0 0', fontSize: responsive.fontSize.small }}>
            Banding (CR 702.22): damage division is inverted for some edges.
          </p>
        )}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14, width: '100%', maxWidth: 720 }}>
        {sourceIds.map((sourceId) => (
          <div
            key={sourceId}
            style={{
              display: 'flex', gap: 14, alignItems: 'center', padding: 12,
              backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: 10, border: '1px solid #2a2a3a',
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
              {targetVisual(sourceId)}
              <span style={{ color: 'white', fontSize: responsive.fontSize.small, fontWeight: 600, maxWidth: cardW + 30, textAlign: 'center' }}>
                {sourceLabel(sourceId)}
              </span>
              {hasBanding(sourceId) && bandingPill()}
              {sourceBandingInverted(sourceId) && !hasBanding(sourceId) && (
                <span style={{ color: '#c084fc', fontSize: responsive.fontSize.small, maxWidth: cardW + 30, textAlign: 'center', lineHeight: 1.2 }}>
                  Banding: you assign this creature's damage
                </span>
              )}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, flex: 1 }}>
              {edgesBySource(sourceId).map(edgeRow)}
            </div>
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 14, marginBottom: 16 }}>
        {ownsAnyEditable && (
          <button
            onClick={handleReset}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '14px 28px', fontSize: responsive.fontSize.normal,
              backgroundColor: '#4b5563', color: 'white', border: 'none', borderRadius: 8,
              cursor: 'pointer', fontWeight: 500,
            }}
          >
            Reset
          </button>
        )}
        <button
          onClick={handleConfirm}
          style={{
            padding: responsive.isMobile ? '10px 32px' : '14px 44px', fontSize: responsive.fontSize.large,
            backgroundColor: '#16a34a', color: 'white', border: 'none', borderRadius: 8,
            cursor: 'pointer', fontWeight: 600,
          }}
        >
          Confirm Damage
        </button>
      </div>
    </div>
    </ResponsiveContext.Provider>
  )
}
