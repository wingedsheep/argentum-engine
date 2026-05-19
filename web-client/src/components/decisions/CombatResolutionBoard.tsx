import { useEffect, useMemo, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type {
  CombatResolutionDecision,
  DamageEdge,
  EntityId,
  ResolutionAttacker,
  ResolutionBlocker,
  ResolutionDefender,
} from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { bandColorFor } from '../game/board/styles'

/**
 * Bipartite-graph combat damage board (replaces CombatDamagePlanModal when the
 * engine emits a [CombatResolutionDecision]). One screen, one logical decision,
 * one click in the happy path. See docs/plans/combat-resolution-board.md §6.
 *
 * The board renders three tiers — attackers, blockers, drain targets (players /
 * planeswalkers / battles) — and one ± chip per edge the local player owns.
 * Non-editable edges are shown read-only so both halves of a banding split are
 * legible on the same board. Lethal-first is enforced as a per-edge minimum;
 * trample drain edges only unlock once preceding edges hit lethal.
 */
export function CombatResolutionBoard({ decision }: { decision: CombatResolutionDecision }) {
  const submit = useGameStore((s) => s.submitCombatResolutionDecision)
  const playerId = useGameStore((s) => s.playerId)
  const gameState = useGameStore((s) => s.gameState)
  const hoverCard = useGameStore((s) => s.hoverCard)
  const responsive = useResponsive()
  const [minimized, setMinimized] = useState(false)

  // Mutable per-edge amounts. Initialised from server-computed defaults.
  const [draft, setDraft] = useState<Record<string, number>>(() => {
    const initial: Record<string, number> = {}
    for (const edge of decision.edges) initial[edge.id] = edge.amount
    return initial
  })

  // If the decision changes (re-pause for next chooser in the banding queue),
  // re-seed the draft from the new defaults.
  useEffect(() => {
    const next: Record<string, number> = {}
    for (const edge of decision.edges) next[edge.id] = edge.amount
    setDraft(next)
  }, [decision.id, decision.edges])

  // ─── derived indices ─────────────────────────────────────────────────────
  const attackerById = useMemo(
    () => new Map(decision.attackers.map((a) => [a.id, a])),
    [decision.attackers],
  )
  const blockerById = useMemo(
    () => new Map(decision.blockers.map((b) => [b.id, b])),
    [decision.blockers],
  )
  const defenderById = useMemo(
    () => new Map(decision.defenders.map((d) => [d.id, d])),
    [decision.defenders],
  )
  const edgesBySource = useMemo(() => {
    const m = new Map<EntityId, DamageEdge[]>()
    for (const edge of decision.edges) {
      const list = m.get(edge.sourceId) ?? []
      list.push(edge)
      m.set(edge.sourceId, list)
    }
    for (const list of m.values()) list.sort((a, b) => a.unlockOrder - b.unlockOrder)
    return m
  }, [decision.edges])

  const myEditableEdges = useMemo(
    () => decision.edges.filter((e) => e.editableBy === playerId),
    [decision.edges, playerId],
  )

  // Band index → colour slot (stable across the board).
  const bandIndexFor = (a: ResolutionAttacker): number => {
    if (!a.bandId) return -1
    const seen: string[] = []
    for (const att of decision.attackers) {
      if (att.bandId && !seen.includes(att.bandId)) seen.push(att.bandId)
    }
    return seen.indexOf(a.bandId)
  }
  const hasBanding = decision.attackers.some((a) => a.bandId)

  // ─── per-edge constraints ────────────────────────────────────────────────
  // Drain edges (trample / planeswalker drain) unlock only once every preceding
  // edge from the same source hits its lethal threshold.
  const isDrainUnlocked = (edge: DamageEdge): boolean => {
    if (!edge.isTrampleDrain) return true
    const siblings = edgesBySource.get(edge.sourceId) ?? []
    for (const other of siblings) {
      if (other.id === edge.id) continue
      if (other.unlockOrder >= edge.unlockOrder) continue
      const lethal = other.lethalThreshold
      if (lethal == null) continue
      if ((draft[other.id] ?? 0) < lethal) return false
    }
    return true
  }

  // Lethal-first: dragging an edge below its minimum would drop a preceding
  // blocker under lethal. The engine pre-computes `minimum` for this case.
  const canDecrement = (edge: DamageEdge): boolean => (draft[edge.id] ?? 0) > edge.minimum
  const canIncrement = (edge: DamageEdge): boolean => {
    const current = draft[edge.id] ?? 0
    if (current >= edge.maximum) return false
    if (!isDrainUnlocked(edge)) return false
    const sourcePower = sourcePowerFor(edge.sourceId)
    if (sourcePower == null) return true
    const allocated = sumFromSource(edge.sourceId)
    return allocated < sourcePower
  }

  function sourcePowerFor(sourceId: EntityId): number | null {
    return attackerById.get(sourceId)?.power ?? blockerById.get(sourceId)?.power ?? null
  }
  function sumFromSource(sourceId: EntityId): number {
    let sum = 0
    for (const edge of edgesBySource.get(sourceId) ?? []) sum += draft[edge.id] ?? 0
    return sum
  }

  // ─── editability + auto-confirm ──────────────────────────────────────────
  const hasMeaningfulEditableEdge = useMemo(() => {
    // If the local player owns no edges OR every editable edge has min===max
    // (server already baked the only legal value), there is nothing to choose
    // and the board can auto-confirm.
    if (myEditableEdges.length === 0) return false
    return myEditableEdges.some((e) => e.maximum > e.minimum)
  }, [myEditableEdges])

  const allSourcesFullyAssigned = useMemo(() => {
    // For every source that has any editable edge, check the local sum matches
    // its power. Sources with no editable edges are server-locked (no-op).
    const editableSources = new Set<EntityId>()
    for (const e of myEditableEdges) editableSources.add(e.sourceId)
    for (const sourceId of editableSources) {
      const power = sourcePowerFor(sourceId)
      if (power == null) continue
      if (sumFromSource(sourceId) !== power) return false
    }
    return true
  }, [myEditableEdges, draft, attackerById, blockerById])

  // Auto-confirm the trivial case (Scenario I Board 2: regular damage after a
  // first-strike attacker pruned the dangerous blocker; nothing left to do).
  useEffect(() => {
    if (hasMeaningfulEditableEdge) return
    if (!allSourcesFullyAssigned) return
    const t = window.setTimeout(() => {
      submit({
        edges: myEditableEdges.map((e) => ({ edgeId: e.id, amount: draft[e.id] ?? e.amount })),
      })
    }, 400)
    return () => window.clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decision.id, hasMeaningfulEditableEdge, allSourcesFullyAssigned])

  // ─── handlers ────────────────────────────────────────────────────────────
  const adjust = (edge: DamageEdge, delta: number) => {
    if (edge.editableBy !== playerId) return
    setDraft((prev) => {
      const current = prev[edge.id] ?? 0
      const next = Math.max(edge.minimum, Math.min(edge.maximum, current + delta))
      if (next === current) return prev
      return { ...prev, [edge.id]: next }
    })
  }
  const reset = () => {
    const initial: Record<string, number> = {}
    for (const edge of decision.edges) initial[edge.id] = edge.amount
    setDraft(initial)
  }
  const handleConfirm = () => {
    if (!allSourcesFullyAssigned) return
    submit({
      edges: myEditableEdges.map((e) => ({ edgeId: e.id, amount: draft[e.id] ?? e.amount })),
    })
  }

  // ─── early returns ───────────────────────────────────────────────────────
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 1000,
          padding: '8px 20px',
          fontSize: 15,
          fontWeight: 600,
          backgroundColor: '#dc2626',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          whiteSpace: 'nowrap',
          pointerEvents: 'auto',
        }}
      >
        Return to Combat Resolution
      </button>
    )
  }

  // ─── render ──────────────────────────────────────────────────────────────
  const isCompact = responsive.isMobile || decision.attackers.length >= 3
  const thumbWidth = isCompact ? 64 : 92
  const thumbHeight = Math.round(thumbWidth * 1.4)

  const waitingForOther = myEditableEdges.length === 0 && decision.coChooserId != null

  // Compact 1v1 strip: a single unblocked-or-once-blocked attacker with no
  // trample fan-out is the most common case and doesn't need the full overlay.
  // The strip lives at the bottom of the screen so the battlefield stays
  // visible behind it.
  const onlyAttacker = decision.attackers[0]
  const compactCase =
    !hasBanding &&
    decision.attackers.length === 1 &&
    decision.blockers.length <= 1 &&
    !!onlyAttacker &&
    !onlyAttacker.hasTrample &&
    !onlyAttacker.hasTrampleOverPlaneswalkers
  if (compactCase && onlyAttacker) {
    const onlyBlocker = decision.blockers[0]
    return (
      <CompactDamageStrip
        attackerName={onlyAttacker.name}
        attackerPower={onlyAttacker.power}
        blockerName={onlyBlocker?.name}
        blockerPower={onlyBlocker?.power}
        editable={hasMeaningfulEditableEdge}
        waitingForOther={waitingForOther}
        onConfirm={handleConfirm}
      />
    )
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: responsive.isMobile ? 10 : 14,
        padding: responsive.containerPadding,
        paddingTop: responsive.isMobile ? 14 : 24,
        pointerEvents: 'auto',
        zIndex: 1000,
        overflowY: 'auto',
      }}
    >
      {/* Step + banding header */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
        <StepChip firstStrike={decision.firstStrike} />
        {hasBanding && <BandingHeaderChip />}
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 18 : 22,
            fontWeight: 600,
          }}
        >
          {waitingForOther ? 'Waiting on opponent…' : 'Assign Combat Damage'}
        </h2>
      </div>

      {/* Attacker rows */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
          width: '100%',
          maxWidth: 1400,
        }}
      >
        {decision.attackers.map((attacker) => (
          <AttackerRow
            key={attacker.id}
            attacker={attacker}
            edges={edgesBySource.get(attacker.id) ?? []}
            draft={draft}
            playerId={playerId}
            adjust={adjust}
            attackerById={attackerById}
            blockerById={blockerById}
            defenderById={defenderById}
            bandIdx={bandIndexFor(attacker)}
            thumbWidth={thumbWidth}
            thumbHeight={thumbHeight}
            sumFromSource={sumFromSource}
            isDrainUnlocked={isDrainUnlocked}
            canIncrement={canIncrement}
            canDecrement={canDecrement}
            hoverCard={hoverCard}
            gameStateCards={gameState?.cards}
          />
        ))}
      </div>

      {/* Blocker → attacker edges (multi-blocker / bipartite). Only render when
          a blocker assigns damage to >1 attacker; otherwise it's redundant. */}
      {decision.blockers
        .filter((b) => (edgesBySource.get(b.id) ?? []).length >= 2)
        .map((blocker) => (
          <BlockerRow
            key={blocker.id}
            blocker={blocker}
            edges={edgesBySource.get(blocker.id) ?? []}
            draft={draft}
            playerId={playerId}
            adjust={adjust}
            attackerById={attackerById}
            thumbWidth={thumbWidth}
            thumbHeight={thumbHeight}
            sumFromSource={sumFromSource}
            canIncrement={canIncrement}
            canDecrement={canDecrement}
            hoverCard={hoverCard}
            gameStateCards={gameState?.cards}
          />
        ))}

      {/* Controls */}
      <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 6 }}>
        <button
          onClick={reset}
          style={controlBtn('#374151', 'white')}
        >
          Reset to default
        </button>
        <button
          onClick={() => setMinimized(true)}
          style={controlBtn('#1f2937', '#aaa', '1px solid #444')}
        >
          Minimize
        </button>
        <button
          onClick={handleConfirm}
          disabled={!allSourcesFullyAssigned || waitingForOther}
          style={{
            ...controlBtn(
              allSourcesFullyAssigned && !waitingForOther ? '#dc2626' : '#3d2020',
              'white',
            ),
            padding: '12px 28px',
            fontSize: 15,
            fontWeight: 600,
            cursor: allSourcesFullyAssigned && !waitingForOther ? 'pointer' : 'not-allowed',
            opacity: allSourcesFullyAssigned && !waitingForOther ? 1 : 0.55,
          }}
        >
          {waitingForOther ? 'Waiting…' : 'Confirm Damage'}
        </button>
      </div>
    </div>
  )
}

