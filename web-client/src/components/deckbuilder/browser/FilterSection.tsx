/**
 * Menu-style filter panel over the catalogue query: set picker, colour chips (with an
 * includes / exactly / at-most mode), type + subtype + rarity + keyword chips, and numeric
 * range rows for mana value / power / toughness.
 *
 * Every control round-trips through the raw query string (via the `cardFilter` chip helpers),
 * so the panel and the free-text search bar always agree. When the query uses `or` / parens —
 * features the flat chip helpers can't rewrite without mangling what the user authored — the
 * panel goes read-only (`advanced`).
 *
 * Shared between the deckbuilder page (left rail) and the scenario builder's card browser.
 */
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { SetIcon } from '@/components/ui/SetIcon'
import { addToken, hasToken, removeToken, toggleToken, type CardSummary } from '../cardFilter'
import styles from '../deckbuilder.module.css'
import type { SetInfo } from './useCardCatalog'

export const COLOR_TOKENS: Array<{ letter: string; label: string; key: string }> = [
  { letter: 'w', label: 'W', key: 'WHITE' },
  { letter: 'u', label: 'U', key: 'BLUE' },
  { letter: 'b', label: 'B', key: 'BLACK' },
  { letter: 'r', label: 'R', key: 'RED' },
  { letter: 'g', label: 'G', key: 'GREEN' },
]

const TYPE_TOKENS = [
  'Creature',
  'Instant',
  'Sorcery',
  'Enchantment',
  'Artifact',
  'Planeswalker',
  'Land',
]

const RARITY_TOKENS = ['common', 'uncommon', 'rare', 'mythic']

// Evergreen keywords most commonly used as filters. Surface as chips; the rest
// remain reachable via free-text `kw:` / `is:` tokens.
const KEYWORD_TOKENS = [
  'Flying',
  'Trample',
  'Vigilance',
  'Lifelink',
  'Deathtouch',
  'Haste',
  'First strike',
  'Double strike',
  'Reach',
  'Menace',
  'Hexproof',
  'Defender',
  'Indestructible',
  'Flash',
  'Ward',
]

