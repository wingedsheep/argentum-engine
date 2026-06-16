import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { SetIcon } from '../ui/SetIcon'
import { HoverCardPreview } from '../ui/HoverCardPreview'
import { ProgressChartOverlay } from './ProgressChartOverlay'
import { getScryfallFallbackUrl } from '@/utils/cardImages'
import {
  fetchSetCoverage,
  fetchCoverageSummary,
  fetchSetDetail,
  type SetCoverage,
  type CoverageSummary,
  type SetDetail,
  type CardCoverage,
} from '@/api/setCoverage'
import styles from './SetCompletionPage.module.css'

type SortKey = 'newest' | 'percent' | 'implemented' | 'name'
type Filter = 'all' | 'standard' | 'inProgress' | 'complete'

const SORT_LABELS: Record<SortKey, string> = {
  newest: 'Newest',
  percent: 'Most complete',
  implemented: 'Most cards done',
  name: 'A–Z',
}

const FILTER_LABELS: Record<Filter, string> = {
  all: 'All',
  standard: 'Standard',
  inProgress: 'In progress',
  complete: 'Complete',
}

/** Coverage tier drives the bar/accent color. Mirrors the at-a-glance red→green of the TUI. */
function tier(pct: number): 'empty' | 'low' | 'mid' | 'high' | 'done' {
  if (pct >= 100) return 'done'
  if (pct >= 67) return 'high'
  if (pct >= 34) return 'mid'
  if (pct > 0) return 'low'
  return 'empty'
}

function year(releaseDate: string | null): string {
  return releaseDate?.slice(0, 4) ?? '—'
}

function fmtPct(pct: number): string {
  return Number.isInteger(pct) ? String(pct) : pct.toFixed(1)
}

/** Downscale a baked normal-size Scryfall CDN URL to the small variant for grid thumbnails. */
function smallArt(name: string, imageUri: string | null): string {
  if (imageUri) return imageUri.replace('/normal/', '/small/')
  return getScryfallFallbackUrl(name, 'small')
}

interface HoverState {
  name: string
  imageUri: string | null
  pos: { x: number; y: number }
}

