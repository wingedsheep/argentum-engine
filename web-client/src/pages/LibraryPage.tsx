/**
 * Decks & Cubes — everything the player has built, at `/library` (`?tab=cubes` for the cube shelf).
 *
 * Decks and cubes are built with one tool (the builder and its Deck builder | Cube builder switch), so
 * they live on one shelf too: two tabs over the same gallery, the same full-art tile, the same search,
 * sort and colour filter. A tile opens its deck or cube in the builder; the bar under it carries what
 * that kind of thing is *for* — a cube is played from here, a deck is played from a lobby.
 *
 * The first tile of each tab is the way to start a new one, so an empty shelf and a full one teach the
 * same thing. `/cubes` redirects here, and the builders' title links back.
 */
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CubePlayButton } from '@/components/cube/CubePlayButton'
import { boosterStyleInfo, cubeToText, isBoosterStyle, uniqueCubeName } from '@/components/cube/cubeModel'
import { ShareCubeDialog } from '@/components/cube/ShareCubeDialog'
import { DeckDetailsDialog } from '@/components/deck/DeckDetailsDialog'
import { DeckTile, DeckTileActionButton, deckTileModel } from '@/components/deck/DeckTile'
import { clearWorkingCopy } from '@/components/deckbuilder/BuilderModeSwitch'
import { COLOR_TOKENS, useCardCatalog } from '@/components/deckbuilder/browser'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { PageShell, pageStyles as p } from '@/components/ui/PageShell'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { cubeCardCount } from '@/store/cubeLibrary'
import { mergeCommanderIntoCards } from '@/store/deckLibrary'
import { type UnifiedCube, cubeToShared, useUnifiedCubes } from '@/store/useUnifiedCubes'
import { type UnifiedDeck, useUnifiedDecks } from '@/store/useUnifiedDecks'
import { useSaveDeck } from '@/store/useSaveDeck'
import { labelForFormat, useDeckLegalFormats } from '@/utils/deckLegality'
import styles from '@/components/library/Library.module.css'

export type LibraryTab = 'decks' | 'cubes'
type Sort = 'updated' | 'name' | 'size' | 'colors'

const LAST_TAB_KEY = 'argentum.library.tab'

function readLastTab(): LibraryTab {
  try {
    return window.localStorage.getItem(LAST_TAB_KEY) === 'cubes' ? 'cubes' : 'decks'
  } catch {
    return 'decks'
  }
}

/** One gallery row, whichever kind it is — what search, sort and the colour filter read. */
interface Shelved<T> {
  item: T
  name: string
  /** Searchable text besides the name: a deck's note, a cube's description. */
  blurb: string
  total: number
  colors: string[]
  hero: CardSummary | null
  updatedAt: number
}