// ─── sub-components ────────────────────────────────────────────────────────

/**
 * Compact strip for the trivial 1-attacker × ≤1-blocker non-trample case
 * (§8 Q3). Bottom-center bar instead of full overlay so the battlefield stays
 * visible. Auto-confirms when nothing is meaningfully editable.
 */
function CompactDamageStrip({
  attackerName,
  attackerPower,
  blockerName,
  blockerPower,
  editable,
  waitingForOther,
  onConfirm,
}: {
  attackerName: string
  attackerPower: number
  blockerName: string | undefined
  blockerPower: number | undefined
  editable: boolean
  waitingForOther: boolean
  onConfirm: () => void
}) {
  return (
    <div
      style={{
        position: 'fixed',
        bottom: 24,
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 1000,
        background: 'rgba(20, 20, 30, 0.95)',
        border: '1px solid #444',
        borderRadius: 10,
        padding: '10px 16px',
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        boxShadow: '0 4px 16px rgba(0,0,0,0.6)',
        pointerEvents: 'auto',
      }}
    >
      <span style={{ color: 'white', fontSize: 13 }}>
        <strong style={{ color: '#ff8a8a' }}>{attackerName}</strong>
        {' '}
        <span style={{ color: '#fbbf24' }}>({attackerPower})</span>
      </span>
      <span style={{ color: '#888', fontSize: 18 }}>↔</span>
      <span style={{ color: 'white', fontSize: 13 }}>
        {blockerName ? (
          <>
            <strong style={{ color: '#90caf9' }}>{blockerName}</strong>
            {' '}
            <span style={{ color: '#fbbf24' }}>({blockerPower ?? 0})</span>
          </>
        ) : (
          <span style={{ color: '#888' }}>unblocked</span>
        )}
      </span>
      {editable ? (
        <button
          onClick={onConfirm}
          disabled={waitingForOther}
          style={{
            marginLeft: 6,
            padding: '8px 18px',
            backgroundColor: waitingForOther ? '#3d2020' : '#dc2626',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: waitingForOther ? 'not-allowed' : 'pointer',
            fontSize: 13,
            fontWeight: 600,
          }}
        >
          {waitingForOther ? 'Waiting…' : 'Confirm'}
        </button>
      ) : (
        <span style={{ color: '#4ade80', fontSize: 11, fontStyle: 'italic' }}>
          resolving…
        </span>
      )}
    </div>
  )
}

