/**
 * CubeEditor — build and curate a cube (a hand-picked card pool used as a limited pack source).
 *
 * A cube is a list of card *names* with counts, so this editor never resolves printings and never
 * decides what a pack contains: it edits a list and hands it back. The lobby sends the expanded list
 * to the server (`UpdateLobbySettings.cubeCards`), which resolves it against the card registry and
 * remains the authority on packs, capacity and legality.
 *
 * Two authoring paths, per the design: **paste a list** (reusing the deckbuilder's
 * {@link parseArenaDeckList} + {@link resolveAgainstCatalog}, including its coverage readout) and
 * **search-and-add** against `/api/cards` using the deckbuilder's query language, so `c:red t:creature
 * cmc<=3` works here too.
 *
 * Unimplemented cards are the one cube-specific rule: a cube with unresolvable names is *unplayable*
 * (the server rejects it wholesale), so they're surfaced in red with a one-click "drop them" action and
 * "Use this cube" stays disabled until the list is clean. Saving a still-dirty cube is allowed — sets
 * ship continuously, and a cube that names a card landing next month is a legitimate thing to keep.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { parseQuery } from '@/components/deckbuilder/cardFilter'
import { parseArenaDeckList, resolveAgainstCatalog } from '@/components/deckbuilder/parseArenaDeck'
import type { SharedCube } from '@/api/account'
import { DEFAULT_CUBE_PACK_SIZE, type CubeEntry, cubeCardCount } from '@/store/cubeLibrary'
import { type UnifiedCube, useUnifiedCubes } from '@/store/useUnifiedCubes'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'
import { ManaCost, ManaSymbol } from '@/components/ui/ManaSymbols'
import { useDfcHoverFlip } from '@/components/ui/useDfcHoverFlip'
import { landscapeImageRotateDeg } from '@/utils/cardImages'
import styles from './CubeEditor.module.css'

/** A set the cube can take its basic-land art from. */
export interface BasicLandSetOption {
  readonly code: string
  readonly name: string
}

interface CubeEditorProps {
  /** Cube to edit, or null to start a new one. */
  readonly cube: UnifiedCube | null
  /** Sets offered as the basic-land art source (a cube has no basics of its own). */
  readonly availableSets: readonly BasicLandSetOption[]
  readonly onClose: () => void
  /**
   * Called when the host commits the cube to the lobby. Only reachable once every name resolves —
   * an unresolved cube can't start a game.
   */
  readonly onUse?: (cube: SharedCube) => void
}

/** Module-level cache so re-opening the editor doesn't refetch the whole catalog. */
let cardCache: CardSummary[] | null = null

/** Deck-list ordering for the cube view: the groups a curator actually thinks in. */
const TYPE_ORDER = ['Creature', 'Planeswalker', 'Instant', 'Sorcery', 'Artifact', 'Enchantment', 'Battle', 'Land'] as const

/**
 * Catalog colours arrive as enum names (`BLUE`), not WUBRG letters, so the pip has to be mapped
 * rather than taken from the first character — `BLUE` and `BLACK` both start with B.
 */
const COLOR_GROUPS = [
  { key: 'W', enumName: 'WHITE', label: 'White' },
  { key: 'U', enumName: 'BLUE', label: 'Blue' },
  { key: 'B', enumName: 'BLACK', label: 'Black' },
  { key: 'R', enumName: 'RED', label: 'Red' },
  { key: 'G', enumName: 'GREEN', label: 'Green' },
] as const

const COLOR_KEY_BY_ENUM: Record<string, string> = Object.fromEntries(
  COLOR_GROUPS.map((g) => [g.enumName, g.key]),
)

function primaryType(card: CardSummary | undefined): string {
  if (!card) return 'Unknown'
  const types = card.cardTypes.map((t) => t.charAt(0) + t.slice(1).toLowerCase())
  for (const candidate of TYPE_ORDER) {
    if (types.includes(candidate)) return candidate
  }
  return types[0] ?? 'Other'
}

