/**
 * Shared full-art deck tile — the deck-gallery visual used everywhere a user picks a deck:
 * the deckbuilder's saved-decks browser, the Quick Game lobby picker, and the tournament
 * Premade Decks picker. One place to restyle a deck tile.
 *
 * Each tile paints the art of the deck's rarest non-land card ({@link rarestCard}) as a wide
 * Scryfall art crop, with a bottom scrim carrying the deck name (tinted by the hero card's
 * rarity), a format chip, colour pips and the card count. Decks with no catalogued non-land
 * card fall back to a colour-identity gradient.
 *
 * Everything a caller varies rides on props: the corner ribbon (`badge`), the storage
 * provenance badge (`storage`), and the hover-revealed corner buttons (`actions`, built with
 * {@link DeckTileActionButton}).
 */
import { useMemo, type ReactNode } from 'react'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { rarityColor } from '@/components/draft/RarityBadge'
import { COLOR_DOT } from '@/components/ui/DeckSummary'
import { labelForFormat } from '@/utils/deckLegality'
import { getCdnArtCropUrl, getScryfallArtCropUrl } from '@/utils/cardImages'
import { commandersOf, mergeCommanderIntoCards } from '@/store/deckLibrary'
import styles from './DeckTile.module.css'

/**
 * The card shape a tile needs. Structurally satisfied by both the deckbuilder's
 * `cardFilter.CardSummary` and the trimmed catalog shape the lobby pickers fetch.
 */
export interface DeckTileCard {
  name: string
  cmc: number
  colors: string[]
  cardTypes: string[]
  basicLand: boolean
  rarity: string
  imageUri?: string | null
}

export interface DeckTileProps {
  /** Deck name, shown in the scrim and tinted by the hero card's rarity. */
  name: string
  /** Total cards (commander included) shown bottom-right. */
  total: number
  /** Deck colours in {@link deckColors} order, rendered as mana pips. */
  colors: string[]
  /** Hero card whose art fills the tile. Null = colour-identity gradient. */
  hero: DeckTileCard | null
  /** Format chip. Null/undefined hides the chip. */
  format?: string | null | undefined
  /** Tooltip for the format chip (e.g. "Saved as Commander" vs "Legal in Commander"). */
  formatTitle?: string | undefined
  /** Optional flavour line under the name — used for example decks' descriptions. */
  description?: string | undefined
  /** The owner's note on a saved deck, set in italics under the name. */
  note?: string | undefined
  /** Draws the accent ring: this tile is the current selection. */
  selected?: boolean
  /** Corner ribbon text ("Editing" / "Selected"). Fades on hover so `actions` can take over. */
  badge?: string | null | undefined
  /** Storage provenance. Omit for decks that have none (server-supplied examples). */
  storage?: 'cloud' | 'local' | undefined
  /** Tooltip on the tile's click surface. */
  title?: string | undefined
  disabled?: boolean
  onClick: () => void
  /** Hover-revealed corner buttons. Build them with {@link DeckTileActionButton}. */
  actions?: ReactNode
  /** Extra chips after the format chip — a cube's pack style, a "Shared" or warning flag. */
  chips?: ReactNode
}

export function DeckTile({
  name,
  total,
  colors,
  hero,
  format = null,
  formatTitle,
  description,
  note,
  selected = false,
  badge,
  storage,
  title,
  disabled = false,
  onClick,
  actions,
  chips,
}: DeckTileProps) {
  // Hero art = the deck's rarest card as a wide Scryfall art crop. Derive it straight
  // from the card's CDN image URL (no rate-limited api.scryfall.com lookup) when we have
  // one, falling back to the by-name API lookup otherwise.
  const artUrl = useMemo(() => heroArtUrl(hero), [hero])
  const gradient = useMemo(() => deckBannerGradient(colors), [colors])

  return (
    <div className={`${styles.tile} ${selected ? styles.tileSelected : ''}`}>
      <button
        type="button"
        className={styles.surface}
        onClick={onClick}
        disabled={disabled}
        {...(title ? { title } : {})}
      >
        <span
          className={styles.art}
          // The URL is quoted inside url() because card names can contain apostrophes
          // ("Barrin's Spite"), which encodeURIComponent leaves literal — an unquoted
          // url() with a bare ' is invalid CSS and the browser silently drops the art.
          style={artUrl ? { backgroundImage: `url("${artUrl}")` } : { background: gradient }}
          aria-hidden="true"
        />
        <span className={styles.scrim} aria-hidden="true" />
        <span className={styles.info}>
          <span className={styles.name} style={{ color: deckNameColor(hero?.rarity) }}>
            {name}
          </span>
          {description && <span className={styles.description}>{description}</span>}
          {note && <span className={styles.note}>{note}</span>}
          <span className={styles.metaRow}>
            {format && (
              <span
                className={styles.format}
                style={{ color: formatAccent(format), borderColor: `${formatAccent(format)}66` }}
                {...(formatTitle ? { title: formatTitle } : {})}
              >
                {labelForFormat(format)}
              </span>
            )}
            {chips}
            <span className={styles.pips}>
              {colors.length > 0 ? (
                colors.map((c) => <ManaSymbol key={c} symbol={COLOR_PIP_SYMBOL[c] ?? 'C'} size={15} />)
              ) : (
                <ManaSymbol symbol="C" size={15} />
              )}
            </span>
            <span className={styles.count}>{total} cards</span>
          </span>
        </span>
      </button>

      {storage && (
        <span
          className={`${styles.storage} ${storage === 'cloud' ? styles.storageCloud : styles.storageLocal}`}
          title={
            storage === 'cloud'
              ? 'Saved to your account — synced to the cloud and available on any device'
              : 'Saved only in this browser — not backed up to your account'
          }
        >
          {storage === 'cloud' ? <CloudIcon /> : <MonitorIcon />}
          {storage === 'cloud' ? 'Cloud' : 'Local'}
        </span>
      )}

      {(badge || actions) && (
        <div className={styles.corner}>
          {badge && <span className={styles.badge}>{badge}</span>}
          {actions && <div className={styles.actions}>{actions}</div>}
        </div>
      )}
    </div>
  )
}