function StepChip({ firstStrike }: { firstStrike: boolean }) {
  return (
    <div
      style={{
        background: firstStrike ? '#f59e0b' : '#1f2937',
        color: firstStrike ? '#1a1a2e' : '#aaa',
        border: '1px solid #444',
        borderRadius: 999,
        padding: '4px 12px',
        fontSize: 12,
        fontWeight: 600,
        letterSpacing: 0.4,
      }}
    >
      {firstStrike ? 'First Strike Damage' : 'Regular Damage'}
    </div>
  )
}

function BandingHeaderChip() {
  return (
    <div
      style={{
        background: 'rgba(180, 100, 220, 0.18)',
        color: '#e9d5ff',
        border: '1px solid #a855f7',
        borderRadius: 6,
        padding: '4px 10px',
        fontSize: 11,
        fontWeight: 500,
        maxWidth: 720,
        textAlign: 'center',
      }}
    >
      Banding (CR 702.22j/k): some damage edges are assigned by the other player.
    </div>
  )
}

interface AttackerRowProps {
  attacker: ResolutionAttacker
  edges: DamageEdge[]
  draft: Record<string, number>
  playerId: EntityId | null
  adjust: (edge: DamageEdge, delta: number) => void
  attackerById: Map<EntityId, ResolutionAttacker>
  blockerById: Map<EntityId, ResolutionBlocker>
  defenderById: Map<EntityId, ResolutionDefender>
  bandIdx: number
  thumbWidth: number
  thumbHeight: number
  sumFromSource: (id: EntityId) => number
  isDrainUnlocked: (edge: DamageEdge) => boolean
  canIncrement: (edge: DamageEdge) => boolean
  canDecrement: (edge: DamageEdge) => boolean
  hoverCard: (id: EntityId | null, pos?: { x: number; y: number }) => void
  gameStateCards: Record<EntityId, { name: string; imageUri?: string | null }> | undefined
}

