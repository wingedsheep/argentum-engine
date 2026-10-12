/**
 * The cube editor's left column: three ways to put cards into a cube.
 *
 * - **Search** — the deckbuilder's query language over the catalog, one click per card.
 * - **Add a set** — a whole set at once, each card at the rarity that set printed it at, with the
 *   set's slices (booster pool, optional products, reprints) as toggles. This is the path that makes
 *   a "set cube" — all of Bloomburrow plus Bloomburrow Commander's new cards — a two-step job.
 * - **Paste** — a text list, through the deckbuilder's own parser and catalog resolution, so a cube
 *   list and a deck list import identically.
 */
import { useEffect, useMemo, useState } from 'react'
import type { SharedCubeEntry } from '@/api/account'
import { type CatalogSet, getSetCubeCards, listCatalogSets } from '@/api/cubeSets'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { parseQuery } from '@/components/deckbuilder/cardFilter'
import { parseArenaDeckList, resolveAgainstCatalog } from '@/components/deckbuilder/parseArenaDeck'
import { ManaCost } from '@/components/ui/ManaSymbols'
import { SetIcon } from '@/components/ui/SetIcon'
import {
  CUBE_RARITIES,
  RARITY_LETTER,
  defaultImportChoice,
  emptyTally,
  humanizeProduct,
  selectSetCards,
  type SetCubeCards,
  type SetImportChoice,
} from './cubeModel'
import { RARITY_CLASS } from './RarityChip'
import type { CardHoverHandler } from './useCardHover'
import styles from './CubeEditor.module.css'

export type SourceTab = 'SEARCH' | 'SET' | 'PASTE'

export const SOURCE_TAB_LABEL: Record<SourceTab, string> = {
  SEARCH: 'Search',
  SET: 'Add a set',
  PASTE: 'Paste a list',
}

/* ── Search ───────────────────────────────────────────────────────────── */

