import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ClientYield } from '@/types/gameState'

/**
 * A small fixed panel listing the player's active persistent yields (MTGO right-click yields —
 * backlog §C) with per-yield revoke and a clear-all, so a player is never trapped by a yield they
 * forgot they set. Hidden when there are no yields. Server-authoritative: it reflects
 * `gameState.activeYields` (masked to the viewer) and dispatches revoke intents.
 */
export function ActiveYieldsPanel() {
  const yields = useGameStore((s) => s.gameState?.activeYields) ?? []
  const clearAbilityYield = useGameStore((s) => s.clearAbilityYield)
  const clearAllYields = useGameStore((s) => s.clearAllYields)
  const [collapsed, setCollapsed] = useState(false)

  if (yields.length === 0) return null

  return (
    <div
      style={{
        position: 'fixed',
        right: 12,
        bottom: 96,
        zIndex: 60,
        width: 230,
        background: 'rgba(18, 14, 28, 0.94)',
        border: '1px solid rgba(150, 100, 200, 0.45)',
        borderRadius: 8,
        boxShadow: '0 4px 14px rgba(0, 0, 0, 0.5)',
        color: '#e0d4f0',
        fontSize: 11,
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '6px 10px',
          background: 'rgba(80, 50, 120, 0.4)',
          cursor: 'pointer',
        }}
        onClick={() => setCollapsed((c) => !c)}
        title="Persistent yields you've set"
      >
        <span style={{ fontWeight: 700 }}>Active yields ({yields.length})</span>
        <span aria-hidden style={{ color: '#b8a8cc' }}>{collapsed ? '▸' : '▾'}</span>
      </div>

      {!collapsed && (
        <div style={{ maxHeight: '40vh', overflowY: 'auto' }}>
          {yields.map((y) => (
            <YieldRow
              key={`${y.cardDefinitionId}|${y.abilityId}`}
              entry={y}
              onRevoke={() => clearAbilityYield(y.cardDefinitionId, y.abilityId)}
            />
          ))}
          <button
            type="button"
            onClick={() => clearAllYields()}
            style={{
              display: 'block',
              width: '100%',
              padding: '7px 10px',
              background: 'transparent',
              border: 'none',
              borderTop: '1px solid rgba(150,100,200,0.25)',
              color: '#ff9a8c',
              cursor: 'pointer',
              font: 'inherit',
              textAlign: 'center',
            }}
          >
            Clear all yields
          </button>
        </div>
      )}
    </div>
  )
}

/** Human-readable summary of which yield dimensions are active on one ability. */
function describe(y: ClientYield): string {
  const parts: string[] = []
  if (y.autoAnswer === true) parts.push('always yes')
  if (y.autoAnswer === false) parts.push('always no')
  if (y.wholeGame) parts.push('yield (game)')
  if (y.untilEndOfTurn) parts.push('yield (turn)')
  return parts.join(', ')
}

function YieldRow({ entry, onRevoke }: { entry: ClientYield; onRevoke: () => void }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 6, padding: '6px 10px' }}>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {entry.displayName}
        </div>
        <div style={{ color: '#b8a8cc', fontSize: 10 }}>{describe(entry)}</div>
      </div>
      <button
        type="button"
        onClick={onRevoke}
        title="Revoke this yield"
        style={{
          flexShrink: 0,
          width: 20,
          height: 20,
          lineHeight: '18px',
          textAlign: 'center',
          background: 'rgba(150, 60, 60, 0.3)',
          border: '1px solid rgba(200, 90, 90, 0.5)',
          borderRadius: 4,
          color: '#ffc0b0',
          cursor: 'pointer',
          padding: 0,
        }}
      >
        ×
      </button>
    </div>
  )
}