function AttackerRow(props: AttackerRowProps) {
  const {
    attacker, edges, draft, playerId, adjust, blockerById, defenderById,
    bandIdx, thumbWidth, thumbHeight, sumFromSource, isDrainUnlocked,
    canIncrement, canDecrement, hoverCard, gameStateCards,
  } = props
  const band = bandIdx >= 0 ? bandColorFor(bandIdx) : null
  const remaining = attacker.power - sumFromSource(attacker.id)
  const card = gameStateCards?.[attacker.id]

  return (
    <div
      style={{
        display: 'flex',
        gap: 12,
        alignItems: 'center',
        padding: 10,
        background: attacker.dealsDamageThisStep
          ? 'rgba(20, 20, 30, 0.7)'
          : 'rgba(20, 20, 30, 0.3)',
        border: band ? `2px solid ${band.border}` : '1px solid #333',
        borderRadius: 8,
        boxShadow: band ? `0 0 16px ${band.glow}` : 'none',
        opacity: attacker.dealsDamageThisStep ? 1 : 0.55,
      }}
    >
      <SourceThumbnail
        id={attacker.id}
        name={attacker.name}
        imageUri={card?.imageUri}
        thumbWidth={thumbWidth}
        thumbHeight={thumbHeight}
        borderColor="#ff4444"
        glowColor="rgba(255, 68, 68, 0.5)"
        hoverCard={hoverCard}
        chips={[
          band ? <BandChip key="band" idx={bandIdx} band={band} /> : null,
          <span key="power" style={{ color: '#fbbf24', fontSize: 11 }}>{attacker.power} pwr</span>,
          attacker.hasTrample ? <KeywordChip key="t" color="#f59e0b">trample</KeywordChip> : null,
          attacker.hasTrampleOverPlaneswalkers ? <KeywordChip key="tpw" color="#f59e0b">trample-PW</KeywordChip> : null,
          attacker.hasDeathtouch ? <KeywordChip key="dt" color="#a855f7">deathtouch</KeywordChip> : null,
          attacker.hasFirstStrike ? <KeywordChip key="fs" color="#fde68a">first strike</KeywordChip> : null,
          attacker.hasDoubleStrike ? <KeywordChip key="ds" color="#fde68a">double strike</KeywordChip> : null,
          attacker.markedDamage > 0 ? (
            <MarkedDamageChip key="md" amount={attacker.markedDamage} />
          ) : null,
        ].filter((x): x is React.ReactElement => x != null)}
        remainingLabel={
          remaining === 0
            ? '✓ done'
            : remaining > 0
              ? `${remaining} left`
              : `over by ${-remaining}`
        }
        remainingColor={remaining === 0 ? '#4ade80' : '#f87171'}
      />

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', flex: 1 }}>
        {edges.map((edge) => {
          const blk = blockerById.get(edge.targetId)
          const def = defenderById.get(edge.targetId)
          const amount = draft[edge.id] ?? 0
          const lethal = edge.lethalThreshold
          const isLethal = lethal != null && amount >= lethal
          const editable = edge.editableBy === playerId
          const unlocked = isDrainUnlocked(edge)
          const isDrain = edge.isTrampleDrain
          return (
            <EdgeCell
              key={edge.id}
              targetId={edge.targetId}
              label={blk?.name ?? def?.name ?? 'target'}
              imageUri={blk ? gameStateCards?.[blk.id]?.imageUri : null}
              isDefender={!!def}
              defenderKind={def?.kind}
              defenderValue={def?.lifeOrLoyaltyOrDefense ?? null}
              amount={amount}
              minimum={edge.minimum}
              maximum={edge.maximum}
              isLethal={isLethal}
              isDrain={isDrain}
              unlocked={unlocked}
              editable={editable}
              canDecrement={editable && unlocked && canDecrement(edge)}
              canIncrement={editable && unlocked && canIncrement(edge)}
              lethalThreshold={lethal}
              onAdjust={(d) => adjust(edge, d)}
              thumbWidth={thumbWidth}
              thumbHeight={thumbHeight}
              hoverCard={hoverCard}
            />
          )
        })}
      </div>
    </div>
  )
}

