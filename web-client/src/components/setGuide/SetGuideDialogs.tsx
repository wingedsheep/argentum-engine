/**
 * The Set guide's two dialogs: one set up close ({@link SetProfileDialog}) and up to four sets side
 * by side ({@link SetCompareDialog}). Both are portalled to the body, close on Esc or a backdrop
 * click, and take their sets from the page so the URL can reopen them.
 */
import { useEffect, useRef } from 'react'
import type React from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import { getArchetypesForSets } from '@/components/draft/SetSynergiesOverlay'
import { SetIcon } from '@/components/ui/SetIcon'
import { pageStyles } from '@/components/ui/PageShell'
import { ArchetypeRow, ColorPips, MechanicChips, MechanicList, RatingMeter, RatingMeters, ThemeTags, TipList, TryIf } from './SetProfileParts'
import { MAX_COMPARE, RATINGS, type GuideSet } from './setGuideModel'
import styles from './SetGuide.module.css'

function GuideDialog({ label, wide = false, onClose, children }: { label: string; wide?: boolean; onClose: () => void; children: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    ref.current?.focus()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return createPortal(
    <div className={styles.backdrop} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div ref={ref} className={styles.dialog} data-wide={wide} role="dialog" aria-modal="true" aria-label={label} tabIndex={-1}>
        <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Close" title="Close (Esc)">
          <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden focusable="false">
            <path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
        </button>
        {children}
      </div>
    </div>,
    document.body,
  )
}

function SetMeta({ set }: { set: GuideSet }) {
  return (
    <span className={styles.meta}>
      {[set.year, `${set.cardCount.toLocaleString()} cards`, set.extension ? 'Extension' : null].filter(Boolean).join(' · ')}
    </span>
  )
}

export function SetProfileDialog({
  set,
  comparing,
  compareFull,
  onToggleCompare,
  onClose,
}: {
  set: GuideSet
  comparing: boolean
  compareFull: boolean
  onToggleCompare: () => void
  onClose: () => void
}) {
  const p = set.profile
  const archetypes = getArchetypesForSets([set.code])
  return (
    <GuideDialog label={set.name} onClose={onClose}>
      <header className={styles.dialogHero}>
        <SetIcon code={set.code} className={styles.heroIcon} />
        <div className={styles.heroText}>
          <h2 className={styles.heroName}>{set.name}</h2>
          <SetMeta set={set} />
          {p && <p className={styles.heroTagline}>{p.tagline}</p>}
          {p && <TryIf text={p.tryIf} />}
        </div>
        <SetIcon code={set.code} className={styles.heroWatermark} />
      </header>
      <div className={styles.dialogBody}>
        {p ? (
          <>
            <div className={styles.dialogColumns}>
              <div className={styles.prose}>
                <section>
                  <h3 className={styles.sectionTitle}>How games play</h3>
                  <p>{p.gameplay}</p>
                </section>
                <section>
                  <h3 className={styles.sectionTitle}>How to win</h3>
                  <TipList tips={p.tips} />
                </section>
                <section>
                  <h3 className={styles.sectionTitle}>Mechanics in play</h3>
                  <MechanicList mechanics={p.mechanics} />
                </section>
              </div>
              <aside className={styles.factsAside}>
                <RatingMeters profile={p} />
                <div>
                  <h3 className={styles.sectionTitle}>Themes</h3>
                  <ThemeTags themes={p.themes} />
                </div>
                <div>
                  <h3 className={styles.sectionTitle}>Setting</h3>
                  <p className={styles.themeLine}>{p.setting}</p>
                </div>
              </aside>
            </div>
            {archetypes.length > 0 && (
              <section>
                <h3 className={styles.sectionTitle}>Archetypes</h3>
                <ul className={styles.archetypes}>
                  {archetypes.map((a) => (
                    <ArchetypeRow key={a.name} archetype={a} />
                  ))}
                </ul>
              </section>
            )}
          </>
        ) : (
          <p className={pageStyles.muted}>No guide has been written for this set yet.</p>
        )}
      </div>
      <footer className={styles.dialogFooter}>
        <Link className={pageStyles.button} to={`/cards?q=${encodeURIComponent(`s:${set.code.toLowerCase()}`)}`}>
          Browse its cards
        </Link>
        <button
          type="button"
          className={comparing ? pageStyles.button : pageStyles.buttonPrimary}
          onClick={onToggleCompare}
          disabled={!comparing && compareFull}
          title={!comparing && compareFull ? `You can compare up to ${MAX_COMPARE} sets` : undefined}
        >
          {comparing ? 'Remove from comparison' : 'Add to comparison'}
        </button>
      </footer>
    </GuideDialog>
  )
}

export function SetCompareDialog({
  sets,
  onRemove,
  onOpen,
  onClose,
}: {
  sets: readonly GuideSet[]
  onRemove: (code: string) => void
  onOpen: (code: string) => void
  onClose: () => void
}) {
  const row = (label: string, cell: (s: GuideSet) => React.ReactNode) => (
    <tr key={label}>
      <th scope="row">{label}</th>
      {sets.map((s) => (
        <td key={s.code}>{s.profile ? cell(s) : <span className={pageStyles.muted}>—</span>}</td>
      ))}
    </tr>
  )
  return (
    <GuideDialog label="Compare sets" wide onClose={onClose}>
      <header className={styles.compareHeader}>
        <h2 className={styles.heroName}>Compare sets</h2>
      </header>
      <div className={styles.dialogBody}>
        <div className={styles.compareScroll}>
          <table className={styles.compareTable} style={{ ['--cols' as string]: sets.length }}>
            <thead>
              <tr>
                <th aria-hidden />
                {sets.map((s) => (
                  <th key={s.code} scope="col">
                    <div className={styles.compareSet}>
                      <SetIcon code={s.code} className={styles.compareIcon} />
                      <button type="button" className={styles.linkButton} onClick={() => onOpen(s.code)}>
                        {s.name}
                      </button>
                      <SetMeta set={s} />
                      <button type="button" className={styles.compareRemove} onClick={() => onRemove(s.code)} aria-label={`Remove ${s.name}`}>
                        Remove
                      </button>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {row('In a line', (s) => <span className={styles.compareTagline}>{s.profile!.tagline}</span>)}
              {row('Try it if you like', (s) => <span className={styles.compareTryIf}>{s.profile!.tryIf}</span>)}
              {RATINGS.map((r) => row(r.label, (s) => <RatingMeter ratingKey={r.key} value={s.profile![r.key]} />))}
              {row('How games play', (s) => <span className={styles.compareProse}>{s.profile!.gameplay}</span>)}
              {row('Top tip', (s) => <span className={styles.compareProse}>{s.profile!.tips[0]}</span>)}
              {row('Mechanics', (s) => <MechanicChips mechanics={s.profile!.mechanics} />)}
              {row('Themes', (s) => <ThemeTags themes={s.profile!.themes} />)}
              {row('Archetypes', (s) => {
                const archetypes = getArchetypesForSets([s.code])
                if (archetypes.length === 0) return <span className={pageStyles.muted}>—</span>
                return (
                  <ul className={styles.compareArchetypes}>
                    {archetypes.map((a) => (
                      <li key={a.name}>
                        <ColorPips colors={a.colors} size={12} /> {a.name}
                      </li>
                    ))}
                  </ul>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </GuideDialog>
  )
}
