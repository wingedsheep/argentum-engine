import { useGameStore } from '@/store/gameStore.ts'
import { CostPreviewReadout } from './CostPreviewReadout'

/**
 * Compact floating HUD bar for convoke selection.
 * Shows mana cost progress and confirm/cancel buttons while
 * creatures are selected directly on the battlefield.
 *
 * The remaining cost and the payable/unpayable verdict are the server's — every toggle re-prices
 * the draft through the cost preview (`CostPreviewReadout`). Nothing here gates the Cast button:
 * the server decides whether the selection is payable and rejects one that isn't with a message
 * naming the creature (see `AlternativePaymentHandler.validateForSpell`).
 */
export function ConvokeSelector() {
  const convokeSelectionState = useGameStore((state) => state.convokeSelectionState)
  const cancelConvokeSelection = useGameStore((state) => state.cancelConvokeSelection)
  const confirmConvokeSelection = useGameStore((state) => state.confirmConvokeSelection)

  if (!convokeSelectionState) return null

  const { cardName, selectedCreatures, actionInfo, manaCost } = convokeSelectionState
  const isAbility = actionInfo.action.type === 'ActivateAbility'

  return (
    <div style={styles.bar}>
      <span style={styles.label}>
        {isAbility ? 'Tap creatures for' : 'Convoke'} <strong>{cardName}</strong>
      </span>
      <span style={styles.divider} />
      <CostPreviewReadout originalCost={manaCost} />
      <span style={styles.count}>
        ({selectedCreatures.length} tapped)
      </span>
      <span style={styles.divider} />
      <button onClick={cancelConvokeSelection} style={styles.cancelButton}>
        Cancel
      </button>
      <button onClick={confirmConvokeSelection} style={styles.confirmButton}>
        {isAbility ? 'Activate' : 'Cast'}
      </button>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  bar: {
    position: 'absolute',
    bottom: 12,
    left: '50%',
    transform: 'translateX(-50%)',
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '10px 20px',
    backgroundColor: 'rgba(20, 20, 40, 0.95)',
    border: '2px solid #4a4a6a',
    borderRadius: 10,
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.6)',
    zIndex: 1500,
    whiteSpace: 'nowrap',
  },
  label: {
    color: '#ccc',
    fontSize: 14,
  },
  divider: {
    width: 1,
    height: 20,
    backgroundColor: '#4a4a6a',
  },
  count: {
    color: '#666',
    fontSize: 12,
  },
  cancelButton: {
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: '#444',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  confirmButton: {
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: '#0066cc',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
}