export function CubeEditor({ cube, availableSets, onClose, onUse }: CubeEditorProps) {
  const { cubes, saveCube } = useUnifiedCubes()

  const [name, setName] = useState(cube?.name ?? 'My Cube')
  const [entries, setEntries] = useState<CubeEntry[]>(() => cube?.cards.map((e) => ({ ...e })) ?? [])
  const [packSize, setPackSize] = useState(cube?.packSize ?? DEFAULT_CUBE_PACK_SIZE)
  const [basicLandSetCode, setBasicLandSetCode] = useState(
    cube?.basicLandSetCode ?? availableSets[0]?.code ?? '',
  )
  const [allCards, setAllCards] = useState<CardSummary[]>(cardCache ?? [])
  const [query, setQuery] = useState('')
  const [showImport, setShowImport] = useState(entries.length === 0)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  // Hover preview, same affordance as the deckbuilder and draft overlays (press F to flip a DFC).
  const [hoveredCard, setHoveredCard] = useState<CardSummary | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)

  const hoverable = hoveredCard
    ? { ...hoveredCard, imageUri: hoveredCard.imageUri ?? null }
    : null
  const dfc = useDfcHoverFlip(hoverable)
  const resetDfcFlip = dfc.resetFlip

  const handleHover = useCallback(
    (card: CardSummary | null, e?: React.MouseEvent) => {
      setHoveredCard((prev) => {
        if (prev?.name !== card?.name) resetDfcFlip()
        return card
      })
      setHoverPos(card && e ? { x: e.clientX, y: e.clientY } : null)
    },
    [resetDfcFlip],
  )

  // Fetch the catalog once (cached across mounts), the same way the ban-list editor does.
  useEffect(() => {
    if (cardCache) return
    let cancelled = false
    fetch('/api/cards')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: CardSummary[]) => {
        if (cancelled) return
        cardCache = list
        setAllCards(list)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  const byName = useMemo(() => {
    const map = new Map<string, CardSummary>()
    for (const card of allCards) map.set(card.name.toLowerCase(), card)
    return map
  }, [allCards])

  const catalogReady = allCards.length > 0
  const totalCards = cubeCardCount(entries)
  /** Names the catalog doesn't know: the cube is unplayable until these are gone. */
  const unresolved = useMemo(
    () => (catalogReady ? entries.filter((e) => !byName.has(e.name.toLowerCase())) : []),
    [entries, byName, catalogReady],
  )

  const inCube = useMemo(() => new Set(entries.map((e) => e.name.toLowerCase())), [entries])

  const searchResults = useMemo(() => {
    const trimmed = query.trim()
    if (!trimmed || !catalogReady) return []
    const predicate = parseQuery(trimmed)
    return allCards
      .filter((c) => !c.basicLand && !inCube.has(c.name.toLowerCase()) && predicate(c))
      .sort((a, b) => a.name.localeCompare(b.name))
      .slice(0, 60)
  }, [query, allCards, inCube, catalogReady])

  function setCount(cardName: string, count: number) {
    setEntries((prev) => {
      if (count <= 0) return prev.filter((e) => e.name !== cardName)
      return prev.map((e) => (e.name === cardName ? { ...e, count } : e))
    })
  }

  function addCard(cardName: string) {
    setEntries((prev) =>
      prev.some((e) => e.name.toLowerCase() === cardName.toLowerCase())
        ? prev
        : [...prev, { name: cardName, count: 1 }],
    )
  }

  function dropUnresolved() {
    const misses = new Set(unresolved.map((e) => e.name))
    setEntries((prev) => prev.filter((e) => !misses.has(e.name)))
  }

  /** Group the cube by primary card type for the curation view, each group name-sorted. */
  const grouped = useMemo(() => {
    const groups = new Map<string, CubeEntry[]>()
    for (const entry of entries) {
      const key = catalogReady ? primaryType(byName.get(entry.name.toLowerCase())) : 'Cards'
      const bucket = groups.get(key)
      if (bucket) bucket.push(entry)
      else groups.set(key, [entry])
    }
    const order = [...TYPE_ORDER, 'Other', 'Unknown', 'Cards'] as readonly string[]
    return [...groups.entries()]
      .sort((a, b) => {
        const ai = order.indexOf(a[0])
        const bi = order.indexOf(b[0])
        return (ai < 0 ? order.length : ai) - (bi < 0 ? order.length : bi)
      })
      .map(([key, list]) => [key, [...list].sort((x, y) => x.name.localeCompare(y.name))] as const)
  }, [entries, byName, catalogReady])

  /** Colour spread + mana curve — the two things a curator balances a cube on. */
  const stats = useMemo(() => {
    const colors: Record<string, number> = { W: 0, U: 0, B: 0, R: 0, G: 0, C: 0, M: 0 }
    const curve = [0, 0, 0, 0, 0, 0, 0, 0]
    for (const entry of entries) {
      const card = byName.get(entry.name.toLowerCase())
      if (!card) continue
      const identity = card.colorIdentity ?? []
      if (identity.length === 0) colors.C = (colors.C ?? 0) + entry.count
      else if (identity.length > 1) colors.M = (colors.M ?? 0) + entry.count
      else {
        const key = COLOR_KEY_BY_ENUM[identity[0]!]
        if (key) colors[key] = (colors[key] ?? 0) + entry.count
      }
      if (!card.cardTypes.includes('LAND')) {
        const bucket = Math.min(Math.max(Math.round(card.cmc), 0), curve.length - 1)
        curve[bucket] = (curve[bucket] ?? 0) + entry.count
      }
    }
    return { colors, curve, curveMax: Math.max(1, ...curve) }
  }, [entries, byName])

  const canUse = totalCards > 0 && unresolved.length === 0 && basicLandSetCode !== ''

  const shared: SharedCube = {
    name: name.trim() || 'Untitled cube',
    cards: entries,
    basicLandSetCode,
    packSize,
  }

  /** Route the save back to the copy this editor opened, or to a same-named cube in the library. */
  const saveTarget = cube ?? cubes.find((c) => c.name.toLowerCase() === shared.name.toLowerCase())

  async function handleSave(): Promise<boolean> {
    setSaveError(null)
    setSaving(true)
    try {
      await saveCube(shared, saveTarget)
      return true
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Failed to save cube')
      return false
    } finally {
      setSaving(false)
    }
  }

  // Portalled to <body>: the lobby panel this opens from sits inside a `backdrop-filter` ancestor,
  // which is a containing block for `position: fixed` — a full-screen dialog rendered in place would
  // size itself to the lobby's scroll height instead of the viewport.
  return createPortal(
    <>
      <div className={styles.backdrop} onClick={onClose} />
      <div className={styles.dialog} role="dialog" aria-label="Cube editor">
        <div className={styles.header}>
          <label className={`${styles.field} ${styles.nameField}`}>
            <span className={styles.fieldLabel}>Cube name</span>
            <input
              className={styles.input}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="My Cube"
            />
          </label>
          <label className={styles.field}>
            <span className={styles.fieldLabel}>Pack size</span>
            <input
              className={`${styles.input} ${styles.numberInput}`}
              type="number"
              min={1}
              max={30}
              value={packSize}
              onChange={(e) => setPackSize(Math.max(1, Math.min(30, Number(e.target.value) || 1)))}
            />
          </label>
          <label className={styles.field}>
            <span className={styles.fieldLabel}>Basic land art</span>
            <select
              className={styles.select}
              value={basicLandSetCode}
              onChange={(e) => setBasicLandSetCode(e.target.value)}
            >
              {availableSets.map((set) => (
                <option key={set.code} value={set.code}>
                  {set.name}
                </option>
              ))}
            </select>
          </label>
          <div className={styles.headerActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => setShowImport((v) => !v)}
            >
              {showImport ? 'Search cards' : 'Paste list'}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={saving || totalCards === 0}
              onClick={() => void handleSave()}
            >
              {saving ? 'Saving…' : 'Save'}
            </button>
            {onUse && (
              <button
                type="button"
                className={styles.primaryButton}
                disabled={!canUse || saving}
                title={
                  canUse
                    ? undefined
                    : unresolved.length > 0
                      ? `${unresolved.length} cards in this cube aren't implemented yet`
                      : 'Add cards to the cube first'
                }
                onClick={async () => {
                  if (!canUse) return
                  if (await handleSave()) onUse(shared)
                }}
              >
                Use this cube
              </button>
            )}
            <button type="button" className={styles.secondaryButton} onClick={onClose}>
              Close
            </button>
          </div>
        </div>

        {unresolved.length > 0 && (
          <div className={styles.warning}>
            <span>
              <strong>{unresolved.length}</strong> of {totalCards} cards aren&apos;t implemented yet, so
              this cube can&apos;t be played: {unresolved.slice(0, 6).map((e) => e.name).join(', ')}
              {unresolved.length > 6 ? `, +${unresolved.length - 6} more` : ''}
            </span>
            <button type="button" className={styles.secondaryButton} onClick={dropUnresolved}>
              Drop the {unresolved.length} unimplemented card{unresolved.length === 1 ? '' : 's'}
            </button>
          </div>
        )}

        {saveError && <div className={styles.warning}>{saveError}</div>}

        <div className={styles.body}>
          <div className={styles.column}>
            {showImport ? (
              <ImportPane catalog={allCards} onReplace={setEntries} onSuggestName={setName} />
            ) : (
              <>
                <div className={styles.columnHeader}>
                  <span className={styles.columnHeaderTitle}>Add cards</span>
                  <span>{searchResults.length > 0 ? `${searchResults.length} shown` : ''}</span>
                </div>
                <div className={styles.searchRow}>
                  <input
                    className={styles.input}
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search — e.g. c:red t:creature cmc<=3"
                  />
                </div>
                <div className={styles.scroll}>
                  {!catalogReady ? (
                    <p className={styles.hint}>Loading card catalog…</p>
                  ) : searchResults.length === 0 ? (
                    <p className={styles.hint}>
                      Search by name or with the deckbuilder&apos;s query language:{' '}
                      <code>c:wu</code>, <code>t:instant</code>, <code>cmc&gt;=5</code>,{' '}
                      <code>s:BLB</code>, <code>o:draw</code>.
                    </p>
                  ) : (
                    <ul className={styles.rowList}>
                      {searchResults.map((card) => (
                        <li key={card.name} className={styles.row}>
                          <button
                            type="button"
                            className={styles.rowButton}
                            onClick={() => addCard(card.name)}
                            onMouseEnter={(e) => handleHover(card, e)}
                            onMouseMove={(e) => handleHover(card, e)}
                            onMouseLeave={() => handleHover(null)}
                          >
                            <span className={styles.rowName}>{card.name}</span>
                            <span className={styles.rowMeta}>
                              {card.manaCost ? <ManaCost cost={card.manaCost} size={12} /> : '—'}
                            </span>
                            <span className={styles.rowSet}>{card.setCode ?? ''}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </>
            )}
          </div>

          <div className={styles.column}>
            <div className={styles.columnHeader}>
              <span className={styles.columnHeaderTitle}>{totalCards} cards in cube</span>
              {entries.length > 0 && (
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => setEntries([])}
                >
                  Clear
                </button>
              )}
            </div>
            <div className={styles.scroll}>
              {entries.length === 0 ? (
                <p className={styles.hint}>
                  Empty cube. Paste a list or search for cards to add. A 360-card cube seats 8 players
                  at 3 packs of 15.
                </p>
              ) : (
                grouped.map(([group, list]) => (
                  <div key={group}>
                    <div className={styles.groupHeader}>
                      <span>{group}</span>
                      <span>{cubeCardCount(list)}</span>
                    </div>
                    <ul className={styles.rowList}>
                      {list.map((entry) => {
                        const card = byName.get(entry.name.toLowerCase())
                        const missing = catalogReady && !card
                        return (
                          <li key={entry.name} className={styles.row}>
                            <span
                              className={`${styles.rowName} ${missing ? styles.rowMissing : ''}`}
                              title={missing ? 'Not implemented yet' : entry.name}
                              onMouseEnter={(e) => card && handleHover(card, e)}
                              onMouseMove={(e) => card && handleHover(card, e)}
                              onMouseLeave={() => handleHover(null)}
                            >
                              {entry.name}
                            </span>
                            <span className={styles.rowMeta}>
                              {card?.manaCost ? <ManaCost cost={card.manaCost} size={12} /> : null}
                            </span>
                            <div className={styles.countControls}>
                              <button
                                type="button"
                                className={styles.stepButton}
                                aria-label={`One fewer ${entry.name}`}
                                onClick={() => setCount(entry.name, entry.count - 1)}
                              >
                                −
                              </button>
                              <span className={styles.count}>{entry.count}</span>
                              <button
                                type="button"
                                className={styles.stepButton}
                                aria-label={`One more ${entry.name}`}
                                onClick={() => setCount(entry.name, entry.count + 1)}
                              >
                                +
                              </button>
                            </div>
                          </li>
                        )
                      })}
                    </ul>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        <div className={styles.summary}>
          <div className={styles.summaryGroup}>
            <span className={styles.summaryLabel}>Colours</span>
            {COLOR_GROUPS.map((group) => (
              <span key={group.key} className={styles.colorStat} title={`${group.label}: ${stats.colors[group.key] ?? 0}`}>
                <ManaSymbol symbol={group.key} size={13} />
                {stats.colors[group.key] ?? 0}
              </span>
            ))}
            <span className={styles.colorStat} title={`Multicolour: ${stats.colors.M ?? 0}`}>
              {/* No printed symbol for "multicolour" — gold is the card frame's own convention. */}
              <span className={styles.goldPip} aria-label="Multicolour" />
              {stats.colors.M ?? 0}
            </span>
            <span className={styles.colorStat} title={`Colourless: ${stats.colors.C ?? 0}`}>
              <ManaSymbol symbol="C" size={13} />
              {stats.colors.C ?? 0}
            </span>
          </div>
          <div className={styles.summaryGroup}>
            <span className={styles.summaryLabel}>Curve</span>
            <div className={styles.curve}>
              {stats.curve.map((count, cmc) => (
                <div key={cmc} className={styles.curveBar} title={`${count} at mana value ${cmc}`}>
                  <div
                    className={styles.curveFill}
                    style={{ height: `${(count / stats.curveMax) * 100}%` }}
                  />
                  <span className={styles.curveLabel}>{cmc === stats.curve.length - 1 ? `${cmc}+` : cmc}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {hoveredCard && hoverPos && (
        <HoverCardPreview
          name={dfc.displayName ?? hoveredCard.name}
          imageUri={dfc.displayImageUri ?? hoveredCard.imageUri ?? null}
          pos={hoverPos}
          overlay={dfc.hint}
          imageRotateDeg={landscapeImageRotateDeg(hoveredCard)}
        />
      )}
    </>,
    document.body,
  )
}

/**
 * Paste-a-list pane. Reuses the deckbuilder's parser and catalog resolution wholesale, including its
 * "N matched of M" coverage readout, so a cube list and a deck list import identically.
 */
function ImportPane({
  catalog,
  onReplace,
  onSuggestName,
}: {
  catalog: readonly CardSummary[]
  onReplace: (entries: CubeEntry[]) => void
  onSuggestName: (name: string) => void
}) {
  const [text, setText] = useState('')

  const preview = useMemo(() => {
    if (text.trim() === '') return null
    const parsed = parseArenaDeckList(text)
    const resolved = resolveAgainstCatalog(parsed.entries, catalog as CardSummary[])
    return { parsed, resolved }
  }, [text, catalog])

  /** Build cube entries from the parse, optionally keeping the names the catalog didn't know. */
  function commit(includeUnmatched: boolean) {
    if (!preview) return
    const source = includeUnmatched
      ? { ...preview.resolved.deckCards, ...preview.resolved.unmatchedCards }
      : preview.resolved.deckCards
    onReplace(Object.entries(source).map(([name, count]) => ({ name, count })))
    if (preview.parsed.deckName) onSuggestName(preview.parsed.deckName)
    setText('')
  }

  const missing = preview ? preview.resolved.unmatched.length : 0

  return (
    <>
      <div className={styles.columnHeader}>
        <span className={styles.columnHeaderTitle}>Paste a cube list</span>
      </div>
      <div className={styles.scroll} style={{ display: 'flex', flexDirection: 'column' }}>
        <textarea
          className={styles.importArea}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={'1 Llanowar Elves\n1 Lightning Bolt\n1 Counterspell'}
          spellCheck={false}
        />
        <p className={styles.hint}>
          Plain text, MTG Arena or Moxfield format — one <code>count name</code> per line. This
          replaces the current cube contents.
        </p>

        {preview && (
          <>
            <div className={styles.importSummary}>
              <span>
                <strong>{preview.resolved.matchedCards}</strong> matched
                {preview.resolved.totalCards !== preview.resolved.matchedCards
                  ? ` of ${preview.resolved.totalCards} (${preview.resolved.totalCards - preview.resolved.matchedCards} not implemented)`
                  : ''}
              </span>
              {missing > 0 ? (
                <span className={styles.badBadge}>{missing} unplayable</span>
              ) : (
                <span className={styles.okBadge}>ready to play</span>
              )}
              {preview.parsed.errors.length > 0 && (
                <span className={styles.badBadge}>
                  {preview.parsed.errors.length} unparseable line
                  {preview.parsed.errors.length === 1 ? '' : 's'}
                </span>
              )}
            </div>

            {missing > 0 && (
              <details className={styles.importDetails} open>
                <summary>Not implemented yet ({missing})</summary>
                <ul>
                  {preview.resolved.unmatched.map((u) => (
                    <li key={`${u.entry.line}-${u.entry.raw}`}>
                      Line {u.entry.line}: <code>{u.entry.raw}</code>
                    </li>
                  ))}
                </ul>
              </details>
            )}

            {preview.parsed.errors.length > 0 && (
              <details className={styles.importDetails}>
                <summary>Unparseable lines ({preview.parsed.errors.length})</summary>
                <ul>
                  {preview.parsed.errors.map((e) => (
                    <li key={`${e.line}-${e.raw}`}>
                      Line {e.line}: <code>{e.raw}</code> — {e.reason}
                    </li>
                  ))}
                </ul>
              </details>
            )}

            <div className={styles.importActions}>
              {missing > 0 && (
                <button type="button" className={styles.secondaryButton} onClick={() => commit(true)}>
                  Import all {preview.resolved.totalCards}
                </button>
              )}
              <button
                type="button"
                className={styles.primaryButton}
                disabled={preview.resolved.matchedCards === 0}
                onClick={() => commit(false)}
              >
                {missing > 0
                  ? `Import ${preview.resolved.matchedCards}, drop the ${missing} unimplemented`
                  : `Import ${preview.resolved.matchedCards} cards`}
              </button>
            </div>
          </>
        )}
      </div>
    </>
  )
}