export function FilterSection({
  query,
  onQueryChange,
  catalog,
  setInfos,
  advanced,
}: {
  query: string
  onQueryChange: (next: string) => void
  catalog: CardSummary[]
  setInfos: SetInfo[]
  /**
   * `true` when the query uses or / parens — features the flat chip helpers can't
   * round-trip without rewriting expressions the user authored. We surface a hint
   * banner and freeze chip toggles instead. The active-state detection still runs
   * (chips stay visually inert because hasToken is whole-term and won't match
   * anything inside parens), so the panel reads as "snapshot" rather than "stale".
   */
  advanced: boolean
}) {
  const toggle = (token: string) => {
    if (advanced) return
    onQueryChange(toggleToken(query, token))
  }

  // Numeric range filters parse current values out of the query so the boxes
  // round-trip with whatever the user typed.
  const [cmcMin, cmcMax] = useMemo(() => extractRange(query, 'cmc'), [query])
  const [powMin, powMax] = useMemo(() => extractRange(query, 'pow'), [query])
  const [touMin, touMax] = useMemo(() => extractRange(query, 'tou'), [query])

  // Sets the picker can offer: every set that has at least one card in the loaded
  // catalogue. `/api/sets` may include sets the backend hasn't implemented yet, and the
  // catalogue may include codes `/api/sets` doesn't know about — keep the intersection plus
  // a defensive fallback so neither source can silently swallow the other.
  // We count both a card's canonical `setCode` and every `printingSetCodes` reprint, so
  // reprint-only sets (e.g. 8ED, whose cards are all reprints of earlier definitions) still
  // appear — matching the `s:` matcher, which already filters on reprint set codes.
  const availableSets = useMemo(() => {
    const codes = new Set<string>()
    for (const c of catalog) {
      if (c.setCode) codes.add(c.setCode)
      if (c.printingSetCodes) for (const code of c.printingSetCodes) codes.add(code)
    }
    const present = setInfos.filter((s) => codes.has(s.code))
    const known = new Set(present.map((s) => s.code))
    for (const code of codes) {
      if (!known.has(code)) present.push({ code, name: code, releaseDate: null })
    }
    return present
  }, [setInfos, catalog])

  const activeSet = useMemo(() => {
    const m = query.match(/(?:^|\s)s:([^\s]+)/)
    return m ? m[1]! : ''
  }, [query])

  const onSetChange = (next: string) => {
    const without = query.replace(/(?:^|\s)s:[^\s]+(?=\s|$)/g, '').trim()
    onQueryChange(next ? (without ? `${without} s:${next}` : `s:${next}`) : without)
  }

  // Color mode is derived from the query so a directly typed `c=wu` immediately
  // re-selects the "Exactly" segment without a separate state to drift.
  const colorOp = useMemo(() => detectColorOp(query), [query])
  const colorLetters = useMemo(() => collectColorLetters(query), [query])

  const toggleColorLetter = (letter: string) => {
    const next = new Set(colorLetters)
    if (next.has(letter)) next.delete(letter)
    else next.add(letter)
    onQueryChange(rewriteColorTokens(query, colorOp, next))
  }

  const changeColorMode = (newOp: ColorOp) => {
    onQueryChange(rewriteColorTokens(query, newOp, colorLetters))
  }

  return (
    <fieldset className={styles.filterFieldset} disabled={advanced}>
      {advanced && (
        <div className={styles.advancedBanner} role="status">
          Advanced query — chips disabled. Edit the search bar directly.
        </div>
      )}
      {availableSets.length > 0 && (
        <section className={`${styles.section} ${styles.setPickerSection}`}>
          <h2 className={styles.sectionLabel}>Set</h2>
          <SetCombobox sets={availableSets} value={activeSet} onChange={onSetChange} />
        </section>
      )}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionLabel}>Colour</h2>
          <ColorModeSegmented op={colorOp} onChange={changeColorMode} />
        </div>
        <div className={styles.filterRow}>
          {COLOR_TOKENS.map(({ letter, label }) => {
            const active = colorLetters.has(letter)
            return (
              <button
                key={letter}
                className={`${styles.chip} ${styles.chipMana} ${active ? styles.chipActive : ''}`}
                onClick={() => toggleColorLetter(letter)}
                type="button"
                aria-label={label}
                title={label}
              >
                <ManaSymbol symbol={label} size={16} />
              </button>
            )
          })}
          <button
            className={`${styles.chip} ${styles.chipMana} ${hasToken(query, 'is:colorless') ? styles.chipActive : ''}`}
            onClick={() => toggle('is:colorless')}
            type="button"
            aria-label="Colourless"
            title="Colourless cards"
          >
            <ManaSymbol symbol="C" size={16} />
          </button>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Type</h2>
        <div className={styles.filterRow}>
          {TYPE_TOKENS.map((label) => {
            const token = `t:${label}`
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                {label}
              </button>
            )
          })}
          {(() => {
            // Legendary lives next to the type chips because it's the most common supertype
            // filter players reach for (commander eligibility, "your commander said legendary
            // matters", etc.). `is:legendary` resolves via supertypes — distinct from `t:` which
            // matches across cardTypes/supertypes/subtypes by substring. The crown glyph (♛)
            // is the same one the deck-row commander toggle uses, so the visual cue carries
            // across the deckbuilder.
            const token = 'is:legendary'
            const active = hasToken(query, token)
            return (
              <button
                key="legendary"
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                <span aria-hidden="true" style={{ marginRight: 4, color: '#d4a017' }}>♛</span>
                Legendary
              </button>
            )
          })()}
        </div>
      </section>

      <SubtypeSection query={query} onQueryChange={onQueryChange} catalog={catalog} />

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Rarity</h2>
        <div className={styles.filterRow}>
          {RARITY_TOKENS.map((label) => {
            const token = `r:${label}`
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                {label[0]!.toUpperCase() + label.slice(1)}
              </button>
            )
          })}
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Mana value</h2>
        <RangeRow
          token="cmc"
          min={cmcMin}
          max={cmcMax}
          onMin={(v) => onQueryChange(setRange(query, 'cmc', v, cmcMax))}
          onMax={(v) => onQueryChange(setRange(query, 'cmc', cmcMin, v))}
        />
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Power / Toughness</h2>
        <RangeRow
          token="pow"
          label="Pow"
          min={powMin}
          max={powMax}
          onMin={(v) => onQueryChange(setRange(query, 'pow', v, powMax))}
          onMax={(v) => onQueryChange(setRange(query, 'pow', powMin, v))}
        />
        <RangeRow
          token="tou"
          label="Tou"
          min={touMin}
          max={touMax}
          onMin={(v) => onQueryChange(setRange(query, 'tou', v, touMax))}
          onMax={(v) => onQueryChange(setRange(query, 'tou', touMin, v))}
        />
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Keywords</h2>
        <div className={styles.filterRow}>
          {KEYWORD_TOKENS.map((label) => {
            const token = `kw:${label.toLowerCase().replace(/\s+/g, '_')}`
            // KEYWORD enum names use underscores; the parser is case-insensitive.
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                {label}
              </button>
            )
          })}
        </div>
      </section>

      <section className={styles.section}>
        <button
          className={styles.linkButton}
          onClick={() => onQueryChange('')}
          disabled={query.length === 0}
        >
          Clear filters
        </button>
      </section>
    </fieldset>
  )
}

