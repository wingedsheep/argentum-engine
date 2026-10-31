/**
 * The lobby set picker's side pane: the Set guide's profile of whichever set the player points at,
 * so they can choose without leaving the lobby. The full guide opens in a new tab for the same
 * reason — navigating away would drop the lobby.
 */
import { getArchetypesForSets } from '@/components/draft/SetSynergiesOverlay'
import { SetIcon } from '@/components/ui/SetIcon'
import type { AvailableSet } from '@/types/messages'
import { ColorPips, MechanicChips, RatingMeters, ThemeTags, TryIf } from './SetProfileParts'
import { setProfile } from './setProfiles'
import styles from './SetGuide.module.css'

export function SetPickerPreview({ set }: { set: AvailableSet | undefined }) {
  if (!set) {
    return (
      <p className={styles.previewEmpty}>
        Point at a set to see what it is about and how it plays.
        <br />
        <a href="/sets" target="_blank" rel="noopener" className={styles.introLink}>Compare every set in the Set guide ↗</a>
      </p>
    )
  }
  const p = setProfile(set.code)
  const archetypes = getArchetypesForSets([set.code])
  const year = set.releaseDate?.slice(0, 4)
  return (
    <div className={styles.preview}>
      <div className={styles.previewHead}>
        <SetIcon code={set.code} className={styles.previewIcon} />
        <div>
          <h4 className={styles.previewName}>{set.name}</h4>
          <span className={styles.meta}>
            {[year, set.implementedCount != null ? `${set.implementedCount} cards` : null].filter(Boolean).join(' · ')}
          </span>
        </div>
      </div>
      {p ? (
        <>
          <p className={styles.tagline}>{p.tagline}</p>
          <TryIf text={p.tryIf} compact />
          <RatingMeters profile={p} />
          <ThemeTags themes={p.themes} />
          <p className={styles.previewText}>{p.gameplay}</p>
          <MechanicChips mechanics={p.mechanics} />
          {archetypes.length > 0 && (
            <ul className={styles.previewArchetypes} aria-label="Archetypes">
              {archetypes.slice(0, 5).map((a) => (
                <li key={a.name}>
                  <ColorPips colors={a.colors} size={12} /> {a.name}
                </li>
              ))}
              {archetypes.length > 5 && <li className={styles.meta}>and {archetypes.length - 5} more</li>}
            </ul>
          )}
          <a href={`/sets?set=${encodeURIComponent(set.code)}`} target="_blank" rel="noopener" className={styles.introLink}>
            More in the Set guide ↗
          </a>
        </>
      ) : (
        <p className={styles.previewText}>
          {set.partial
            ? 'Only part of this set is implemented, so its boosters draw from the cards that exist.'
            : 'No guide has been written for this set yet.'}
        </p>
      )}
    </div>
  )
}
