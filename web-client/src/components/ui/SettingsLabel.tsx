/**
 * A lobby setting's label with an inline `?`. Both lobby screens use it, so the explanation of a
 * setting lives in `src/help/topics.ts` rather than in a `title=` attribute that only one of them
 * happens to have.
 */
import type { ReactNode } from 'react'
import { HelpTip } from '@/components/help/HelpTip'
import styles from './GameUI.module.css'

export function SettingsLabel({ topicId, children }: { topicId: string; children: ReactNode }) {
  return (
    <span className={styles.settingsLabelWithHelp}>
      <span className={styles.settingsLabel}>{children}</span>
      <HelpTip topicId={topicId} size="sm" />
    </span>
  )
}