/**
 * A hover-revealed corner button for a tile's `actions` slot. Stops propagation so clicking
 * it never also fires the tile's own "pick this deck" click.
 */
export function DeckTileActionButton({
  onClick,
  title,
  ariaLabel,
  danger = false,
  children,
}: {
  onClick: () => void
  title: string
  ariaLabel: string
  danger?: boolean
  children: ReactNode
}) {
  return (
    <button
      type="button"
      className={danger ? styles.actionButtonDanger : styles.actionButton}
      onClick={(e) => {
        e.stopPropagation()
        onClick()
      }}
      title={title}
      aria-label={ariaLabel}
    >
      {children}
    </button>
  )
}

/** What a deck needs to describe itself as a tile: its list plus the optional chosen cover. */
export interface DeckTileSource {
  cards: Record<string, number>
  commander?: string | null | undefined
  /** The second commander of a partner pair; like `commander`, kept out of `cards` when saved. */
  secondCommander?: string | null | undefined
  coverCard?: string | null | undefined
  coverImageUri?: string | null | undefined
}

export interface DeckTileModel<T extends DeckTileCard> {
  /** Total cards, commander included. */
  total: number
  colors: string[]
  /** The card whose art paints the tile — the chosen cover, else the rarest card. */
  hero: T | null
}

/**
 * Everything a {@link DeckTile} shows that is derived from the deck: total, colours and hero.
 * The one place the cover rule lives — every gallery and picker goes through it so a deck looks
 * the same wherever it is chosen.
 *
 * The chosen cover wins while the card is still in the deck (removing it falls back to the
 * rarest card rather than to a picture of something you no longer play). Its stored image URL
 * — the pinned printing's art — overrides the catalogue's default printing.
 */
export function deckTileModel<T extends DeckTileCard>(
  deck: DeckTileSource,
  catalog: Record<string, T>,
): DeckTileModel<T> {
  const full = mergeCommanderIntoCards(deck.cards, ...commandersOf(deck))
  const total = Object.values(full).reduce((a, b) => a + b, 0)
  return { total, colors: deckColors(full, catalog), hero: deckCover(deck, catalog) }
}

/** The deck's cover card: the chosen one while it's still in the deck, else {@link rarestCard}. */
export function deckCover<T extends DeckTileCard>(
  deck: DeckTileSource,
  catalog: Record<string, T>,
): T | null {
  const cover = deck.coverCard
  if (cover && (deck.cards[cover] || commandersOf(deck).includes(cover))) {
    const known = catalog[cover]
    if (known) return deck.coverImageUri ? { ...known, imageUri: deck.coverImageUri } : known
    // Catalogue not loaded (or the card left it): the stored art is still enough to paint it.
    if (deck.coverImageUri) {
      return {
        name: cover, cmc: 0, colors: [], cardTypes: [], basicLand: false, rarity: 'COMMON',
        imageUri: deck.coverImageUri,
      } as DeckTileCard as T
    }
  }
  const full = mergeCommanderIntoCards(deck.cards, ...commandersOf(deck))
  return rarestCard(full, catalog, deck.commander ?? null)
}

/** The landscape art-crop URL for a hero card, or null when there is none. */
export function heroArtUrl(hero: DeckTileCard | null): string | null {
  if (!hero) return null
  return getCdnArtCropUrl(hero.imageUri) ?? getScryfallArtCropUrl(hero.name)
}

