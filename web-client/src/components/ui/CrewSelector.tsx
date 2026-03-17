import { useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'

/**
 * Compact floating HUD bar for crew selection.
 * Shows power progress and confirm/cancel buttons while
 * creatures are selected directly on the battlefield.
 */
export function CrewSelector() {
  const crewSelectionState = useGameStore((state) => state.crewSelectionState)
  const cancelCrewSelection = useGameStore((state) => state.cancelCrewSelection)
  const confirmCrewSelection = useGameStore((state) => state.confirmCrewSelection)

  const selectedPower = useMemo(() => {
    if (!crewSelectionState) return 0
    const { selectedCreatures, validCreatures } = crewSelectionState
    let total = 0
    for (const id of selectedCreatures) {
      const creature = validCreatures.find((c) => c.entityId === id)
      if (creature) total += creature.power
    }
    return total
  }, [crewSelectionState])

  if (!crewSelectionState) return null

  const { vehicleName, crewPower, selectedCreatures } = crewSelectionState
  const canConfirm = selectedPower >= crewPower

  return (
    <div style={styles.bar}>
      <span style={styles.label}>
        Crew <strong>{vehicleName}</strong>
      </span>
      <span style={styles.divider} />
      <span style={styles.powerInfo}>
        Power:&nbsp;
        <strong style={{ color: canConfirm ? '#4caf50' : '#ff9800' }}>
          {selectedPower}
        </strong>
        &nbsp;/&nbsp;{crewPower}
      </span>
      <span style={styles.count}>
        ({selectedCreatures.length} creature{selectedCreatures.length !== 1 ? 's' : ''})
      </span>
      <span style={styles.divider} />
      <button onClick={cancelCrewSelection} style={styles.cancelButton}>
        Cancel
      </button>
      <button
        onClick={confirmCrewSelection}
        disabled={!canConfirm}
        style={{
          ...styles.confirmButton,
          opacity: canConfirm ? 1 : 0.5,
          cursor: canConfirm ? 'pointer' : 'not-allowed',
        }}
      >
        Confirm Crew
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
    gap: 12,
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
  powerInfo: {
    color: '#aaa',
    fontSize: 14,
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