interface BlockerRowProps {
  blocker: ResolutionBlocker
  edges: DamageEdge[]
  draft: Record<string, number>
  playerId: EntityId | null
  adjust: (edge: DamageEdge, delta: number) => void
  attackerById: Map<EntityId, ResolutionAttacker>
  thumbWidth: number
  thumbHeight: number
  sumFromSource: (id: EntityId) => number
  canIncrement: (edge: DamageEdge) => boolean
  canDecrement: (edge: DamageEdge) => boolean
  hoverCard: (id: EntityId | null, pos?: { x: number; y: number }) => void
  gameStateCards: Record<EntityId, { name: string; imageUri?: string | null }> | undefined
}

function BlockerRow(props: BlockerRowProps) {
  const {
    blocker, edges, draft, playerId, adjust, attackerById, thumbWidth,
    thumbHeight, sumFromSource, canIncrement, canDecrement, hoverCard, gameStateCards,
  } = props
  const remaining = blocker.power - sumFromSource(blocker.id)
  const card = gameStateCards?.[blocker.id]

  return (
    <div
      style={{
        display: 'flex',
        gap: 12,
        alignItems: 'center',
        padding: 10,
        background: blocker.dealsDamageThisStep
          ? 'rgba(30, 25, 40, 0.7)'
          : 'rgba(30, 25, 40, 0.3)',
        border: '1px solid #4b5563',
        borderRadius: 8,
        opacity: blocker.dealsDamageThisStep ? 1 : 0.55,
        maxWidth: 1400,
        width: '100%',
      }}
    >
      <SourceThumbnail
        id={blocker.id}
        name={blocker.name}
        imageUri={card?.imageUri}
        thumbWidth={thumbWidth}
        thumbHeight={thumbHeight}
        borderColor="#60a5fa"
        glowColor="rgba(96, 165, 250, 0.5)"
        hoverCard={hoverCard}
        chips={[
          <span key="power" style={{ color: '#fbbf24', fontSize: 11 }}>{blocker.power} pwr</span>,
          blocker.hasDeathtouch ? <KeywordChip key="dt" color="#a855f7">deathtouch</KeywordChip> : null,
          blocker.markedDamage > 0 ? <MarkedDamageChip key="md" amount={blocker.markedDamage} /> : null,
        ].filter((x): x is React.ReactElement => x != null)}
        remainingLabel={
          remaining === 0
            ? '✓ done'
            : remaining > 0
              ? `${remaining} left`
              : `over by ${-remaining}`
        }
        remainingColor={remaining === 0 ? '#4ade80' : '#f87171'}
      />

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', flex: 1 }}>
        {edges.map((edge) => {
          const atk = attackerById.get(edge.targetId)
          const amount = draft[edge.id] ?? 0
          const lethal = edge.lethalThreshold
          const isLethal = lethal != null && amount >= lethal
          const editable = edge.editableBy === playerId
          return (
            <EdgeCell
              key={edge.id}
              targetId={edge.targetId}
              label={atk?.name ?? 'attacker'}
              imageUri={gameStateCards?.[edge.targetId]?.imageUri}
              isDefender={false}
              defenderKind={undefined}
              defenderValue={null}
              amount={amount}
              minimum={edge.minimum}
              maximum={edge.maximum}
              isLethal={isLethal}
              isDrain={false}
              unlocked={true}
              editable={editable}
              canDecrement={editable && canDecrement(edge)}
              canIncrement={editable && canIncrement(edge)}
              lethalThreshold={lethal}
              onAdjust={(d) => adjust(edge, d)}
              thumbWidth={thumbWidth}
              thumbHeight={thumbHeight}
              hoverCard={hoverCard}
            />
          )
        })}
      </div>
    </div>
  )
}