/** Mana-pip symbol per colour key, matching the deckbuilder's colour token order. */
const COLOR_PIP_SYMBOL: Record<string, string> = {
  WHITE: 'W',
  BLUE: 'U',
  BLACK: 'B',
  RED: 'R',
  GREEN: 'G',
}

// Rarity ranking for choosing a deck's "hero" card — the splashiest spell whose art
// represents the deck in the gallery. Higher wins.
const RARITY_RANK: Record<string, number> = { MYTHIC: 3, RARE: 2, UNCOMMON: 1, COMMON: 0 }

/**
 * The deck's rarest non-land card, used as the gallery tile's hero art. Lands (even rare
 * duals) and basics are skipped — they make for dull, repetitive tiles across a collection.
 * Ties break toward the commander (the deck's identity), then the highest mana value (the
 * marquee bomb), then name for stable ordering. Returns null only for an empty / all-land /
 * uncatalogued deck, in which case the tile falls back to a colour-identity gradient.
 */
export function rarestCard<T extends DeckTileCard>(
  cards: Record<string, number>,
  catalog: Record<string, T>,
  commander: string | null,
): T | null {
  let best: T | null = null
  let bestRank = -1
  let bestIsCommander = false
  for (const [rawName, n] of Object.entries(cards)) {
    if (n <= 0) continue
    const name = rawName.split('#')[0] ?? rawName
    const c = catalog[name]
    if (!c || c.basicLand) continue
    if (c.cardTypes.some((t) => t.toUpperCase() === 'LAND')) continue
    const rank = RARITY_RANK[(c.rarity ?? 'COMMON').toUpperCase()] ?? 0
    const isCommander = commander != null && name === commander
    if (best === null || rank > bestRank) {
      best = c
      bestRank = rank
      bestIsCommander = isCommander
      continue
    }
    if (rank === bestRank && !bestIsCommander) {
      if (isCommander || c.cmc > best.cmc || (c.cmc === best.cmc && c.name < best.name)) {
        best = c
        bestIsCommander = isCommander
      }
    }
  }
  return best
}

/** The deck's colours, most-represented first — drives the tile's pips and gradient. */
export function deckColors(
  cards: Record<string, number>,
  catalog: Record<string, DeckTileCard>,
): string[] {
  const counts: Record<string, number> = {}
  for (const [name, n] of Object.entries(cards)) {
    if (n <= 0) continue
    const c = catalog[name.split('#')[0] ?? name]
    if (!c) continue
    for (const col of c.colors) counts[col] = (counts[col] ?? 0) + n
  }
  return Object.entries(counts)
    .sort((a, b) => b[1] - a[1])
    .map(([color]) => color)
}

/** Colour-identity gradient used when a deck has no catalogued non-land card to show art for. */
function deckBannerGradient(colors: string[]): string {
  if (colors.length === 0) {
    return 'linear-gradient(135deg, rgba(120,120,140,0.5), rgba(60,60,80,0.6))'
  }
  if (colors.length === 1) {
    const c = COLOR_DOT[colors[0]!] ?? '#888'
    return `linear-gradient(135deg, ${c}aa, ${c}55)`
  }
  const stops = colors.map((c, i) => {
    const hex = COLOR_DOT[c] ?? '#888'
    return `${hex} ${Math.round((i / (colors.length - 1)) * 100)}%`
  })
  return `linear-gradient(135deg, ${stops.join(', ')})`
}

// Deck-name tint by hero rarity — reuses the draft rarity palette so colours read
// consistently across the app, but lifts COMMON off near-black so names stay legible
// over the dark art scrim.
function deckNameColor(rarity: string | undefined): string {
  if (!rarity || rarity.toUpperCase() === 'COMMON') return '#eef1f6'
  return rarityColor(rarity)
}

// Per-format accent for the format chip. Keyed by upper-case format name; anything
// unmapped falls back to a neutral slate so new formats still render cleanly.
const FORMAT_ACCENT: Record<string, string> = {
  STANDARD: '#6aa3ff',
  PIONEER: '#c47dff',
  MODERN: '#8a7bff',
  LEGACY: '#7bd0ff',
  VINTAGE: '#ff9d57',
  PAUPER: '#9aa6b8',
  COMMANDER: '#e0b15a',
  BRAWL: '#e0b15a',
  STANDARD_BRAWL: '#e0b15a',
  HISTORIC: '#ff8aa8',
  CUSTOM_DECKS: '#7be0a8',
}

function formatAccent(format: string): string {
  return FORMAT_ACCENT[format.toUpperCase()] ?? '#9aa6b8'
}

function CloudIcon({ size = 12 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M19.35 10.04A7.49 7.49 0 0 0 12 4 7.5 7.5 0 0 0 5.35 8.04 5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z" />
    </svg>
  )
}

function MonitorIcon({ size = 12 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="2" y="3" width="20" height="14" rx="2" />
      <path d="M8 21h8M12 17v4" />
    </svg>
  )
}
