/** One always-visible settings group: axis controls and summary in the header, refinements below. */
import type { ReactNode } from 'react'
import { HelpTip } from '@/components/help/HelpTip'
import styles from '../ui/GameUI.module.css'

export function SettingsGroup({
  label,
  topicId,
  summary,
  axisStrip,
  blocking,
  testId,
  children,
}: {
  label: string
  /** Help topic for the *value in effect*, so `?` explains what is selected. */
  topicId: string | null
  /** The live values inside, kept as a compact overview. */
  summary: string
  /** The axis's buttons — always visible, never behind the chevron. */
  axisStrip?: ReactNode
  /** This group holds the reason Start is disabled. */
  blocking?: string | undefined
  testId: string
  /** The refinements, when this lobby shape has any. */
  children?: ReactNode
}) {
  const hasBody = Boolean(children)

  const summaryLine = (
    <>
      <span className={styles.settingsGroupSummary}>{summary}</span>
      {blocking && (
        <span className={styles.settingsGroupBlockingNote} title={blocking}>! {blocking}</span>
      )}
    </>
  )

  return (
    <div
      className={`${styles.settingsGroup} ${blocking ? styles.settingsGroupBlocking : ''}`}
      data-testid={`settings-group-${testId}`}
    >
      <div className={styles.settingsGroupHeader}>
        <div className={styles.settingsGroupLabel}>
          <span>{label}</span>
          {topicId && <HelpTip topicId={topicId} label={`What is ${label}?`} size="sm" />}
        </div>
        <div className={styles.settingsGroupMain}>
          {axisStrip}
          <div className={styles.settingsGroupSummaryRow}>{summaryLine}</div>
        </div>
      </div>
      {hasBody && <div className={styles.settingsGroupBody}>{children}</div>}
    </div>
  )
}