export function SetCompletionPage() {
  const navigate = useNavigate()
  const [sets, setSets] = useState<readonly SetCoverage[] | null>(null)
  const [summary, setSummary] = useState<CoverageSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<SortKey>('newest')
  const [filter, setFilter] = useState<Filter>('all')
  const [openCode, setOpenCode] = useState<string | null>(null)
  const [showProgress, setShowProgress] = useState(false)

  useEffect(() => {
    let cancelled = false
    fetchSetCoverage()
      .then((data) => !cancelled && setSets(data))
      .catch((e) => !cancelled && setError(e instanceof Error ? e.message : String(e)))
    fetchCoverageSummary()
      .then((data) => !cancelled && setSummary(data))
      .catch(() => {}) // banner falls back to summing the per-set rows
    return () => {
      cancelled = true
    }
  }, [])

  const totals = useMemo(() => {
    if (!sets) return null
    const implemented = sets.reduce((n, s) => n + s.implemented, 0)
    const total = sets.reduce((n, s) => n + s.total, 0)
    const complete = sets.filter((s) => s.percent >= 100).length
    return {
      implemented,
      total,
      complete,
      setCount: sets.length,
      percent: total === 0 ? 0 : Math.round((implemented * 1000) / total) / 10,
    }
  }, [sets])

  const visible = useMemo(() => {
    if (!sets) return []
    const q = query.trim().toLowerCase()
    const filtered = sets.filter((s) => {
      if (filter === 'standard' && !s.inStandard) return false
      if (filter === 'complete' && s.percent < 100) return false
      if (filter === 'inProgress' && (s.percent >= 100 || s.implemented === 0)) return false
      if (!q) return true
      return s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q)
    })
    const byName = (a: SetCoverage, b: SetCoverage) => a.name.localeCompare(b.name)
    const sorted = [...filtered]
    switch (sort) {
      case 'percent':
        sorted.sort((a, b) => b.percent - a.percent || b.implemented - a.implemented || byName(a, b))
        break
      case 'implemented':
        sorted.sort((a, b) => b.implemented - a.implemented || byName(a, b))
        break
      case 'name':
        sorted.sort(byName)
        break
      case 'newest':
      default:
        sorted.sort((a, b) => (b.releaseDate ?? '').localeCompare(a.releaseDate ?? '') || a.code.localeCompare(b.code))
        break
    }
    return sorted
  }, [sets, query, sort, filter])

  return (
    <div className={styles.page}>
      <header className={styles.topbar}>
        <button className={styles.backButton} onClick={() => navigate('/')}>
          ← Back to menu
        </button>
        <h1 className={styles.title}>Set Completion</h1>
        <div className={styles.topbarSpacer} />
      </header>

      {totals && (
        <button
          className={`${styles.summary} ${styles.summaryButton}`}
          onClick={() => setShowProgress(true)}
          title="View implementation progress over time"
        >
          {(() => {
            // Headline is the DISTINCT figure (reprints deduped by name) so it answers "how much of
            // Magic is covered" rather than "how many booster printings across all sets" — the latter
            // double-counts a staple once per set. Printings stay as a secondary line. Until the
            // summary endpoint loads we fall back to the printing-based sum of the per-set rows.
            const pct = summary ? summary.distinctPercent : totals.percent
            return (
              <>
                <div className={styles.summaryStat}>
                  <span className={styles.summaryValue}>{fmtPct(pct)}%</span>
                  <span className={styles.summaryLabel}>overall</span>
                </div>
                <div className={styles.summaryBarWrap}>
                  <div className={styles.summaryBarTrack}>
                    <div className={styles.summaryBarFill} style={{ width: `${Math.min(100, pct)}%` }} />
                  </div>
                  <div className={styles.summaryMeta}>
                    {summary ? (
                      <>
                        <span>
                          <strong>{summary.distinctImplemented.toLocaleString()}</strong> /{' '}
                          {summary.distinctTotal.toLocaleString()} distinct booster cards implemented
                        </span>
                        <span>
                          + <strong>{summary.extraDistinctImplemented.toLocaleString()}</strong> /{' '}
                          {summary.extraDistinctTotal.toLocaleString()} distinct extras (completionist)
                        </span>
                        <span>
                          <strong>{summary.printingsImplemented.toLocaleString()}</strong> /{' '}
                          {summary.printingsTotal.toLocaleString()} booster printings across all sets
                        </span>
                        <span>
                          <strong>{summary.setsComplete}</strong> / {summary.setCount} sets complete
                        </span>
                      </>
                    ) : (
                      <>
                        <span>
                          <strong>{totals.implemented.toLocaleString()}</strong> / {totals.total.toLocaleString()}{' '}
                          booster printings implemented
                        </span>
                        <span>
                          <strong>{totals.complete}</strong> / {totals.setCount} sets complete
                        </span>
                      </>
                    )}
                  </div>
                </div>
                <span className={styles.summaryHint}>📈 View progress →</span>
              </>
            )
          })()}
        </button>
      )}

      <div className={styles.controls}>
        <input
          className={styles.search}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Filter sets by name or code…"
          aria-label="Filter sets"
        />
        <div className={styles.segmented} role="group" aria-label="Filter by completion">
          {(Object.keys(FILTER_LABELS) as Filter[]).map((f) => (
            <button
              key={f}
              className={filter === f ? styles.segmentActive : styles.segment}
              onClick={() => setFilter(f)}
            >
              {FILTER_LABELS[f]}
            </button>
          ))}
        </div>
        <div className={styles.segmented} role="group" aria-label="Sort">
          {(Object.keys(SORT_LABELS) as SortKey[]).map((k) => (
            <button
              key={k}
              className={sort === k ? styles.segmentActive : styles.segment}
              onClick={() => setSort(k)}
            >
              {SORT_LABELS[k]}
            </button>
          ))}
        </div>
      </div>

      {error && <div className={styles.error}>Couldn’t load coverage: {error}</div>}
      {!sets && !error && <div className={styles.loading}>Loading set coverage…</div>}

      {sets && (
        <>
          {visible.length === 0 ? (
            <div className={styles.empty}>No sets match “{query}”.</div>
          ) : (
            <div className={styles.grid}>
              {visible.map((s) => (
                <SetCard key={s.code} set={s} onOpen={() => setOpenCode(s.code)} />
              ))}
            </div>
          )}
        </>
      )}

      {openCode && <SetDetailOverlay code={openCode} onClose={() => setOpenCode(null)} />}
      {showProgress && <ProgressChartOverlay onClose={() => setShowProgress(false)} />}
    </div>
  )
}