export function LibraryPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const tabParam = searchParams.get('tab')
  const tab: LibraryTab = tabParam === 'cubes' || tabParam === 'decks' ? tabParam : readLastTab()

  const decks = useUnifiedDecks()
  const cubes = useUnifiedCubes()
  const { index, loading: catalogLoading } = useCardCatalog()

  const [filter, setFilter] = useState('')
  const [sort, setSort] = useState<Sort>('updated')
  const [colorFilter, setColorFilter] = useState<ReadonlySet<string>>(new Set())

  useEffect(() => {
    try {
      window.localStorage.setItem(LAST_TAB_KEY, tab)
    } catch {
      // Storage blocked: the shelf just opens on Decks next time.
    }
  }, [tab])

  const selectTab = (next: LibraryTab) => {
    if (next === tab) return
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev)
        params.set('tab', next)
        return params
      },
      { replace: true },
    )
  }

  const deckRows = useMemo<Shelved<UnifiedDeck>[]>(
    () =>
      decks.decks.map((deck) => {
        const { total, colors, hero } = deckTileModel(deck, index)
        return { item: deck, name: deck.name, blurb: deck.note ?? '', total, colors, hero, updatedAt: deck.updatedAt }
      }),
    [decks.decks, index],
  )

  const cubeRows = useMemo<Shelved<UnifiedCube>[]>(
    () =>
      cubes.cubes.map((cube) => {
        const cards: Record<string, number> = {}
        for (const entry of cube.cards) cards[entry.name] = (cards[entry.name] ?? 0) + entry.count
        const { colors, hero } = deckTileModel({ cards }, index)
        return {
          item: cube,
          name: cube.name,
          blurb: cube.description ?? '',
          total: cubeCardCount(cube.cards),
          colors,
          hero,
          updatedAt: cube.updatedAt,
        }
      }),
    [cubes.cubes, index],
  )

  const shelve = useCallback(
    <T,>(rows: Shelved<T>[]): Shelved<T>[] => {
      const needle = filter.trim().toLowerCase()
      const kept = rows.filter((row) => {
        if (needle && !row.name.toLowerCase().includes(needle) && !row.blurb.toLowerCase().includes(needle)) return false
        if (colorFilter.size === 0) return true
        for (const key of colorFilter) {
          if (key === 'COLORLESS' ? row.colors.length === 0 : row.colors.includes(key)) return true
        }
        return false
      })
      return kept.sort((a, b) => {
        switch (sort) {
          case 'name':
            return a.name.localeCompare(b.name)
          case 'size':
            return b.total - a.total || a.name.localeCompare(b.name)
          case 'colors':
            return colorBucket(a.colors) - colorBucket(b.colors) || a.name.localeCompare(b.name)
          default:
            return b.updatedAt - a.updatedAt
        }
      })
    },
    [filter, colorFilter, sort],
  )

  const toggleColor = (key: string) =>
    setColorFilter((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })

  const newDeck = () => {
    clearWorkingCopy('deck')
    navigate('/deckbuilder')
  }
  const newCube = (from?: 'set' | 'paste') => {
    clearWorkingCopy('cube')
    navigate(from ? `/cubes/new?from=${from}` : '/cubes/new')
  }

  const counts = { decks: decks.decks.length, cubes: cubes.cubes.length }
  const isLoggedIn = decks.isLoggedIn
  const filtering = filter.trim() !== '' || colorFilter.size > 0

  return (
    <PageShell title="Decks & Cubes" width="wide">
      <header className={styles.hero}>
        <div>
          <h1 className={p.h1}>Decks &amp; cubes</h1>
          <p className={p.lede}>
            Everything you build, on one shelf. Decks you bring to a game; cubes you draft or open as sealed.
          </p>
        </div>
        <button
          type="button"
          className={`${p.buttonPrimary} ${styles.heroAction}`}
          onClick={() => (tab === 'decks' ? newDeck() : newCube())}
        >
          <PlusIcon /> {tab === 'decks' ? 'New deck' : 'New cube'}
        </button>
      </header>

      <div className={styles.bar}>
        <div className={styles.tabs} role="tablist" aria-label="Library">
          {(['decks', 'cubes'] as const).map((t) => (
            <button
              key={t}
              type="button"
              role="tab"
              id={`library-tab-${t}`}
              aria-controls="library-panel"
              aria-selected={tab === t}
              className={styles.tab}
              onClick={() => selectTab(t)}
            >
              {t === 'decks' ? <DeckIcon /> : <CubeIcon />}
              {t === 'decks' ? 'Decks' : 'Cubes'}
              <span className={styles.tabCount}>{counts[t]}</span>
            </button>
          ))}
        </div>

        <div className={styles.filters}>
          <label className={styles.search}>
            <SearchIcon />
            <input
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder={tab === 'decks' ? 'Search decks…' : 'Search cubes…'}
              aria-label={tab === 'decks' ? 'Search decks by name or note' : 'Search cubes by name or description'}
            />
          </label>
          <div className={styles.colors} role="group" aria-label="Filter by colour">
            {COLOR_TOKENS.map(({ label, key }) => (
              <button
                key={key}
                type="button"
                className={styles.colorChip}
                aria-pressed={colorFilter.has(key)}
                aria-label={key.toLowerCase()}
                title={key.charAt(0) + key.slice(1).toLowerCase()}
                onClick={() => toggleColor(key)}
              >
                <ManaSymbol symbol={label} size={18} />
              </button>
            ))}
            <button
              type="button"
              className={styles.colorChip}
              aria-pressed={colorFilter.has('COLORLESS')}
              aria-label="colourless"
              title="Colourless"
              onClick={() => toggleColor('COLORLESS')}
            >
              <ManaSymbol symbol="C" size={18} />
            </button>
          </div>
          <select
            className={styles.sort}
            value={sort}
            onChange={(e) => setSort(e.target.value as Sort)}
            aria-label="Sort"
          >
            <option value="updated">Recently updated</option>
            <option value="name">Name (A→Z)</option>
            <option value="size">Card count</option>
            <option value="colors">Colour</option>
          </select>
        </div>
      </div>

      <section id="library-panel" role="tabpanel" aria-labelledby={`library-tab-${tab}`} className={styles.panel}>
        {tab === 'decks' ? (
          <DeckShelf
            rows={shelve(deckRows)}
            library={decks}
            ready={decks.settled}
            filtering={filtering}
            onNew={newDeck}
            onClearFilters={() => {
              setFilter('')
              setColorFilter(new Set())
            }}
          />
        ) : (
          <CubeShelf
            rows={shelve(cubeRows)}
            library={cubes}
            index={index}
            catalogReady={!catalogLoading}
            filtering={filtering}
            onNew={newCube}
            onClearFilters={() => {
              setFilter('')
              setColorFilter(new Set())
            }}
          />
        )}
      </section>

      {!isLoggedIn && counts[tab] > 0 && (
        <p className={styles.footnote}>
          <MonitorIcon /> Saved in this browser. Sign in to keep your {tab} in your account
          {tab === 'cubes' ? ' and share them by link' : ' on every device'}.
        </p>
      )}
    </PageShell>
  )
}