// ---------------------------------------------------------------------------
// Set combobox — the "primary lens" on the catalogue. Owns its own search,
// sort, and open/active state; communicates with the parent only through
// {sets, value, onChange}. Sort preference persists to localStorage.
// ---------------------------------------------------------------------------

function SetCombobox({
  sets,
  value,
  onChange,
}: {
  sets: SetInfo[]
  value: string
  onChange: (next: string) => void
}) {
  const [sortMode, setSortMode] = useState<'name' | 'date'>(() => {
    if (typeof window === 'undefined') return 'name'
    return (window.localStorage.getItem('deckbuilder.setSort') as 'name' | 'date') ?? 'name'
  })
  useEffect(() => {
    if (typeof window !== 'undefined') window.localStorage.setItem('deckbuilder.setSort', sortMode)
  }, [sortMode])

  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [activeIdx, setActiveIdx] = useState(0)

  const rootRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const listRef = useRef<HTMLUListElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  // The combobox sits in a scrolling sidebar (`.left` has overflow), which would clip a wider
  // dropdown. Render the panel in a portal anchored to the input so it can overflow the sidebar
  // and sit above the card grid.
  const [panelPos, setPanelPos] = useState<{ top: number; left: number; width: number } | null>(null)

  const selected = useMemo(() => sets.find((s) => s.code === value) ?? null, [sets, value])

  const sorted = useMemo(() => {
    const arr = [...sets]
    if (sortMode === 'date') {
      arr.sort((a, b) => {
        if (!a.releaseDate && !b.releaseDate) return a.name.localeCompare(b.name)
        if (!a.releaseDate) return 1
        if (!b.releaseDate) return -1
        return b.releaseDate.localeCompare(a.releaseDate)
      })
    } else {
      arr.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }))
    }
    return arr
  }, [sets, sortMode])

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase()
    if (!needle) return sorted
    return sorted.filter(
      (s) => s.name.toLowerCase().includes(needle) || s.code.toLowerCase().includes(needle),
    )
  }, [sorted, search])

  // Close on outside click. mousedown (not click) so a selection inside fires before close.
  // The panel is portaled out of rootRef, so check it separately.
  useEffect(() => {
    if (!open) return
    function onDown(e: MouseEvent) {
      const target = e.target as Node
      if (!rootRef.current?.contains(target) && !panelRef.current?.contains(target)) {
        setOpen(false)
        setSearch('')
      }
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [open])

  // Anchor the portaled panel to the input, and keep it pinned while the sidebar scrolls or the
  // window resizes. Runs before paint to avoid a flash at the origin.
  useLayoutEffect(() => {
    if (!open) {
      setPanelPos(null)
      return
    }
    const reposition = () => {
      const rect = rootRef.current?.getBoundingClientRect()
      if (rect) setPanelPos({ top: rect.bottom + 4, left: rect.left, width: rect.width })
    }
    reposition()
    window.addEventListener('scroll', reposition, true) // capture: catch ancestor scrolls too
    window.addEventListener('resize', reposition)
    return () => {
      window.removeEventListener('scroll', reposition, true)
      window.removeEventListener('resize', reposition)
    }
  }, [open])

  // When opening, point the active row at the current selection so Enter is a no-op
  // by default and arrow keys move from "where you are".
  useEffect(() => {
    if (!open) return
    const idx = selected ? filtered.findIndex((s) => s.code === selected.code) : -1
    setActiveIdx(idx >= 0 ? idx : 0)
  }, [open, selected, filtered])

  // Keep active row in view as the user navigates.
  useEffect(() => {
    if (!open) return
    const el = listRef.current?.querySelector<HTMLElement>(`[data-idx="${activeIdx}"]`)
    el?.scrollIntoView({ block: 'nearest' })
  }, [open, activeIdx])

  function commit(code: string) {
    onChange(code)
    setSearch('')
    setOpen(false)
    inputRef.current?.blur()
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (!open) setOpen(true)
      else setActiveIdx((i) => Math.min(filtered.length - 1, i + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActiveIdx((i) => Math.max(0, i - 1))
    } else if (e.key === 'Enter') {
      if (!open) return
      e.preventDefault()
      const pick = filtered[activeIdx]
      if (pick) commit(pick.code)
    } else if (e.key === 'Escape' && open) {
      e.preventDefault()
      setOpen(false)
      setSearch('')
    } else if (e.key === 'Backspace' && !search && selected && !open) {
      e.preventDefault()
      commit('')
    }
  }

  const displayValue = open ? search : selected ? `${selected.name} (${selected.code})` : ''
  const placeholder = selected ? '' : 'All sets — type to search'
  const activeOptionId = open && filtered[activeIdx] ? `set-opt-${filtered[activeIdx].code}` : undefined

  return (
    <div className={styles.setCombobox} ref={rootRef}>
      <div className={`${styles.setComboInput} ${open ? styles.setComboInputOpen : ''}`}>
        {selected && !open ? (
          <SetIcon code={selected.code} className={styles.setComboIcon} />
        ) : (
          <span className={styles.setComboIcon} aria-hidden="true">⌕</span>
        )}
        <input
          ref={inputRef}
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-controls="set-combobox-list"
          aria-autocomplete="list"
          {...(activeOptionId ? { 'aria-activedescendant': activeOptionId } : {})}
          value={displayValue}
          placeholder={placeholder}
          spellCheck={false}
          autoComplete="off"
          onChange={(e) => {
            setSearch(e.target.value)
            if (!open) setOpen(true)
            setActiveIdx(0)
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
        {selected && !open && (
          <button
            type="button"
            className={styles.setComboClear}
            aria-label={`Clear set filter (${selected.name})`}
            onMouseDown={(e) => {
              e.preventDefault()
              commit('')
            }}
          >
            ×
          </button>
        )}
      </div>
      {open && panelPos &&
        createPortal(
        <div
          ref={panelRef}
          className={styles.setComboPanel}
          style={{ top: panelPos.top, left: panelPos.left, minWidth: panelPos.width }}
        >
          <div className={styles.setComboPanelHeader}>
            <span className={styles.setComboCount}>
              {filtered.length} {filtered.length === 1 ? 'set' : 'sets'}
            </span>
            <div className={styles.modeSegmented} role="tablist" aria-label="Sort sets">
              <button
                type="button"
                role="tab"
                aria-selected={sortMode === 'name'}
                className={`${styles.modeButton} ${sortMode === 'name' ? styles.modeButtonActive : ''}`}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => setSortMode('name')}
              >
                Name
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={sortMode === 'date'}
                className={`${styles.modeButton} ${sortMode === 'date' ? styles.modeButtonActive : ''}`}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => setSortMode('date')}
              >
                Release
              </button>
            </div>
          </div>
          <ul
            id="set-combobox-list"
            ref={listRef}
            className={styles.setComboList}
            role="listbox"
            aria-label="Sets"
          >
            <li
              id="set-opt-all"
              role="option"
              aria-selected={!selected}
              data-idx={-1}
              className={`${styles.setComboOption} ${styles.setComboOptionAll} ${!selected ? styles.setComboOptionSelected : ''}`}
              onMouseDown={(e) => {
                e.preventDefault()
                commit('')
              }}
            >
              <span className={styles.setComboOptionName}>All sets</span>
            </li>
            {filtered.map((s, i) => {
              const isActive = i === activeIdx
              const isSelected = s.code === value
              const year = s.releaseDate ? s.releaseDate.slice(0, 4) : '—'
              const classes = [
                styles.setComboOption,
                isActive ? styles.setComboOptionActive : '',
                isSelected ? styles.setComboOptionSelected : '',
              ]
                .filter(Boolean)
                .join(' ')
              return (
                <li
                  key={s.code}
                  id={`set-opt-${s.code}`}
                  data-idx={i}
                  role="option"
                  aria-selected={isSelected}
                  className={classes}
                  onMouseEnter={() => setActiveIdx(i)}
                  onMouseDown={(e) => {
                    e.preventDefault()
                    commit(s.code)
                  }}
                >
                  <SetIcon code={s.code} className={styles.setComboOptionIcon} />
                  <span className={styles.setComboOptionName}>{s.name}</span>
                  <span className={styles.setComboOptionMeta}>
                    <span className={styles.setComboOptionYear}>{year}</span>
                    <span className={styles.setComboOptionCode}>{s.code}</span>
                  </span>
                </li>
              )
            })}
            {filtered.length === 0 && (
              <li className={styles.setComboEmpty}>No sets match "{search}"</li>
            )}
          </ul>
        </div>,
        document.body,
      )}
    </div>
  )
}

function SubtypeSection({
  query,
  onQueryChange,
  catalog,
}: {
  query: string
  onQueryChange: (next: string) => void
  catalog: CardSummary[]
}) {
  const [input, setInput] = useState('')

  const allSubtypes = useMemo(() => {
    const set = new Set<string>()
    for (const c of catalog) for (const s of c.subtypes) set.add(s)
    return [...set].sort()
  }, [catalog])

  // Active subtype tokens: any t:<X> where X (case-insensitive) is a known
  // subtype. This lets the type-row chips (t:Creature, etc.) coexist without
  // showing as subtype chips here.
  const activeSubtypes = useMemo(() => {
    if (allSubtypes.length === 0) return [] as Array<{ token: string; label: string }>
    const subSet = new Set(allSubtypes.map((s) => s.toLowerCase()))
    const tokens = query.match(/(?:^|\s)(-?t:(?:"[^"]+"|[^\s]+))/g) ?? []
    const out: Array<{ token: string; label: string }> = []
    for (const raw of tokens) {
      const trimmed = raw.trim()
      const m = trimmed.match(/^-?t:"?([^"]+?)"?$/)
      if (m && subSet.has(m[1]!.toLowerCase())) {
        out.push({ token: trimmed, label: m[1]! })
      }
    }
    return out
  }, [query, allSubtypes])

  const addSubtype = (raw: string) => {
    const trimmed = raw.trim()
    if (!trimmed) return
    const canonical = allSubtypes.find((s) => s.toLowerCase() === trimmed.toLowerCase()) ?? trimmed
    const tokenValue = canonical.includes(' ') ? `"${canonical}"` : canonical
    onQueryChange(addToken(query, `t:${tokenValue}`))
    setInput('')
  }

  if (allSubtypes.length === 0) return null

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionLabel}>Subtype / tribe</h2>
      {activeSubtypes.length > 0 && (
        <div className={styles.filterRow}>
          {activeSubtypes.map(({ token, label }) => (
            <button
              key={token}
              className={`${styles.chip} ${styles.chipActive}`}
              onClick={() => onQueryChange(removeToken(query, token))}
              type="button"
              title="Click to remove"
            >
              {label} ✕
            </button>
          ))}
        </div>
      )}
      <input
        list="deckbuilder-subtypes"
        className={styles.textInput}
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault()
            addSubtype(input)
          }
        }}
        onBlur={() => {
          // Auto-add on blur if the entry exactly matches a known subtype,
          // so picking from the datalist (which doesn't always fire Enter)
          // still works.
          const exact = allSubtypes.find((s) => s.toLowerCase() === input.trim().toLowerCase())
          if (exact) addSubtype(exact)
        }}
        placeholder="Goblin, Wizard, Beast…"
      />
      <datalist id="deckbuilder-subtypes">
        {allSubtypes.map((s) => (
          <option key={s} value={s} />
        ))}
      </datalist>
    </section>
  )
}

