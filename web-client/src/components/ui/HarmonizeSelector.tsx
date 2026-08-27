import { useGameStore } from '@/store/gameStore.ts'
import { CostPreviewReadout } from './CostPreviewReadout'

/**
 * Floating HUD bar for the Harmonize creature-tap step. The player may tap one creature
 * (selected directly on the battlefield) to reduce the generic harmonize cost by its power;
 * {X} is already expanded into the displayed cost. Tapping is optional — "Cast" is always
 * live; the reduced cost shown is the server's cost preview for the draft (which applies the
 * reduction in the engine's own order, printed generic first, then the X), and the server has
 * the final say on the tapped creature and the payment.
 */
export function HarmonizeSelector() {
  const harmonizeSelectionState = useGameStore((state) => state.harmonizeSelectionState)
  const cancelHarmonizeSelection = useGameStore((state) => state.cancelHarmonizeSelection)
  const confirmHarmonizeSelection = useGameStore((state) => state.confirmHarmonizeSelection)

  if (!harmonizeSelectionState) return null

  const { cardName, selectedCreature, validCreatures, manaCost } = harmonizeSelectionState
  const tappedPower = selectedCreature
    ? validCreatures.find((c) => c.entityId === selectedCreature)?.power ?? 0
    : 0

  return (
    <div style={styles.bar}>
      <span style={styles.label}>
        Harmonize <strong>{cardName}</strong>
      </span>
      <span style={styles.divider} />
      <span style={styles.hint}>
        {selectedCreature ? `Tapping a ${tappedPower}-power creature` : 'Tap a creature to reduce (optional)'}
      </span>
      <span style={styles.divider} />
      <CostPreviewReadout originalCost={manaCost} />
      <span style={styles.divider} />
      <button onClick={cancelHarmonizeSelection} style={styles.cancelButton}>
        Cancel
      </button>
      <button onClick={confirmHarmonizeSelection} style={styles.confirmButton}>
        Cast
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
  label: { color: '#ccc', fontSize: 14 },
  hint: { color: '#9a9', fontSize: 12 },
  divider: { width: 1, height: 20, backgroundColor: '#4a4a6a' },
  cancelButton: {
    padding: '6px 14px', fontSize: 13, backgroundColor: '#444', color: '#fff',
    border: 'none', borderRadius: 6, cursor: 'pointer',
  },
  confirmButton: {
    padding: '6px 14px', fontSize: 13, backgroundColor: '#0066cc', color: '#fff',
    border: 'none', borderRadius: 6, cursor: 'pointer',
  },
}