interface SourceThumbnailProps {
  id: EntityId
  name: string
  imageUri: string | null | undefined
  thumbWidth: number
  thumbHeight: number
  borderColor: string
  glowColor: string
  hoverCard: (id: EntityId | null, pos?: { x: number; y: number }) => void
  chips: React.ReactElement[]
  remainingLabel: string
  remainingColor: string
}

function SourceThumbnail(props: SourceThumbnailProps) {
  const { id, name, imageUri, thumbWidth, thumbHeight, borderColor, glowColor, hoverCard, chips, remainingLabel, remainingColor } = props
  return (
    <div
      onMouseEnter={(e) => hoverCard(id, { x: e.clientX, y: e.clientY })}
      onMouseLeave={() => hoverCard(null)}
      style={{
        flexShrink: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        minWidth: thumbWidth + 16,
      }}
    >
      <div
        style={{
          width: thumbWidth,
          height: thumbHeight,
          borderRadius: 6,
          overflow: 'hidden',
          border: `2px solid ${borderColor}`,
          boxShadow: `0 0 12px ${glowColor}`,
        }}
      >
        <img
          src={getCardImageUrl(name, imageUri)}
          alt={name}
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
      </div>
      <div style={{ color: 'white', fontSize: 11, fontWeight: 600, textAlign: 'center', maxWidth: thumbWidth + 16 }}>
        {name}
      </div>
      <div style={{ display: 'flex', gap: 4, alignItems: 'center', flexWrap: 'wrap', justifyContent: 'center', maxWidth: thumbWidth + 24 }}>
        {chips}
      </div>
      <div style={{ color: remainingColor, fontSize: 11, fontWeight: 600 }}>
        {remainingLabel}
      </div>
    </div>
  )
}

