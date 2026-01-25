import { useGameStore } from '../../store/gameStore'

/**
 * MTG Arena-style X cost selector overlay.
 * Shown when casting spells with {X} in their mana cost.
 */
export function XCostSelector() {
  const xSelectionState = useGameStore((state) => state.xSelectionState)
  const updateXValue = useGameStore((state) => state.updateXValue)
  const cancelXSelection = useGameStore((state) => state.cancelXSelection)
  const confirmXSelection = useGameStore((state) => state.confirmXSelection)

  if (!xSelectionState) return null

  const { cardName, minX, maxX, selectedX } = xSelectionState

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    updateXValue(parseInt(e.target.value, 10))
  }

  const handleIncrement = () => {
    if (selectedX < maxX) {
      updateXValue(selectedX + 1)
    }
  }

  const handleDecrement = () => {
    if (selectedX > minX) {
      updateXValue(selectedX - 1)
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.container}>
        <h2 style={styles.title}>Choose X Value</h2>
        <p style={styles.cardName}>{cardName}</p>

        <div style={styles.valueDisplay}>
          <span style={styles.xLabel}>X =</span>
          <span style={styles.xValue}>{selectedX}</span>
        </div>

        <div style={styles.controls}>
          <button
            onClick={handleDecrement}
            disabled={selectedX <= minX}
            style={{
              ...styles.controlButton,
              opacity: selectedX <= minX ? 0.5 : 1,
              cursor: selectedX <= minX ? 'not-allowed' : 'pointer',
            }}
          >
            -
          </button>

          <input
            type="range"
            min={minX}
            max={maxX}
            value={selectedX}
            onChange={handleSliderChange}
            style={styles.slider}
          />

          <button
            onClick={handleIncrement}
            disabled={selectedX >= maxX}
            style={{
              ...styles.controlButton,
              opacity: selectedX >= maxX ? 0.5 : 1,
              cursor: selectedX >= maxX ? 'not-allowed' : 'pointer',
            }}
          >
            +
          </button>
        </div>

        <p style={styles.manaInfo}>
          Available mana: {maxX + (xSelectionState.actionInfo.action.type === 'CastSpell' ? 0 : 0)}
        </p>

        <div style={styles.buttonRow}>
          <button onClick={cancelXSelection} style={styles.cancelButton}>
            Cancel
          </button>
          <button onClick={confirmXSelection} style={styles.confirmButton}>
            Cast
          </button>
        </div>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1500,
  },
  container: {
    backgroundColor: '#1a1a2e',
    borderRadius: 12,
    padding: 24,
    minWidth: 320,
    maxWidth: 400,
    border: '2px solid #4a4a6a',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
  },
  title: {
    margin: '0 0 8px 0',
    color: '#fff',
    fontSize: 20,
    textAlign: 'center',
  },
  cardName: {
    margin: '0 0 24px 0',
    color: '#aaa',
    fontSize: 16,
    textAlign: 'center',
    fontStyle: 'italic',
  },
  valueDisplay: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    marginBottom: 24,
  },
  xLabel: {
    color: '#888',
    fontSize: 24,
    fontWeight: 'bold',
  },
  xValue: {
    color: '#ffcc00',
    fontSize: 48,
    fontWeight: 'bold',
    minWidth: 60,
    textAlign: 'center',
  },
  controls: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 16,
  },
  controlButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    border: 'none',
    backgroundColor: '#333',
    color: '#fff',
    fontSize: 24,
    fontWeight: 'bold',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  slider: {
    flex: 1,
    height: 8,
    appearance: 'none',
    backgroundColor: '#333',
    borderRadius: 4,
    cursor: 'pointer',
  },
  manaInfo: {
    margin: '0 0 24px 0',
    color: '#666',
    fontSize: 14,
    textAlign: 'center',
  },
  buttonRow: {
    display: 'flex',
    gap: 12,
    justifyContent: 'center',
  },
  cancelButton: {
    padding: '10px 24px',
    fontSize: 16,
    backgroundColor: '#444',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
  confirmButton: {
    padding: '10px 24px',
    fontSize: 16,
    backgroundColor: '#0066cc',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
}