// ---------------------------------------------------------------------------
// Color mode segmented control + helpers
// ---------------------------------------------------------------------------

type ColorOp = ':' | '=' | '<='

function ColorModeSegmented({
  op,
  onChange,
}: {
  op: ColorOp
  onChange: (op: ColorOp) => void
}) {
  const options: Array<{ op: ColorOp; label: string; title: string }> = [
    { op: ':', label: 'Includes', title: 'Cards that include the chosen colour(s)' },
    { op: '=', label: 'Exactly', title: 'Cards whose colours are exactly the chosen set' },
    { op: '<=', label: 'At most', title: 'Cards whose colours are a subset of the chosen set' },
  ]
  return (
    <div className={styles.modeSegmented} role="group" aria-label="Colour comparison mode">
      {options.map((opt) => (
        <button
          key={opt.op}
          className={op === opt.op ? styles.modeButtonActive : styles.modeButton}
          onClick={() => onChange(opt.op)}
          title={opt.title}
          type="button"
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}

const C_TOKEN_RE = /(?:^|\s)c(<=|=|:)([wubrg]+)(?=\s|$)/gi

function detectColorOp(query: string): ColorOp {
  C_TOKEN_RE.lastIndex = 0
  const match = C_TOKEN_RE.exec(query)
  return ((match?.[1] as ColorOp) ?? ':')
}

function collectColorLetters(query: string): Set<string> {
  const out = new Set<string>()
  C_TOKEN_RE.lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = C_TOKEN_RE.exec(query)) !== null) {
    for (const ch of m[2]!.toLowerCase()) out.add(ch)
  }
  return out
}

function rewriteColorTokens(query: string, op: ColorOp, letters: Set<string>): string {
  // Strip every existing colour token regardless of operator so we don't
  // leave stale `c:X` behind when switching to exactly/at-most.
  const cleaned = query.replace(/(?:^|\s)c(?:<=|=|:)[wubrgWUBRG]+(?=\s|$)/g, '').trim()
  if (letters.size === 0) return cleaned
  const sorted = [...letters].sort().join('')
  if (op === ':') {
    // Per-letter tokens — "must contain each chosen colour" reads cleanly
    // and stays compatible with the existing chip-toggle UX.
    const tokens = [...sorted].map((l) => `c:${l}`).join(' ')
    return cleaned ? `${cleaned} ${tokens}` : tokens
  }
  return cleaned ? `${cleaned} c${op}${sorted}` : `c${op}${sorted}`
}

function RangeRow({
  label,
  min,
  max,
  onMin,
  onMax,
}: {
  token: string
  label?: string
  min: number | null
  max: number | null
  onMin: (v: string) => void
  onMax: (v: string) => void
}) {
  return (
    <div className={styles.numericRow}>
      {label && <span style={{ minWidth: 32 }}>{label}</span>}
      <span>min</span>
      <input
        type="number"
        min={0}
        max={20}
        className={styles.numericInput}
        value={min ?? ''}
        onChange={(e) => onMin(e.target.value)}
      />
      <span>max</span>
      <input
        type="number"
        min={0}
        max={20}
        className={styles.numericInput}
        value={max ?? ''}
        onChange={(e) => onMax(e.target.value)}
      />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Range helpers (numeric inputs ↔ query string)
//
// Manipulate `<key>>=N` and `<key><=N` tokens directly so the menu and the
// raw query string stay in sync. Generic over key (cmc, pow, tou).
// ---------------------------------------------------------------------------

export function extractRange(query: string, key: string): [number | null, number | null] {
  const minRe = new RegExp(`(?:^|\\s)${key}>=(\\d+)(?:\\s|$)`)
  const maxRe = new RegExp(`(?:^|\\s)${key}<=(\\d+)(?:\\s|$)`)
  const minMatch = query.match(minRe)
  const maxMatch = query.match(maxRe)
  return [
    minMatch ? parseInt(minMatch[1]!, 10) : null,
    maxMatch ? parseInt(maxMatch[1]!, 10) : null,
  ]
}

export function setRange(
  query: string,
  key: string,
  min: string | number | null,
  max: string | number | null
): string {
  const minRe = new RegExp(`(?:^|\\s)${key}>=\\d+(?=\\s|$)`, 'g')
  const maxRe = new RegExp(`(?:^|\\s)${key}<=\\d+(?=\\s|$)`, 'g')
  let next = query.replace(minRe, '').replace(maxRe, '').trim()
  const minN = typeof min === 'string' ? (min === '' ? null : parseInt(min, 10)) : min
  const maxN = typeof max === 'string' ? (max === '' ? null : parseInt(max, 10)) : max
  if (minN !== null && Number.isFinite(minN)) next = `${next} ${key}>=${minN}`.trim()
  if (maxN !== null && Number.isFinite(maxN)) next = `${next} ${key}<=${maxN}`.trim()
  return next
}
