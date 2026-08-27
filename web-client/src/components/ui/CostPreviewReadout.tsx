import { useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { parseManaCost } from '@/utils/manaCost'
import { ManaSymbol } from './ManaSymbols'

/**
 * The "printed cost → what you still owe" strip every payment HUD shows (convoke, improvise /
 * waterbend, harmonize, delve), plus the server's verdict on the selection so far.
 *
 * Every number here is the engine's: the remaining cost, whether it is payable, and why not,
 * come from the `costPreview` reply to the draft the HUD is building — there is no client
 * arithmetic to disagree with the server. While a reply is in flight the last answer stays up,
 * dimmed, so the strip never flickers to nothing between clicks. The verdict is a readout, not
 * a gate: the Confirm button beside it stays live, and the submission is what the server rules
 * on.
 */
export function CostPreviewReadout({ originalCost, size = 18 }: { originalCost: string; size?: number }) {
  const costPreview = useGameStore((state) => state.costPreview)
  const originalSymbols = useMemo(() => parseManaCost(originalCost), [originalCost])
  const preview = costPreview?.preview ?? null
  const pending = costPreview?.pending ?? true
  const remainingSymbols = useMemo(
    () => (preview ? parseManaCost(preview.manaCostString) : null),
    [preview?.manaCostString],
  )

  return (
    <>
      <span style={styles.costLabel}>Cost:</span>
      <div style={styles.manaSymbols}>
        {originalSymbols.map((symbol, i) => (
          <ManaSymbol key={i} symbol={symbol} size={size} />
        ))}
      </div>
      <span style={styles.arrow}>→</span>
      <div style={{ ...styles.manaSymbols, opacity: pending ? 0.55 : 1 }} title={pending ? 'Asking the server…' : undefined}>
        {remainingSymbols === null ? (
          <span style={styles.pendingDots}>…</span>
        ) : remainingSymbols.length > 0 ? (
          remainingSymbols.map((symbol, i) => <ManaSymbol key={i} symbol={symbol} size={size} />)
        ) : (
          <span style={styles.freeCast}>Free!</span>
        )}
      </div>
      {preview && !preview.affordable && (
        <span style={styles.unpayable} title={preview.error ?? undefined}>
          {shortReason(preview.error)}
        </span>
      )}
    </>
  )
}

/**
 * The server's reason, trimmed for a one-line HUD. The full text is on the tooltip.
 */
function shortReason(error: string | null | undefined): string {
  if (!error) return "can't pay this"
  if (/not enough mana/i.test(error)) return 'not enough mana'
  return error.length > 48 ? `${error.slice(0, 45)}…` : error
}

const styles: Record<string, React.CSSProperties> = {
  costLabel: {
    color: '#888',
    fontSize: 13,
  },
  manaSymbols: {
    display: 'flex',
    alignItems: 'center',
    gap: 3,
    transition: 'opacity 120ms ease',
  },
  arrow: {
    color: '#666',
    fontSize: 14,
  },
  freeCast: {
    color: '#4caf50',
    fontWeight: 'bold',
    fontSize: 13,
  },
  pendingDots: {
    color: '#888',
    fontSize: 14,
    minWidth: 18,
    textAlign: 'center',
  },
  unpayable: {
    color: '#e0a83a',
    fontSize: 12,
    maxWidth: 220,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
}