interface EdgeCellProps {
  targetId: EntityId
  label: string
  imageUri: string | null | undefined
  isDefender: boolean
  defenderKind: 'PLAYER' | 'PLANESWALKER' | 'BATTLE' | 'ATTACKER' | 'BLOCKER' | undefined
  defenderValue: number | null
  amount: number
  minimum: number
  maximum: number
  isLethal: boolean
  isDrain: boolean
  unlocked: boolean
  editable: boolean
  canDecrement: boolean
  canIncrement: boolean
  lethalThreshold: number | null | undefined
  onAdjust: (delta: number) => void
  thumbWidth: number
  thumbHeight: number
  hoverCard: (id: EntityId | null, pos?: { x: number; y: number }) => void
}

function EdgeCell(props: EdgeCellProps) {
  const {
    targetId, label, imageUri, isDefender, defenderKind, defenderValue,
    amount, isLethal, isDrain, unlocked, editable, canDecrement, canIncrement,
    lethalThreshold, onAdjust, thumbWidth, thumbHeight, hoverCard,
  } = props
  const dim = isDrain && !unlocked
  const border = isLethal
    ? '2px solid #dc2626'
    : isDrain
      ? `2px dashed ${unlocked ? '#fbbf24' : '#555'}`
      : '1px solid #444'

  return (
    <div
      onMouseEnter={(e) => !isDefender && hoverCard(targetId, { x: e.clientX, y: e.clientY })}
      onMouseLeave={() => hoverCard(null)}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        padding: 6,
        background: 'rgba(0,0,0,0.4)',
        border,
        borderRadius: 6,
        minWidth: thumbWidth + 16,
        opacity: dim ? 0.45 : 1,
      }}
    >
      {isDefender ? (
        <DefenderThumb
          label={label}
          kind={defenderKind}
          value={defenderValue}
          width={thumbWidth}
          height={thumbHeight}
        />
      ) : (
        <div style={{ width: thumbWidth, height: thumbHeight, borderRadius: 4, overflow: 'hidden' }}>
          <img
            src={getCardImageUrl(label, imageUri)}
            alt={label}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        </div>
      )}

      {lethalThreshold != null && (
        <div style={{ fontSize: 10, color: isLethal ? '#4ade80' : '#aaa' }}>
          lethal {lethalThreshold}{isLethal ? ' ✓' : ''}
        </div>
      )}

      <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
        <button
          onClick={() => onAdjust(-1)}
          disabled={!canDecrement}
          style={stepBtn(canDecrement)}
          aria-label="decrease damage"
        >
          −
        </button>
        <DragHandle
          amount={amount}
          editable={editable}
          canIncrement={canIncrement}
          canDecrement={canDecrement}
          onAdjust={onAdjust}
        />
        <button
          onClick={() => onAdjust(1)}
          disabled={!canIncrement}
          style={stepBtn(canIncrement)}
          aria-label="increase damage"
        >
          +
        </button>
      </div>

      {!editable && (
        <div style={{ fontSize: 9, color: '#888', fontStyle: 'italic' }}>
          read-only
        </div>
      )}
    </div>
  )
}

/**
 * Vertical-drag amount readout. Holding the mouse on the value and dragging up
 * increments, dragging down decrements. Each 14 px of motion is one step;
 * lethal-first is enforced by [canIncrement] / [canDecrement] from the parent.
 * The ± buttons next to it stay as click/tap fallback (and the only path on
 * touch-only devices since we don't bind pointer events here).
 */
