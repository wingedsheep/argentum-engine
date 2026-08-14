import { useEffect, useReducer, useRef } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ChooseTargetsDecision, EntityId } from '@/types'
import { useResponsive } from '@/hooks/useResponsive.ts'
import {
  chooseTargetsView,
  chooseTargetsWalkReducer,
  initialChooseTargetsWalk,
  type ChooseTargetsWalkState,
} from './chooseTargetsWalk.ts'
import { BattlefieldTargetingUI } from './BattlefieldTargetingUI'
import { GraveyardTargetingUI } from './GraveyardTargetingUI'

/**
 * Walks a ChooseTargetsDecision one target requirement at a time, routing **each** requirement to
 * the UI that can collect it, and submits every slot together once the last one is answered.
 *
 * The routing has to be per requirement, not per decision: a requirement whose legal targets all
 * live in a graveyard or exile pile needs the pile picker ([GraveyardTargetingUI]) because a pile
 * isn't clickable card-by-card on the board, while everything else is picked by clicking the board
 * ([BattlefieldTargetingUI]). The Spot, Living Portal's ETB has one of each — "exile up to one
 * target nonland permanent **and** up to one target nonland permanent card from a graveyard" — so
 * deciding once for the whole decision strands one of the two slots: the graveyard card would have
 * no clickable target at all, and a graveyard-first decision would submit slot 0 and silently drop
 * the rest.
 *
 * A *single* requirement can also span both, which is not the same shape: Taskmaster, Mercenary
 * Mimic's trigger targets "up to one target creature on the battlefield **or** creature card in a
 * graveyard". Then the board banner stays up (permanents stay clickable) and offers a button that
 * opens the pile picker, which offers the way back — the picks cross with the player, so both
 * halves count toward the same slot. Deciding all-or-nothing there made the graveyard half
 * unreachable, which is exactly the bug the action path ([TargetingOverlay]) had. Both paths now
 * route through the same `routeTargetsByZone`.
 *
 * The walk itself (which slot, what's collected, which collector is showing) is the pure reducer in
 * [chooseTargetsWalk] so it can be tested without a DOM.
 *
 * Player-only lone requirements are handled before this by PlayerTargetingUI (click a life orb).
 */
export function ChooseTargetsUI({ decision }: { decision: ChooseTargetsDecision }) {
  const gameState = useGameStore((s) => s.gameState)
  const submitTargetsDecision = useGameStore((s) => s.submitTargetsDecision)
  const responsive = useResponsive()

  const [walk, dispatch] = useReducer(chooseTargetsWalkReducer, initialChooseTargetsWalk)

  const totalRequirements = decision.targetRequirements.length
  const view = chooseTargetsView(decision, walk, gameState?.cards)

  // The reducer stays pure: confirming the final requirement parks the payload in `submission` and
  // the send happens here. The ref guards against a second SubmitDecision for the same walk — the
  // app runs under StrictMode, which re-runs mount effects, and a duplicate submission would answer
  // whatever decision came next.
  //
  // It holds the payload it sent rather than a bare `true`. StrictMode re-runs the effect with the
  // *same* `submission` object, which is what has to be suppressed; a re-confirm always builds a
  // fresh one (the reducer spreads `collected` into a new object), which must go through. A bare
  // boolean can't tell those apart and latches forever — so a server that re-sent the same decision
  // after rejecting a submission would leave the walk with no way to answer it.
  const submittedRef = useRef<ChooseTargetsWalkState['submission']>(null)
  useEffect(() => {
    if (walk.submission && submittedRef.current !== walk.submission) {
      submittedRef.current = walk.submission
      submitTargetsDecision(walk.submission)
    }
  }, [walk.submission])

  const handleComplete = (targets: readonly EntityId[]) =>
    dispatch({ type: 'confirm', targets, totalRequirements })

  const backProps = walk.requirementIndex > 0 ? { onBack: () => dispatch({ type: 'back' }) } : {}

  if (view.collector === 'pile') {
    return (
      <GraveyardTargetingUI
        key={walk.requirementIndex}
        decision={decision}
        graveyardCards={view.pileCards}
        responsive={responsive}
        requirementIndex={walk.requirementIndex}
        totalRequirements={totalRequirements}
        initialSelection={walk.pending}
        onComplete={handleComplete}
        {...(view.isMixed
          ? { onViewBattlefield: (carried: readonly EntityId[]) => dispatch({ type: 'closePile', carried }) }
          : {})}
        {...backProps}
      />
    )
  }

  return (
    <BattlefieldTargetingUI
      key={walk.requirementIndex}
      decision={decision}
      requirementIndex={walk.requirementIndex}
      totalRequirements={totalRequirements}
      legalTargets={view.legalTargets}
      initialSelection={walk.pending}
      onComplete={handleComplete}
      {...(view.isMixed
        ? {
            pileButton: {
              zoneLabel: view.pileZoneLabel,
              count: view.pileCards.length,
              onOpen: (carried: readonly EntityId[]) => dispatch({ type: 'openPile', carried }),
            },
          }
        : {})}
      {...backProps}
    />
  )
}
