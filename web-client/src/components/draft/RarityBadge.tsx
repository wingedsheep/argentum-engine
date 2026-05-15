import type { CSSProperties } from 'react'

const RARITY_COLORS: Record<string, string> = {
  MYTHIC: '#ff8b00',
  RARE: '#ffd700',
  UNCOMMON: '#c0c0c0',
  COMMON: '#555',
}

function normalizeRarity(rarity: string): 'MYTHIC' | 'RARE' | 'UNCOMMON' | 'COMMON' {
  const upper = rarity.toUpperCase()
  if (upper === 'MYTHIC' || upper === 'RARE' || upper === 'UNCOMMON') return upper
  return 'COMMON'
}

export function rarityColor(rarity: string): string {
  return RARITY_COLORS[normalizeRarity(rarity)]!
}

/**
 * Tiny single-letter pill — M/R/U/C — colored by rarity. Used in draft picked-card
 * lists and opponent-card overlays so players can gauge pool quality at a glance.
 */
export function RarityBadge({ rarity, size = 12 }: { rarity: string; size?: number }) {
  const key = normalizeRarity(rarity)
  const letter = key[0]
  const isLight = key === 'RARE' || key === 'UNCOMMON'
  const style: CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
    width: size + 2,
    height: size + 2,
    borderRadius: 3,
    fontSize: size - 3,
    fontWeight: 700,
    lineHeight: 1,
    fontFamily: 'monospace',
    backgroundColor: RARITY_COLORS[key]!,
    color: isLight ? '#1a1a1a' : '#fff',
  }
  return <span style={style} title={key.charAt(0) + key.slice(1).toLowerCase()}>{letter}</span>
}