// ---------------------------------------------------------------------------
// Decks
// ---------------------------------------------------------------------------

function DeckShelf({
  rows,
  library,
  ready,
  filtering,
  onNew,
  onClearFilters,
}: {
  rows: Shelved<UnifiedDeck>[]
  library: ReturnType<typeof useUnifiedDecks>
  ready: boolean
  filtering: boolean
  onNew: () => void
  onClearFilters: () => void
}) {
  const navigate = useNavigate()
  const { save } = useSaveDeck()
  const [editing, setEditing] = useState<UnifiedDeck | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Server-authoritative legality, batched into one request; the tile shows one format chip.
  const legalityInput = useMemo(() => {
    const out: Record<string, Record<string, number>> = {}
    for (const { item } of rows) out[item.id] = mergeCommanderIntoCards(item.cards, item.commander ?? null)
    return out
  }, [rows])
  const legality = useDeckLegalFormats(legalityInput)

  const open = (deck: UnifiedDeck) => navigate(`/deckbuilder/${encodeURIComponent(deck.id)}`)

  async function duplicate(deck: UnifiedDeck) {
    setError(null)
    try {
      const { id: _id, updatedAt: _updatedAt, online: _online, cloudId: _cloudId, ...rest } = deck
      const name = uniqueCubeName(`${deck.name} copy`, library.decks.map((d) => d.name))
      await save({ ...rest, name })
      library.reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not duplicate the deck')
    }
  }

  async function remove(deck: UnifiedDeck) {
    setError(null)
    try {
      await library.removeDeck(deck)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete the deck')
    }
  }

  return (
    <>
      {error && <p className={styles.error} role="alert">{error}</p>}
      <ul className={styles.grid}>
        {!filtering && (
          <li>
            <StartTile
              title="New deck"
              body="Search every implemented card, or start from a list or a ready-made deck."
              onClick={onNew}
              links={[
                { label: 'Import a list', onClick: () => navigate('/deckbuilder?open=import') },
                { label: 'Start from an example', onClick: () => navigate('/deckbuilder?open=examples') },
              ]}
            />
          </li>
        )}
        {!ready && rows.length === 0
          ? <SkeletonTiles />
          : rows.map(({ item: deck, total, colors, hero }) => {
              const legal = legality[deck.id] ?? []
              const shownFormat = deck.format ?? legal[0] ?? null
              return (
                <ShelfTile
                  key={deck.id}
                  name={deck.name}
                  confirmText="Delete this deck?"
                  onConfirmDelete={() => void remove(deck)}
                  tile={(askDelete) => (
                    <DeckTile
                      name={deck.name}
                      note={deck.note}
                      total={total}
                      colors={colors}
                      hero={hero}
                      format={shownFormat}
                      formatTitle={
                        shownFormat
                          ? deck.format
                            ? `Saved as ${labelForFormat(shownFormat)}`
                            : `Legal in ${labelForFormat(shownFormat)}`
                          : undefined
                      }
                      storage={deck.online ? 'cloud' : 'local'}
                      title={`Open ${deck.name} in the deck builder`}
                      onClick={() => open(deck)}
                      actions={
                        <>
                          <DeckTileActionButton onClick={() => setEditing(deck)} title="Rename or edit note" ariaLabel={`Rename ${deck.name}`}>
                            <PencilIcon />
                          </DeckTileActionButton>
                          <DeckTileActionButton onClick={() => void duplicate(deck)} title="Duplicate" ariaLabel={`Duplicate ${deck.name}`}>
                            <CopyIcon />
                          </DeckTileActionButton>
                          <DeckTileActionButton onClick={askDelete} title="Delete" ariaLabel={`Delete ${deck.name}`} danger>
                            <TrashIcon />
                          </DeckTileActionButton>
                        </>
                      }
                    />
                  )}
                  footer={
                    <>
                      <span className={styles.updated}>{updatedLabel(deck.updatedAt)}</span>
                      <button type="button" className={styles.footerButton} onClick={() => open(deck)}>
                        Edit deck
                      </button>
                    </>
                  }
                />
              )
            })}
      </ul>
      {ready && rows.length === 0 && filtering && <NoMatches noun="decks" onClear={onClearFilters} />}
      {editing && (
        <DeckDetailsDialog
          name={editing.name}
          note={editing.note ?? ''}
          onCancel={() => setEditing(null)}
          onSave={({ name, note }) => {
            const deck = editing
            setEditing(null)
            void library.updateDetails(deck, { name, note }).catch((err: unknown) =>
              setError(err instanceof Error ? err.message : 'Could not rename the deck'),
            )
          }}
        />
      )}
    </>
  )
}

