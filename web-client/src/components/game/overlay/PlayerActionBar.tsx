import { useGameStore } from '@/store/gameStore.ts'
import './PlayerActionBar.css'

/** Effect-created actions belong to the player, so remain usable after the spell leaves the stack. */
export function PlayerActionBar() {
  const legalActions = useGameStore((state) => state.legalActions)
  const pipelineState = useGameStore((state) => state.pipelineState)
  const actions = legalActions.filter((offer) => offer.action.type === 'TakePlayerAction')
  if (actions.length === 0 || pipelineState != null) return null
  return (
    <div className="player-action-bar" aria-label="Actions from resolved spells">
      {actions.map((offer) => (
        <button key={offer.action.type === 'TakePlayerAction' ? offer.action.permissionId : offer.description}
          disabled={!offer.isAffordable} onClick={() => {
            const live = useGameStore.getState()
            const current = live.legalActions.find((candidate) =>
              candidate.action.type === 'TakePlayerAction' && offer.action.type === 'TakePlayerAction' &&
              candidate.action.permissionId === offer.action.permissionId && candidate.isAffordable)
            if (current && live.pipelineState == null && offer.interactionEpoch === live.interactionEpoch) {
              live.submitAction(offer.action, offer.interactionEpoch)
            }
          }}>
          {offer.description}
        </button>
      ))}
    </div>
  )
}
