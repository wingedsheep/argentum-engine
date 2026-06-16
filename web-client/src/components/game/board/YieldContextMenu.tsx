import type React from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ClientAbilityIdentity, ClientYield } from '@/types/gameState'
import type { YieldKind } from '@/types'

/**
 * Right-click / long-press context menu on a stack ability, mirroring MTGO's persistent-yield
 * options (backlog §C). Lets the controller set "yield until end of turn / always yield / always
 * yes / always no" against an ability, keyed by its {@link ClientAbilityIdentity} so the preference
 * follows every current and future copy. The server owns the authoritative yield state.
 */
export function YieldContextMenu({
  identity,
  sourceName,
  position,
  existing,
  onClose,
}: {
  identity: ClientAbilityIdentity
  sourceName: string
  position: { x: number; y: number }
  /** The current yield on this ability, if any — drives the active marks + revoke row. */
  existing: ClientYield | undefined
  onClose: () => void
}) {
  const setAbilityYield = useGameStore((s) => s.setAbilityYield)
  const clearAbilityYield = useGameStore((s) => s.clearAbilityYield)

  const apply = (kind: YieldKind) => {
    setAbilityYield(identity.cardDefinitionId, identity.abilityId, kind)
    onClose()
  }

  const items: { label: string; kind: YieldKind; active: boolean }[] = [
    { label: 'Yield until end of turn', kind: 'YIELD_UNTIL_END_OF_TURN', active: existing?.untilEndOfTurn ?? false },
    { label: 'Always yield', kind: 'YIELD_WHOLE_GAME', active: existing?.wholeGame ?? false },
    { label: 'Always answer Yes', kind: 'ALWAYS_ANSWER_YES', active: existing?.autoAnswer === true },
    { label: 'Always answer No', kind: 'ALWAYS_ANSWER_NO', active: existing?.autoAnswer === false },
  ]

  // Clamp roughly within the viewport so the menu never opens off-screen.
  const left = Math.min(position.x, window.innerWidth - 230)
  const top = Math.min(position.y, window.innerHeight - 200)

  return (
    <>
      {/* Click-away backdrop */}
      <div
        style={{ position: 'fixed', inset: 0, zIndex: 999 }}
        onClick={onClose}
        onContextMenu={(e) => { e.preventDefault(); onClose() }}
      />
      <div
        role="menu"
        style={{
          position: 'fixed',
          left,
          top,
          zIndex: 1000,
          minWidth: 210,
          background: 'rgba(18, 14, 28, 0.97)',
          border: '1px solid rgba(150, 100, 200, 0.5)',
          borderRadius: 8,
          boxShadow: '0 6px 20px rgba(0, 0, 0, 0.6)',
          overflow: 'hidden',
          fontSize: 12,
          color: '#e0d4f0',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ padding: '7px 12px', fontWeight: 700, color: '#b8a8cc', borderBottom: '1px solid rgba(150,100,200,0.25)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 230 }}>
          Yields — {sourceName}
        </div>
        {items.map((item) => (
          <MenuButton key={item.kind} label={item.label} active={item.active} onClick={() => apply(item.kind)} />
        ))}
        {existing && (
          <MenuButton
            label="Revoke yields"
            danger
            onClick={() => { clearAbilityYield(identity.cardDefinitionId, identity.abilityId); onClose() }}
          />
        )}
      </div>
    </>
  )
}

function MenuButton({ label, active, danger, onClick }: { label: string; active?: boolean; danger?: boolean; onClick: () => void }) {
  const base: React.CSSProperties = {
    display: 'flex',
    width: '100%',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 8,
    padding: '8px 12px',
    background: 'transparent',
    border: 'none',
    borderTop: danger ? '1px solid rgba(150,100,200,0.25)' : undefined,
    color: danger ? '#ff9a8c' : '#e0d4f0',
    cursor: 'pointer',
    textAlign: 'left',
    font: 'inherit',
  }
  return (
    <button
      type="button"
      style={base}
      onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.background = 'rgba(150, 100, 200, 0.22)' }}
      onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.background = 'transparent' }}
      onClick={onClick}
    >
      <span>{label}</span>
      {active && <span aria-hidden style={{ color: '#7fe0a0', fontWeight: 800 }}>✓</span>}
    </button>
  )
}