function DragHandle({
  amount,
  editable,
  canIncrement,
  canDecrement,
  onAdjust,
}: {
  amount: number
  editable: boolean
  canIncrement: boolean
  canDecrement: boolean
  onAdjust: (delta: number) => void
}) {
  const DRAG_PX_PER_STEP = 14
  const [dragging, setDragging] = useState(false)
  // Latest readouts captured in refs so the window listeners see fresh values
  // without re-binding on every render.
  const startYRef = useRef(0)
  const appliedStepsRef = useRef(0)
  const canIncRef = useRef(canIncrement)
  const canDecRef = useRef(canDecrement)
  canIncRef.current = canIncrement
  canDecRef.current = canDecrement

  useEffect(() => {
    if (!dragging) return
    const onMove = (e: MouseEvent) => {
      const deltaY = startYRef.current - e.clientY // up = positive = increase
      const targetSteps = Math.trunc(deltaY / DRAG_PX_PER_STEP)
      const stepDelta = targetSteps - appliedStepsRef.current
      if (stepDelta === 0) return
      const direction = Math.sign(stepDelta)
      // Apply one step at a time so the parent's per-step clamp / lethal-first
      // gates each increment individually (a single big delta would skip the
      // check and could overshoot a lethal threshold).
      for (let i = 0; i < Math.abs(stepDelta); i++) {
        if (direction > 0 && !canIncRef.current) break
        if (direction < 0 && !canDecRef.current) break
        onAdjust(direction)
        appliedStepsRef.current += direction
      }
    }
    const onUp = () => setDragging(false)
    window.addEventListener('mousemove', onMove)
    window.addEventListener('mouseup', onUp)
    return () => {
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseup', onUp)
    }
  }, [dragging, onAdjust])

  const onMouseDown = (e: React.MouseEvent) => {
    if (!editable) return
    e.preventDefault()
    startYRef.current = e.clientY
    appliedStepsRef.current = 0
    setDragging(true)
  }

  return (
    <div
      onMouseDown={onMouseDown}
      role="slider"
      aria-valuenow={amount}
      aria-label="damage amount (drag vertically to change)"
      tabIndex={editable ? 0 : -1}
      style={{
        minWidth: 28,
        textAlign: 'center',
        fontWeight: 700,
        color: amount > 0 ? '#dc2626' : '#666',
        cursor: editable ? (dragging ? 'ns-resize' : 'grab') : 'default',
        userSelect: 'none',
        padding: '0 4px',
      }}
    >
      {amount}
    </div>
  )
}

function DefenderThumb({
  label,
  kind,
  value,
  width,
  height,
}: {
  label: string
  kind: 'PLAYER' | 'PLANESWALKER' | 'BATTLE' | 'ATTACKER' | 'BLOCKER' | undefined
  value: number | null
  width: number
  height: number
}) {
  const icon = kind === 'PLAYER' ? '♟' : kind === 'PLANESWALKER' ? '✦' : kind === 'BATTLE' ? '⚔' : '?'
  const valueLabel = kind === 'PLAYER' ? `Life: ${value}` : kind === 'PLANESWALKER' ? `Loyalty: ${value}` : kind === 'BATTLE' ? `Defense: ${value}` : ''
  return (
    <div
      style={{
        width,
        height,
        borderRadius: 4,
        backgroundColor: '#1a1a2e',
        border: '1px solid #333',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 4,
      }}
    >
      <span style={{ color: '#888', fontSize: 22 }}>{icon}</span>
      <span style={{ color: 'white', fontSize: 11, textAlign: 'center', padding: '0 4px' }}>{label}</span>
      {valueLabel && <span style={{ color: '#888', fontSize: 10 }}>{valueLabel}</span>}
    </div>
  )
}

function BandChip({ idx, band }: { idx: number; band: ReturnType<typeof bandColorFor> }) {
  return (
    <span
      style={{
        background: band.chipBg,
        color: 'white',
        fontWeight: 700,
        fontSize: 10,
        padding: '1px 5px',
        borderRadius: 3,
        border: `1px solid ${band.border}`,
      }}
    >
      B{idx + 1}
    </span>
  )
}

function KeywordChip({ color, children }: { color: string; children: React.ReactNode }) {
  return <span style={{ color, fontSize: 10 }}>{children}</span>
}

function MarkedDamageChip({ amount }: { amount: number }) {
  return (
    <span
      style={{
        background: 'rgba(220, 38, 38, 0.2)',
        color: '#fca5a5',
        fontSize: 10,
        padding: '1px 5px',
        borderRadius: 3,
        border: '1px solid #7f1d1d',
      }}
    >
      {amount} marked
    </span>
  )
}

// ─── style helpers ─────────────────────────────────────────────────────────

function controlBtn(bg: string, color: string, border?: string): React.CSSProperties {
  return {
    padding: '10px 20px',
    backgroundColor: bg,
    color,
    border: border ?? 'none',
    borderRadius: 6,
    cursor: 'pointer',
    fontSize: 14,
  }
}

function stepBtn(enabled: boolean): React.CSSProperties {
  return {
    width: 24,
    height: 24,
    borderRadius: 4,
    border: '1px solid #555',
    background: '#1f2937',
    color: enabled ? 'white' : '#555',
    fontWeight: 700,
    cursor: enabled ? 'pointer' : 'not-allowed',
  }
}