function SetCard({ set, onOpen }: { set: SetCoverage; onOpen: () => void }) {
  const t = tier(set.percent)
  const remaining = set.total - set.implemented
  return (
    <button className={styles.card} data-tier={t} onClick={onOpen} title={`View ${set.name} cards`}>
      <SetIcon code={set.code} className={styles.watermark} />
      <div className={styles.cardHead}>
        <SetIcon code={set.code} className={styles.cardIcon} title={set.name} />
        <div className={styles.cardTitle}>
          <span className={styles.cardName} title={set.name}>
            {set.name}
          </span>
          <span className={styles.cardMeta}>
            <span className={styles.codeBadge}>{set.code}</span>
            <span>{year(set.releaseDate)}</span>
            {set.block && <span className={styles.block}>{set.block}</span>}
            {set.inStandard && (
              <span className={styles.standardBadge} title="Currently legal in Standard">
                Standard
              </span>
            )}
          </span>
        </div>
        <span className={styles.percent}>{fmtPct(set.percent)}%</span>
      </div>

      <div className={styles.barTrack}>
        <div className={styles.barFill} style={{ width: `${Math.min(100, set.percent)}%` }} />
      </div>

      <div className={styles.cardFoot}>
        <span>
          <strong>{set.implemented}</strong> / {set.total} {set.extraTotal > 0 ? 'booster' : 'cards'}
        </span>
        {set.extraTotal > 0 && (
          <span className={styles.extra}>
            {set.extraImplemented}/{set.extraTotal} extra
          </span>
        )}
        {t === 'done' ? (
          <span className={styles.doneTag}>Complete</span>
        ) : (
          <span className={styles.remaining}>{remaining} to go</span>
        )}
      </div>
    </button>
  )
}

type CardFilter = 'all' | 'implemented' | 'missing'