export function SearchPane({
  catalog,
  catalogReady,
  inCube,
  onAdd,
  onHover,
}: {
  catalog: readonly CardSummary[]
  catalogReady: boolean
  inCube: ReadonlySet<string>
  onAdd: (name: string) => void
  onHover: CardHoverHandler
}) {
  const [query, setQuery] = useState('')

  const results = useMemo(() => {
    const trimmed = query.trim()
    if (!trimmed || !catalogReady) return []
    const predicate = parseQuery(trimmed)
    return catalog
      .filter((c) => !c.basicLand && !inCube.has(c.name.toLowerCase()) && predicate(c))
      .sort((a, b) => a.name.localeCompare(b.name))
      .slice(0, 80)
  }, [query, catalog, inCube, catalogReady])

  return (
    <>
      <div className={styles.searchRow}>
        <input
          className={styles.input}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search — e.g. c:red t:creature cmc<=3"
          aria-label="Search cards to add"
          autoFocus
        />
      </div>
      <div className={styles.scroll}>
        {!catalogReady ? (
          <p className={styles.hint}>Loading card catalog…</p>
        ) : results.length === 0 ? (
          <p className={styles.hint}>
            {query.trim()
              ? 'No cards match that search, or they are all in the cube already.'
              : <>Search by name or with the deckbuilder&apos;s query language: <code>c:wu</code>,{' '}
                <code>t:instant</code>, <code>cmc&gt;=5</code>, <code>s:BLB</code>, <code>o:draw</code>.</>}
          </p>
        ) : (
          <ul className={styles.rowList}>
            {results.map((card) => (
              <li key={card.name} className={styles.row}>
                <button
                  type="button"
                  className={styles.rowButton}
                  onClick={() => onAdd(card.name)}
                  onMouseEnter={(e) => onHover(card, e)}
                  onMouseMove={(e) => onHover(card, e)}
                  onMouseLeave={() => onHover(null)}
                  title={`Add ${card.name}`}
                >
                  <span className={styles.addGlyph} aria-hidden>+</span>
                  <span className={styles.rowName}>{card.name}</span>
                  <span className={styles.rowMeta}>
                    {card.manaCost ? <ManaCost cost={card.manaCost} size={12} /> : null}
                  </span>
                  <span className={styles.rowSet}>{card.setCode ?? ''}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  )
}

/* ── Add a set ────────────────────────────────────────────────────────── */

export function SetPane({
  inCube,
  onAdd,
}: {
  inCube: ReadonlySet<string>
  /** Hand the chosen cards over; the editor merges them and may adopt the set's name and basics. */
  onAdd: (entries: SharedCubeEntry[], set: SetCubeCards) => void
}) {
  const [sets, setSets] = useState<CatalogSet[]>([])
  const [query, setQuery] = useState('')
  const [loaded, setLoaded] = useState<SetCubeCards | null>(null)
  const [loadingCode, setLoadingCode] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [choice, setChoice] = useState<SetImportChoice | null>(null)
  const [lastAdded, setLastAdded] = useState<{ set: string; added: number } | null>(null)

  useEffect(() => {
    let cancelled = false
    void listCatalogSets().then((list) => {
      if (!cancelled) setSets(list)
    })
    return () => {
      cancelled = true
    }
  }, [])

  const matches = useMemo(() => {
    const q = query.trim().toLowerCase()
    const list = q
      ? sets.filter((s) => s.name.toLowerCase().includes(q) || s.code.toLowerCase() === q)
      : sets
    return list.slice(0, 60)
  }, [sets, query])

  async function pick(code: string) {
    setLoadingCode(code)
    setError(null)
    try {
      const data = await getSetCubeCards(code)
      setLoaded(data)
      setChoice(defaultImportChoice(data))
    } catch (err) {
      setError(err instanceof Error ? err.message : `Couldn't load ${code}`)
    } finally {
      setLoadingCode(null)
    }
  }

  if (!loaded || !choice) {
    return (
      <>
        <div className={styles.searchRow}>
          <input
            className={styles.input}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Find a set — Bloomburrow, BLC…"
            aria-label="Find a set"
            autoFocus
          />
        </div>
        <div className={styles.scroll}>
          {lastAdded && (
            <p className={styles.addedNote}>
              Added {lastAdded.added} card{lastAdded.added === 1 ? '' : 's'} from {lastAdded.set}. Pick another
              set to extend the cube further.
            </p>
          )}
          {error && <p className={styles.error}>{error}</p>}
          {sets.length === 0 ? (
            <p className={styles.hint}>Loading sets…</p>
          ) : matches.length === 0 ? (
            <p className={styles.hint}>No set matches “{query}”.</p>
          ) : (
            <ul className={styles.rowList}>
              {matches.map((set) => (
                <li key={set.code} className={styles.row}>
                  <button
                    type="button"
                    className={styles.rowButton}
                    disabled={loadingCode !== null}
                    onClick={() => void pick(set.code)}
                  >
                    <SetIcon code={set.code} className={styles.setGlyph} />
                    <span className={styles.rowName}>{set.name}</span>
                    <span className={styles.rowMeta}>
                      {loadingCode === set.code ? 'Loading…' : set.releaseDate?.slice(0, 4) ?? ''}
                    </span>
                    <span className={styles.rowSet}>{set.code}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </>
    )
  }

  return (
    <SetChoicePanel
      set={loaded}
      choice={choice}
      onChoice={setChoice}
      inCube={inCube}
      onBack={() => setLoaded(null)}
      onAdd={(entries) => {
        onAdd(entries, loaded)
        setLastAdded({ set: loaded.name, added: entries.filter((e) => !inCube.has(e.name.toLowerCase())).length })
        setLoaded(null)
        setQuery('')
      }}
    />
  )
}

function SetChoicePanel({
  set,
  choice,
  onChoice,
  inCube,
  onBack,
  onAdd,
}: {
  set: SetCubeCards
  choice: SetImportChoice
  onChoice: (choice: SetImportChoice) => void
  inCube: ReadonlySet<string>
  onBack: () => void
  onAdd: (entries: SharedCubeEntry[]) => void
}) {
  const boosterCount = set.cards.filter((c) => c.product === null).length
  const reprintCount = set.cards.filter((c) => c.reprint).length
  const products = useMemo(() => {
    const counts = new Map<string, number>()
    for (const card of set.cards) {
      if (card.product !== null) counts.set(card.product, (counts.get(card.product) ?? 0) + 1)
    }
    return [...counts.entries()].sort((a, b) => a[0].localeCompare(b[0]))
  }, [set])

  const selected = useMemo(() => selectSetCards(set, choice), [set, choice])
  const fresh = selected.filter((e) => !inCube.has(e.name.toLowerCase()))
  const already = selected.length - fresh.length
  const tally = useMemo(() => {
    const t = emptyTally()
    for (const entry of fresh) if (entry.rarity) t[entry.rarity] += 1
    return t
  }, [fresh])

  const toggleProduct = (id: string) => {
    const next = new Set(choice.products)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    onChoice({ ...choice, products: next })
  }

  return (
    <div className={styles.scroll}>
      <div className={styles.setHeader}>
        <SetIcon code={set.code} className={styles.setGlyphLarge} />
        <div className={styles.setHeaderText}>
          <span className={styles.setHeaderName}>{set.name}</span>
          <span className={styles.hintInline}>
            {set.code}
            {set.releaseDate ? `, ${set.releaseDate.slice(0, 4)}` : ''} — {set.cards.length} implemented cards
          </span>
        </div>
        <button type="button" className={styles.linkButton} onClick={onBack}>
          Change set
        </button>
      </div>

      <fieldset className={styles.toggleList}>
        <legend className={styles.srOnly}>Which cards to add</legend>
        <label className={styles.toggleRow} data-disabled={boosterCount === 0}>
          <input
            type="checkbox"
            checked={choice.boosterCards && boosterCount > 0}
            disabled={boosterCount === 0}
            onChange={(e) => onChoice({ ...choice, boosterCards: e.target.checked })}
          />
          <span className={styles.toggleText}>
            <span>Booster cards</span>
            <span className={styles.hintInline}>
              {boosterCount > 0 ? 'Everything that opens in this set’s packs' : 'This set has no booster pool'}
            </span>
          </span>
          <span className={styles.toggleCount}>{boosterCount}</span>
        </label>
        {products.map(([id, count]) => (
          <label key={id} className={styles.toggleRow}>
            <input type="checkbox" checked={choice.products.has(id)} onChange={() => toggleProduct(id)} />
            <span className={styles.toggleText}>
              <span>{humanizeProduct(id)}</span>
              <span className={styles.hintInline}>Printed for this set, outside its packs</span>
            </span>
            <span className={styles.toggleCount}>{count}</span>
          </label>
        ))}
        <label className={styles.toggleRow} data-disabled={reprintCount === 0}>
          <input
            type="checkbox"
            checked={choice.reprints && reprintCount > 0}
            disabled={reprintCount === 0}
            onChange={(e) => onChoice({ ...choice, reprints: e.target.checked })}
          />
          <span className={styles.toggleText}>
            <span>Reprints</span>
            <span className={styles.hintInline}>
              Cards first printed in an earlier set. Turn off to keep only this set’s new cards.
            </span>
          </span>
          <span className={styles.toggleCount}>{reprintCount}</span>
        </label>
      </fieldset>

      <div className={styles.setPreview}>
        <span className={styles.setPreviewCount}>
          {fresh.length === 0 ? 'Nothing new to add' : `Adds ${fresh.length} card${fresh.length === 1 ? '' : 's'}`}
        </span>
        {already > 0 && <span className={styles.hintInline}>{already} already in the cube stay as they are</span>}
        {fresh.length > 0 && (
          <span className={styles.previewRarities}>
            {CUBE_RARITIES.filter((r) => tally[r] > 0).map((r) => (
              <span key={r} className={styles.colorStat}>
                <span className={`${styles.rarityChip} ${styles.rarityExplicit} ${RARITY_CLASS[r]}`} aria-hidden>
                  {RARITY_LETTER[r]}
                </span>
                {tally[r]}
              </span>
            ))}
          </span>
        )}
        <p className={styles.hint}>
          Each card keeps the rarity it has in {set.name}, so booster-style packs open the way this set’s did.
        </p>
        <button
          type="button"
          className={styles.primaryButton}
          disabled={fresh.length === 0}
          onClick={() => onAdd(selected)}
        >
          {fresh.length === 0 ? 'Add cards' : `Add ${fresh.length} cards`}
        </button>
      </div>
    </div>
  )
}

/* ── Paste ────────────────────────────────────────────────────────────── */

export function PastePane({
  catalog,
  cubeIsEmpty,
  onImport,
  onSuggestName,
}: {
  catalog: readonly CardSummary[]
  cubeIsEmpty: boolean
  /** `replace` swaps the cube's contents for the list; otherwise the list is merged in. */
  onImport: (entries: SharedCubeEntry[], replace: boolean) => void
  onSuggestName: (name: string) => void
}) {
  const [text, setText] = useState('')

  const preview = useMemo(() => {
    if (text.trim() === '') return null
    const parsed = parseArenaDeckList(text)
    // A commander tagged as such (an Archidekt `Commander` category, `*CMDR*`) is still a cube card.
    const resolved = resolveAgainstCatalog([...parsed.commander, ...parsed.entries], catalog as CardSummary[])
    return { parsed, resolved }
  }, [text, catalog])

  /** Build cube entries from the parse, optionally keeping the names the catalog didn't know. */
  function commit(includeUnmatched: boolean, replace: boolean) {
    if (!preview) return
    const source = includeUnmatched
      ? { ...preview.resolved.deckCards, ...preview.resolved.unmatchedCards }
      : preview.resolved.deckCards
    onImport(Object.entries(source).map(([name, count]) => ({ name, count })), replace)
    if (preview.parsed.deckName) onSuggestName(preview.parsed.deckName)
    setText('')
  }

  const missing = preview ? preview.resolved.unmatched.length : 0
  const matched = preview?.resolved.matchedCards ?? 0

  return (
    <div className={`${styles.scroll} ${styles.pasteScroll}`}>
      <textarea
        className={styles.importArea}
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={'1 Llanowar Elves\n1 Lightning Bolt\n1 Counterspell'}
        aria-label="Cube list"
        spellCheck={false}
      />
      <p className={styles.hint}>
        Plain text, MTG Arena or Moxfield format — one <code>count name</code> per line. A list exported
        from this editor keeps its cube name.
      </p>

      {preview && (
        <>
          <div className={styles.importSummary}>
            <span>
              <strong>{matched}</strong> matched
              {preview.resolved.totalCards !== matched
                ? ` of ${preview.resolved.totalCards} (${preview.resolved.totalCards - matched} not implemented)`
                : ''}
            </span>
            {missing > 0 ? (
              <span className={styles.badBadge}>{missing} unplayable</span>
            ) : (
              <span className={styles.okBadge}>ready to play</span>
            )}
            {preview.parsed.errors.length > 0 && (
              <span className={styles.badBadge}>
                {preview.parsed.errors.length} unparseable line{preview.parsed.errors.length === 1 ? '' : 's'}
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
              <button type="button" className={styles.secondaryButton} onClick={() => commit(true, cubeIsEmpty)}>
                Keep all {preview.resolved.totalCards}
              </button>
            )}
            {!cubeIsEmpty && (
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={matched === 0}
                onClick={() => commit(false, true)}
                title="Swap the cube's contents for this list"
              >
                Replace cube
              </button>
            )}
            <button
              type="button"
              className={styles.primaryButton}
              disabled={matched === 0}
              onClick={() => commit(false, cubeIsEmpty)}
            >
              {cubeIsEmpty ? `Import ${matched} cards` : `Add ${matched} cards`}
            </button>
          </div>
        </>
      )}
    </div>
  )
}
