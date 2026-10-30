/**
 * The pieces a set profile is drawn from, shared by the Set guide page, its dialogs and the lobby's
 * set picker: the five-step rating meter, the three ratings stacked, and the mechanic/theme chips.
 */
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { getArchetypeGradient, type Archetype } from '@/components/draft/SetSynergiesOverlay'
import { getScryfallArtCropUrl } from '@/utils/cardImages'
import { RATINGS, ratingWord, type RatingKey } from './setGuideModel'
import { SET_THEMES, type Rating, type SetProfile } from './setProfiles'
import styles from './SetGuide.module.css'

export function RatingMeter({ ratingKey, value, compact = false }: { ratingKey: RatingKey; value: Rating; compact?: boolean }) {
  const meta = RATINGS.find((r) => r.key === ratingKey)!
  const word = ratingWord(ratingKey, value)
  return (
    <div className={styles.meter} data-rating={ratingKey} data-compact={compact} title={`${meta.label}: ${word} — ${meta.hint}`}>
      <span className={styles.meterLabel}>{meta.label}</span>
      <span className={styles.meterTrack} role="meter" aria-valuemin={1} aria-valuemax={5} aria-valuenow={value} aria-valuetext={word} aria-label={meta.label}>
        {[1, 2, 3, 4, 5].map((i) => (
          <span key={i} className={styles.meterStep} data-on={i <= value} />
        ))}
      </span>
      {!compact && <span className={styles.meterWord}>{word}</span>}
    </div>
  )
}

export function RatingMeters({ profile, compact = false }: { profile: SetProfile; compact?: boolean }) {
  return (
    <div className={styles.meters}>
      {RATINGS.map((r) => (
        <RatingMeter key={r.key} ratingKey={r.key} value={profile[r.key]} compact={compact} />
      ))}
    </div>
  )
}

/** `single` keeps the chips to one line, dropping the ones that don't fit — tiles show the lead mechanics only. */
export function MechanicChips({ mechanics, single = false }: { mechanics: readonly string[]; single?: boolean }) {
  return (
    <ul className={styles.chips} data-single={single} aria-label="Mechanics">
      {mechanics.map((m) => (
        <li key={m} className={styles.chip}>{m}</li>
      ))}
    </ul>
  )
}

export function themeLabel(id: string): string {
  return SET_THEMES.find((t) => t.id === id)?.label ?? id
}

/** Colour pips for an archetype, e.g. W U. */
export function ColorPips({ colors, size = 14 }: { colors: readonly string[]; size?: number }) {
  return (
    <span className={styles.pips}>
      {colors.map((c) => (
        <ManaSymbol key={c} symbol={c} size={size} />
      ))}
    </span>
  )
}

/** An archetype as a row: key-card art (or a colour gradient), pips, name and its plan. */
export function ArchetypeRow({ archetype }: { archetype: Archetype }) {
  const art = archetype.keyCard ? getScryfallArtCropUrl(archetype.keyCard, archetype.keyCardSet) : null
  return (
    <li className={styles.archetype}>
      <span className={styles.archetypeArt} style={{ background: getArchetypeGradient(archetype.colors) }} aria-hidden>
        {art && (
          <img
            src={art}
            alt=""
            loading="lazy"
            onError={(e) => {
              e.currentTarget.style.display = 'none'
            }}
          />
        )}
      </span>
      <span className={styles.archetypeText}>
        <span className={styles.archetypeName}>
          <ColorPips colors={archetype.colors} size={13} />
          {archetype.name}
        </span>
        <span className={styles.archetypeDesc}>{archetype.description}</span>
      </span>
    </li>
  )
}
