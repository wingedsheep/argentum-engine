/**
 * Mana symbol rendering using local SVG assets.
 */

// Import all mana symbol SVGs as static assets
const symbolModules = import.meta.glob('../../assets/mana/*.svg', { eager: true, query: '?url', import: 'default' }) as Record<string, string>

// Build a lookup map: symbol key -> resolved URL
const SYMBOL_URLS: Record<string, string> = {}
for (const [path, url] of Object.entries(symbolModules)) {
  const match = path.match(/\/(\w+)\.svg$/)
  if (match?.[1]) {
    SYMBOL_URLS[match[1]] = url
  }
}

/**
 * Renders a single mana symbol as an SVG icon.
 */
export function ManaSymbol({ symbol, size = 14 }: { symbol: string; size?: number }) {
  const normalized = symbol.replace('/', '')
  const url = SYMBOL_URLS[normalized]

  if (!url) {
    // Fallback for symbols we don't have locally
    return (
      <span style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: size,
        height: size,
        borderRadius: '50%',
        backgroundColor: '#666',
        color: '#fff',
        fontSize: size * 0.6,
        fontWeight: 700,
        verticalAlign: 'middle',
      }}>
        {symbol}
      </span>
    )
  }

  return (
    <img
      src={url}
      alt={`{${symbol}}`}
      style={{
        width: size,
        height: size,
        verticalAlign: 'middle',
        display: 'inline-block',
      }}
    />
  )
}

/**
 * Renders a full mana cost string like "{2}{W}{U}" as a row of mana symbol icons.
 */
export function ManaCost({ cost, size = 14, gap = 1 }: { cost: string | null; size?: number; gap?: number }) {
  if (!cost) return null

  const symbols = cost.match(/\{([^}]+)\}/g)
  if (!symbols || symbols.length === 0) return null

  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap }}>
      {symbols.map((match, i) => {
        const inner = match.slice(1, -1)
        return <ManaSymbol key={i} symbol={inner} size={size} />
      })}
    </span>
  )
}
