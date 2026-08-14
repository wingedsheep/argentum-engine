/**
 * Catalog search bar — free-text Scryfall-style query, sort selector, result count, and the
 * syntax help popover. Shared between the deckbuilder page and the scenario builder.
 */
import { useEffect, useState } from 'react'
import type { ParseError } from '../cardFilter'
import styles from '../deckbuilder.module.css'
import type { SortMode } from './cardSort'

export function SearchBar({
  query,
  onQueryChange,
  sortMode,
  onSortChange,
  resultLabel,
  errors,
  actionHint,
}: {
  query: string
  onQueryChange: (next: string) => void
  sortMode: SortMode
  onSortChange: (m: SortMode) => void
  resultLabel: string
  errors: ParseError[]
  /** Overrides the trailing "click adds / right-click removes" hint. */
  actionHint?: React.ReactNode
}) {
  const [helpOpen, setHelpOpen] = useState(false)
  const hasErrors = errors.length > 0
  // Local mirror of the URL-backed `query` so the input never has its DOM value reassigned by
  // the round-trip through `useSearchParams`. Without this, typing certain characters (e.g.
  // parentheses) caused the cursor to jump because React would briefly rerender the input
  // with the previous-tick query while the URL update was in flight, and the browser
  // re-anchored the caret.
  const [localQuery, setLocalQuery] = useState(query)
  useEffect(() => {
    setLocalQuery((prev) => (prev === query ? prev : query))
  }, [query])
  return (
    <div className={styles.searchBar}>
      <div className={styles.searchInputWrap}>
        <input
          className={hasErrors ? styles.searchInputError : styles.searchInput}
          placeholder='Search — try: t:creature c:r cmc<=3, o:flying, (c:u or c:b) -is:legendary'
          value={localQuery}
          onChange={(e) => {
            setLocalQuery(e.target.value)
            onQueryChange(e.target.value)
          }}
          aria-invalid={hasErrors}
          aria-describedby={hasErrors ? 'search-errors' : undefined}
        />
        {hasErrors && (
          <ul id="search-errors" className={styles.searchErrors} role="alert">
            {errors.map((e, i) => (
              <li key={i}>
                <code>{query.slice(e.span.start, e.span.end) || '·'}</code>
                <span>{e.message}{e.suggestion ? ` ${e.suggestion}` : ''}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
      <button
        className={helpOpen ? styles.helpIconActive : styles.helpIcon}
        onClick={() => setHelpOpen((v) => !v)}
        title="Search syntax"
        aria-label="Show search syntax help"
        type="button"
      >
        ?
      </button>
      <select
        className={styles.sortSelect}
        value={sortMode}
        onChange={(e) => onSortChange(e.target.value as SortMode)}
      >
        <option value="name">Name</option>
        <option value="cmc">Mana value</option>
        <option value="color">Colour</option>
        <option value="rarity">Rarity</option>
      </select>
      <span className={styles.resultCount}>{resultLabel}</span>
      <span className={styles.cardActionHint}>
        {actionHint ?? (
          <>
            <kbd>Click</kbd> add · <kbd>Right-click</kbd> remove
          </>
        )}
      </span>
      {helpOpen && <SearchHelp onClose={() => setHelpOpen(false)} onInsert={(t) => onQueryChange(t)} />}
    </div>
  )
}

function SearchHelp({ onClose, onInsert }: { onClose: () => void; onInsert: (t: string) => void }) {
  const examples: Array<{ syntax: string; desc: string }> = [
    { syntax: 'lightning', desc: 'name contains "lightning"' },
    { syntax: '!"Lightning Bolt"', desc: 'exact card name' },
    { syntax: 'name:/^bolt/', desc: 'regex on card name (case-insensitive by default)' },
    { syntax: 't:creature', desc: 'type line — AND of words (try t:legendary creature elf)' },
    { syntax: 'o:flying', desc: 'oracle text contains' },
    { syntax: 'c:r', desc: 'colour identity includes red' },
    { syntax: 'c:azorius', desc: 'guild / shard / wedge name (azorius, bant, mardu, …)' },
    { syntax: 'c=wu', desc: 'colours are exactly white + blue' },
    { syntax: 'c<=rw', desc: 'colours are a subset of red / white' },
    { syntax: 'c>=2', desc: 'colour count comparison' },
    { syntax: 'c:colorless', desc: 'no colours' },
    { syntax: 'mana:{2}{u}{u}', desc: 'mana cost symbols (multiset compare)' },
    { syntax: 'mana>={r}{r}', desc: 'cost contains at least these symbols' },
    { syntax: 'cmc:3', desc: 'mana value (also <=, >=, <, >, !=)' },
    { syntax: 'pow>tou', desc: 'cross-field compare (power vs. toughness)' },
    { syntax: 'pow>=4', desc: 'power (numeric only)' },
    { syntax: 'r:rare', desc: 'rarity (common / uncommon / rare / mythic)' },
    { syntax: 's:blb', desc: 'set code' },
    { syntax: 'f:standard', desc: 'format legality' },
    { syntax: 'is:legendary', desc: 'land/creature/spell/permanent/legendary/basic/dfc/vanilla/bear/historic' },
    { syntax: 'layout:transform', desc: 'card layout (transform / mdfc / normal)' },
    { syntax: 'kw:flying', desc: 'keyword' },
    { syntax: '-t:creature', desc: 'negate any term (also: not t:creature)' },
    { syntax: 't:creature or t:planeswalker', desc: 'boolean OR' },
    { syntax: '(c:u or c:b) t:creature', desc: 'grouping with parens' },
    { syntax: '"lord of"', desc: 'quote multi-word values' },
  ]
  return (
    <>
      <div className={styles.helpBackdrop} onClick={onClose} />
      <div className={styles.helpPopover} role="dialog" aria-label="Search syntax">
        <div className={styles.helpHeader}>
          <strong>Search syntax</strong>
          <button className={styles.linkButton} onClick={onClose} type="button">
            Close
          </button>
        </div>
        <p className={styles.helpHint}>
          Tokens combine with implicit AND. Use <code>or</code> for alternation,
          parentheses for grouping, and <code>-</code> or <code>not</code> for negation.
          Click an example to drop it into the search box.
        </p>
        <ul className={styles.helpList}>
          {examples.map((ex) => (
            <li key={ex.syntax}>
              <button
                className={styles.helpExample}
                onClick={() => {
                  onInsert(ex.syntax)
                  onClose()
                }}
                type="button"
              >
                <code>{ex.syntax}</code>
              </button>
              <span className={styles.helpDesc}>{ex.desc}</span>
            </li>
          ))}
        </ul>
      </div>
    </>
  )
}
