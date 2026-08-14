/**
 * In-game help. A slide-over drawer rather than a route, because navigating to `/help` mid-game
 * would unmount the app and drop the WebSocket.
 *
 * Mount it once per screen that needs in-game help; mounting also tells {@link HelpTip} to send
 * "Read more" here instead of to the `/help` page.
 */
import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { HELP_SECTIONS, topicById, topicsInSection, type HelpSection } from '@/help/topics'
import { useHelpUi } from '@/help/helpStore'
import { HelpTopicView } from './HelpTopicView'
import styles from './help.module.css'

export function HelpDrawer() {
  const isOpen = useHelpUi((s) => s.isOpen)
  const openTopicId = useHelpUi((s) => s.openTopicId)
  const setMounted = useHelpUi((s) => s.setMounted)
  const closeDrawer = useHelpUi((s) => s.closeDrawer)
  const [section, setSection] = useState<HelpSection>('playing')

  useEffect(() => {
    setMounted(true)
    return () => setMounted(false)
  }, [setMounted])

  // Opening on a specific topic switches to its section and scrolls it into view.
  useEffect(() => {
    if (!isOpen || !openTopicId) return
    const topic = topicById(openTopicId)
    if (topic) setSection(topic.section)
    const id = window.setTimeout(() => {
      document.getElementById(openTopicId)?.scrollIntoView({ block: 'start' })
    }, 30)
    return () => window.clearTimeout(id)
  }, [isOpen, openTopicId])

  useEffect(() => {
    if (!isOpen) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') { e.stopPropagation(); closeDrawer() }
    }
    window.addEventListener('keydown', onKey, true)
    return () => window.removeEventListener('keydown', onKey, true)
  }, [isOpen, closeDrawer])

  if (!isOpen) return null

  return createPortal(
    <>
      <div className={styles.drawerBackdrop} onClick={closeDrawer} />
      <aside className={styles.drawer} role="dialog" aria-label="Help">
        <div className={styles.drawerHeader}>
          <h2 className={styles.drawerTitle}>Help</h2>
          <button type="button" className={styles.drawerClose} onClick={closeDrawer} aria-label="Close help">
            ✕
          </button>
        </div>
        <div className={styles.drawerTabs}>
          {HELP_SECTIONS.map((s) => (
            <button
              key={s.id}
              type="button"
              className={`${styles.drawerTab} ${section === s.id ? styles.drawerTabActive : ''}`}
              onClick={() => setSection(s.id)}
            >
              {s.title}
            </button>
          ))}
        </div>
        <div className={styles.drawerBody}>
          {topicsInSection(section).map((topic) => (
            <HelpTopicView
              key={topic.id}
              topic={topic}
              className={styles.topicSeparated}
              onNavigate={(id) => {
                const target = topicById(id)
                if (!target) return
                setSection(target.section)
                window.setTimeout(() => {
                  document.getElementById(id)?.scrollIntoView({ block: 'start' })
                }, 30)
              }}
            />
          ))}
        </div>
        <div className={styles.drawerFooter}>
          The full guide lives at <code>/help</code> — open it in a new tab to keep this game running.
        </div>
      </aside>
    </>,
    document.body,
  )
}

/** The persistent `?` entry point. Opens the drawer when one is mounted. */
export function HelpDrawerButton({ className }: { className?: string }) {
  const openDrawer = useHelpUi((s) => s.openDrawer)
  return (
    <button
      type="button"
      className={`${styles.helpEntryButton} ${className ?? ''}`}
      onClick={() => openDrawer()}
      title="Help — priority modes, stops, yields, shortcuts"
    >
      ? Help
    </button>
  )
}
