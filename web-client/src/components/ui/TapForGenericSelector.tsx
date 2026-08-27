import { useGameStore } from '@/store/gameStore.ts'
import { CostPreviewReadout } from './CostPreviewReadout'
import { ManaSymbol } from './ManaSymbols'

/**
 * Compact floating HUD bar for a **tap-for-generic** payment — improvise (CR 702.126) or a
 * waterbend cost. Mirrors the Convoke selector but generic-only: each tapped permanent pays {1},
 * with no color choice. Permanents are selected directly on the battlefield (the server decides
 * which are eligible: artifacts for improvise, artifacts or creatures for waterbend); this bar
 * shows progress and confirm/cancel. Confirming with nothing selected pays the cost entirely with
 * mana, which is legal for both — the taps are always optional ("you *may* tap").
 *
 * The cost readout is the server's cost preview for the draft as it stands and never disables
 * Confirm: the server validates every tapped permanent and the payment itself, and says why when
 * it declines.
 */
export function TapForGenericSelector() {
  const tapForGenericSelectionState = useGameStore((state) => state.tapForGenericSelectionState)
  const cancelTapForGenericSelection = useGameStore((state) => state.cancelTapForGenericSelection)
  const confirmTapForGenericSelection = useGameStore((state) => state.confirmTapForGenericSelection)

  if (!tapForGenericSelectionState) return null

  const { cardName, selectedPermanents, maxTaps, label, manaCost } = tapForGenericSelectionState

  return (
    <div style={styles.bar}>
      <span style={styles.label}>
        {/* Card name first, then the payment prompt in parentheses with the amount {N} rendered
            as a proper mana pip (not literal "{N}" text): e.g. "Ruinous Waterbending (waterbend
            {4})", "Ironheart, Clever Champion (improvise {4})". */}
        <strong>{cardName}</strong>
        <span style={{ display: 'inline-flex', alignItems: 'center', marginLeft: 6 }}>
          <span style={{ marginRight: 3 }}>({label}</span>
          <ManaSymbol symbol={String(maxTaps)} size={16} />
          <span>)</span>
        </span>
      </span>
      <span style={styles.divider} />
      <CostPreviewReadout originalCost={manaCost} />
      <span style={styles.count}>({selectedPermanents.length} tapped)</span>
      <span style={styles.divider} />
      <button onClick={cancelTapForGenericSelection} style={styles.cancelButton}>
        Cancel
      </button>
      <button onClick={confirmTapForGenericSelection} style={styles.confirmButton}>
        Confirm
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
    backgroundColor: 'rgba(20, 30, 48, 0.95)',
    border: '2px solid #3a6a8a',
    borderRadius: 10,
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.6)',
    zIndex: 1500,
    whiteSpace: 'nowrap',
  },
  label: { color: '#cce', fontSize: 14 },
  divider: { width: 1, height: 20, backgroundColor: '#3a6a8a' },
  count: { color: '#668', fontSize: 12 },
  cancelButton: {
    padding: '6px 14px', fontSize: 13, backgroundColor: '#444', color: '#fff',
    border: 'none', borderRadius: 6, cursor: 'pointer',
  },
  confirmButton: {
    padding: '6px 14px', fontSize: 13, backgroundColor: '#0088cc', color: '#fff',
    border: 'none', borderRadius: 6, cursor: 'pointer',
  },
}