// ---------------------------------------------------------------------------
// Cubes
// ---------------------------------------------------------------------------

function CubeShelf({
  rows,
  library,
  index,
  catalogReady,
  filtering,
  onNew,
  onClearFilters,
}: {
  rows: Shelved<UnifiedCube>[]
  library: ReturnType<typeof useUnifiedCubes>
  index: Record<string, CardSummary>
  catalogReady: boolean
  filtering: boolean
  onNew: (from?: 'set' | 'paste') => void
  onClearFilters: () => void
}) {
  const navigate = useNavigate()
  const [sharing, setSharing] = useState<UnifiedCube | null>(null)
  const [error, setError] = useState<string | null>(null)
  const known = useMemo(() => new Set(Object.keys(index).map((n) => n.toLowerCase())), [index])

  const edit = (cube: UnifiedCube) => navigate(`/cubes/edit/${encodeURIComponent(cube.id)}`)

  async function duplicate(cube: UnifiedCube) {
    setError(null)
    try {
      const name = uniqueCubeName(`${cube.name} copy`, library.cubes.map((c) => c.name))
      await library.saveCube({ ...cubeToShared(cube), name })
      library.reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not duplicate the cube')
    }
  }

  async function remove(cube: UnifiedCube) {
    setError(null)
    try {
      await library.removeCube(cube)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete the cube')
    }
  }

  return (
    <>
      {error && <p className={styles.error} role="alert">{error}</p>}
      <ul className={styles.grid}>
        {!filtering && (
          <li>
            <StartTile
              title="New cube"
              body="Your own pool to draft or open as sealed — a classic shuffled cube, or a whole set opened as real boosters."
              onClick={() => onNew()}
              links={[
                { label: 'Start from a set', onClick: () => onNew('set') },
                { label: 'Paste a list', onClick: () => onNew('paste') },
              ]}
            />
          </li>
        )}
        {!library.ready && rows.length === 0
          ? <SkeletonTiles />
          : rows.map(({ item: cube, total, colors, hero }) => {
              const unresolved = catalogReady ? cube.cards.filter((e) => !known.has(e.name.toLowerCase())).length : 0
              const blockedReason = total === 0
                ? 'This cube is empty'
                : unresolved > 0
                  ? `${unresolved} card${unresolved === 1 ? '' : 's'} in this cube aren’t implemented yet — edit it to drop them`
                  : null
              const style = boosterStyleInfo(cube.boosterStyle)
              return (
                <ShelfTile
                  key={cube.id}
                  name={cube.name}
                  confirmText={cube.shareCode ? 'Delete this cube? Its share link stops working.' : 'Delete this cube?'}
                  onConfirmDelete={() => void remove(cube)}
                  tile={(askDelete) => (
                    <DeckTile
                      name={cube.name}
                      description={cube.description}
                      total={total}
                      colors={colors}
                      hero={hero}
                      storage={cube.online ? 'cloud' : 'local'}
                      title={`Open ${cube.name} in the cube builder`}
                      onClick={() => edit(cube)}
                      chips={
                        <>
                          <span
                            className={styles.chip}
                            data-kind={isBoosterStyle(cube.boosterStyle) ? 'booster' : 'shuffled'}
                            title={isBoosterStyle(cube.boosterStyle) ? 'Packs are printed by rarity, like a real set' : 'Packs are dealt from the shuffled cube'}
                          >
                            {style.label}
                          </span>
                          {cube.shareCode && <span className={styles.chip} data-kind="shared" title="Anyone with the link can view and copy it">Shared</span>}
                          {unresolved > 0 && (
                            <span className={styles.chip} data-kind="warning" title={blockedReason ?? undefined}>
                              {unresolved} missing
                            </span>
                          )}
                        </>
                      }
                      actions={
                        <>
                          <DeckTileActionButton onClick={() => setSharing(cube)} title="Share" ariaLabel={`Share ${cube.name}`}>
                            <ShareIcon />
                          </DeckTileActionButton>
                          <DeckTileActionButton onClick={() => void duplicate(cube)} title="Duplicate" ariaLabel={`Duplicate ${cube.name}`}>
                            <CopyIcon />
                          </DeckTileActionButton>
                          <DeckTileActionButton onClick={askDelete} title="Delete" ariaLabel={`Delete ${cube.name}`} danger>
                            <TrashIcon />
                          </DeckTileActionButton>
                        </>
                      }
                    />
                  )}
                  footer={
                    <>
                      <span className={styles.updated}>{updatedLabel(cube.updatedAt)}</span>
                      <button type="button" className={styles.footerButton} onClick={() => edit(cube)}>
                        Edit
                      </button>
                      <CubePlayButton cube={cubeToShared(cube)} blockedReason={blockedReason} className={styles.footerPlay} panelAlign="end" />
                    </>
                  }
                />
              )
            })}
      </ul>
      {library.ready && rows.length === 0 && filtering && <NoMatches noun="cubes" onClear={onClearFilters} />}
      {sharing && (
        <ShareCubeDialog
          saved={sharing}
          dirty={false}
          isLoggedIn={library.isLoggedIn}
          text={cubeToText(sharing)}
          // A device-only cube moves into the account on its first share; `saveCube` routes it there.
          onSave={() => library.saveCube(cubeToShared(sharing))}
          share={library.share}
          unshare={library.unshare}
          onClose={() => setSharing(null)}
        />
      )}
    </>
  )
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

/** A gallery tile plus its footer bar, and the inline delete confirmation that replaces the bar. */
function ShelfTile({
  name,
  tile,
  footer,
  confirmText,
  onConfirmDelete,
}: {
  name: string
  tile: (askDelete: () => void) => ReactNode
  footer: ReactNode
  confirmText: string
  onConfirmDelete: () => void
}) {
  const [confirming, setConfirming] = useState(false)
  return (
    <li className={styles.item} data-confirming={confirming}>
      {tile(() => setConfirming(true))}
      {confirming ? (
        <div className={styles.confirm} role="alert" aria-label={`Delete ${name}`}>
          <span>{confirmText}</span>
          <button type="button" className={styles.footerButton} onClick={() => setConfirming(false)} autoFocus>
            Keep
          </button>
          <button type="button" className={styles.dangerButton} onClick={onConfirmDelete}>
            Delete
          </button>
        </div>
      ) : (
        <div className={styles.footer}>{footer}</div>
      )}
    </li>
  )
}

/** The first tile of a shelf: start a new one, with the shortcuts that kind of thing has. */
function StartTile({
  title,
  body,
  onClick,
  links,
}: {
  title: string
  body: string
  onClick: () => void
  links: Array<{ label: string; onClick: () => void }>
}) {
  return (
    <div className={styles.start}>
      <button type="button" className={styles.startMain} onClick={onClick}>
        <span className={styles.startIcon} aria-hidden>
          <PlusIcon size={22} />
        </span>
        <span className={styles.startTitle}>{title}</span>
        <span className={styles.startBody}>{body}</span>
      </button>
      <div className={styles.startLinks}>
        {links.map((link) => (
          <button key={link.label} type="button" className={styles.startLink} onClick={link.onClick}>
            {link.label} <span aria-hidden>→</span>
          </button>
        ))}
      </div>
    </div>
  )
}

function SkeletonTiles() {
  return (
    <>
      {[0, 1, 2].map((i) => (
        <li key={i} className={styles.skeleton} aria-hidden />
      ))}
    </>
  )
}

function NoMatches({ noun, onClear }: { noun: string; onClear: () => void }) {
  return (
    <div className={styles.noMatches}>
      No {noun} match the search.{' '}
      <button type="button" className={styles.startLink} onClick={onClear}>
        Clear filters
      </button>
    </div>
  )
}

function colorBucket(colors: string[]): number {
  if (colors.length === 0) return 99
  if (colors.length > 1) return 90 + colors.length
  return ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN'].indexOf(colors[0]!)
}

/** "Edited 3 days ago" — coarse on purpose; the tile is no place for a timestamp. */
export function updatedLabel(updatedAt: number, now: number = Date.now()): string {
  if (!updatedAt) return ''
  const minutes = Math.max(0, Math.round((now - updatedAt) / 60_000))
  if (minutes < 1) return 'Edited just now'
  if (minutes < 60) return `Edited ${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `Edited ${hours} h ago`
  const days = Math.round(hours / 24)
  if (days === 1) return 'Edited yesterday'
  if (days < 30) return `Edited ${days} days ago`
  return `Edited ${new Date(updatedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}`
}

// ---------------------------------------------------------------------------
// Icons
// ---------------------------------------------------------------------------

function Svg({ size = 15, children }: { size?: number; children: ReactNode }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden focusable="false">
      {children}
    </svg>
  )
}
const PlusIcon = ({ size }: { size?: number }) => <Svg size={size ?? 16}><path d="M12 5v14M5 12h14" /></Svg>
const SearchIcon = () => <Svg><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></Svg>
const PencilIcon = () => <Svg size={14}><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" /></Svg>
const CopyIcon = () => <Svg size={14}><rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15V5a2 2 0 0 1 2-2h10" /></Svg>
const TrashIcon = () => <Svg size={14}><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" /></Svg>
const ShareIcon = () => <Svg size={14}><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4" /></Svg>
const MonitorIcon = () => <Svg size={13}><rect x="2" y="3" width="20" height="14" rx="2" /><path d="M8 21h8M12 17v4" /></Svg>
const DeckIcon = () => (
  <Svg size={16}>
    <rect x="7" y="3" width="12" height="16" rx="2" />
    <path d="M4 7v12a2 2 0 0 0 2 2h9" />
  </Svg>
)
const CubeIcon = () => (
  <Svg size={16}>
    <path d="M12 2 3 7v10l9 5 9-5V7Z" />
    <path d="m3 7 9 5 9-5M12 12v10" />
  </Svg>
)