function SetDetailOverlay({ code, onClose }: { code: string; onClose: () => void }) {
  const [detail, setDetail] = useState<SetDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cardFilter, setCardFilter] = useState<CardFilter>('all')
  const [hover, setHover] = useState<HoverState | null>(null)

  useEffect(() => {
    let cancelled = false
    setDetail(null)
    setError(null)
    fetchSetDetail(code)
      .then((d) => !cancelled && setDetail(d))
      .catch((e) => !cancelled && setError(e instanceof Error ? e.message : String(e)))
    return () => {
      cancelled = true
    }
  }, [code])

  // Close on Escape.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  // Stable handler + memoized filtered lists so that a hover (high-frequency mousemove) only
  // re-renders the floating preview — not the whole ~280-tile grid. The position update is
  // coalesced to one per animation frame so a fast mousemove can't outrun the paint loop.
  const rafRef = useRef<number | null>(null)
  const pendingHover = useRef<HoverState | null>(null)
  const onHover = useCallback((h: HoverState | null) => {
    if (h === null) {
      if (rafRef.current != null) cancelAnimationFrame(rafRef.current)
      rafRef.current = null
      pendingHover.current = null
      setHover(null)
      return
    }
    pendingHover.current = h
    if (rafRef.current == null) {
      rafRef.current = requestAnimationFrame(() => {
        rafRef.current = null
        if (pendingHover.current) setHover(pendingHover.current)
      })
    }
  }, [])
  useEffect(() => () => {
    if (rafRef.current != null) cancelAnimationFrame(rafRef.current)
  }, [])
  const matches = useCallback(
    (c: CardCoverage) =>
      cardFilter === 'all' || (cardFilter === 'implemented' ? c.implemented : !c.implemented),
    [cardFilter],
  )
  const draftCards = useMemo(() => detail?.draft.filter(matches) ?? [], [detail, matches])
  const extraCards = useMemo(() => detail?.extra.filter(matches) ?? [], [detail, matches])

  const t = detail ? tier(detail.percent) : 'empty'

  return (
    <div className={styles.overlayBackdrop} onClick={onClose}>
      <div className={styles.overlay} data-tier={t} onClick={(e) => e.stopPropagation()}>
        <header className={styles.overlayHeader}>
          <SetIcon code={code} className={styles.overlayIcon} />
          <div className={styles.overlayTitleBlock}>
            <span className={styles.overlayTitle}>{detail?.name ?? code}</span>
            <span className={styles.cardMeta}>
              <span className={styles.codeBadge}>{code}</span>
              {detail && <span>{year(detail.releaseDate)}</span>}
              {detail?.block && <span className={styles.block}>{detail.block}</span>}
            </span>
          </div>
          {detail && (
            <div className={styles.overlayStats}>
              <span className={styles.percent}>{fmtPct(detail.percent)}%</span>
              <span className={styles.overlayCounts}>
                <strong>{detail.implemented}</strong>/{detail.total} {detail.extraTotal > 0 ? 'booster' : 'cards'}
                {detail.extraTotal > 0 && (
                  <>
                    {' · '}
                    {detail.extraImplemented}/{detail.extraTotal} extra
                  </>
                )}
              </span>
            </div>
          )}
          <button className={styles.overlayClose} onClick={onClose} aria-label="Close">
            ✕
          </button>
        </header>

        {detail && (
          <div className={styles.barTrack} style={{ flexShrink: 0 }}>
            <div className={styles.barFill} style={{ width: `${Math.min(100, detail.percent)}%` }} />
          </div>
        )}

        <div className={styles.overlayControls}>
          {(['all', 'implemented', 'missing'] as CardFilter[]).map((f) => (
            <button
              key={f}
              className={cardFilter === f ? styles.segmentActive : styles.segment}
              onClick={() => setCardFilter(f)}
            >
              {f === 'all' ? 'All' : f === 'implemented' ? 'Implemented' : 'Missing'}
            </button>
          ))}
        </div>

        <div className={styles.overlayBody}>
          {error && <div className={styles.error}>Couldn’t load cards: {error}</div>}
          {!detail && !error && <div className={styles.loading}>Loading {code} cards…</div>}
          {detail && (
            <>
              <CardSection
                title={detail.extra.length > 0 ? 'Booster' : 'Cards'}
                cards={draftCards}
                total={detail.draft.length}
                onHover={onHover}
              />
              {detail.extra.length > 0 && (
                <CardSection title="Extras" cards={extraCards} total={detail.extra.length} onHover={onHover} />
              )}
            </>
          )}
        </div>
      </div>
      {hover && <HoverCardPreview name={hover.name} imageUri={hover.imageUri} pos={hover.pos} />}
    </div>
  )
}

const CardSection = memo(function CardSection({
  title,
  cards,
  total,
  onHover,
}: {
  title: string
  cards: readonly CardCoverage[]
  total: number
  onHover: (h: HoverState | null) => void
}) {
  if (total === 0) return null
  return (
    <section className={styles.cardSection}>
      <h3 className={styles.sectionTitle}>
        {title} <span className={styles.sectionCount}>({cards.length})</span>
      </h3>
      {cards.length === 0 ? (
        <div className={styles.sectionEmpty}>Nothing here for this filter.</div>
      ) : (
        <div className={styles.cardImageGrid}>
          {cards.map((c) => (
            <CardTile key={c.name} card={c} onHover={onHover} />
          ))}
        </div>
      )}
    </section>
  )
})

const CardTile = memo(function CardTile({
  card,
  onHover,
}: {
  card: CardCoverage
  onHover: (h: HoverState | null) => void
}) {
  return (
    <div
      className={card.implemented ? styles.tile : styles.tileMissing}
      title={card.name}
      onMouseEnter={(e) => onHover({ name: card.name, imageUri: card.imageUri, pos: { x: e.clientX, y: e.clientY } })}
      onMouseMove={(e) => onHover({ name: card.name, imageUri: card.imageUri, pos: { x: e.clientX, y: e.clientY } })}
      onMouseLeave={() => onHover(null)}
    >
      <img
        className={styles.tileImage}
        src={smallArt(card.name, card.imageUri)}
        alt={card.name}
        loading="lazy"
        decoding="async"
      />
      <span className={styles.tileBadge}>{card.implemented ? '✓' : 'Missing'}</span>
      <span className={styles.tileName}>{card.name}</span>
    </div>
  )
})
