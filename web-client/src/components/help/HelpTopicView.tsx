/**
 * Renders one {@link HelpTopic}. Shared by `/help` and the in-game drawer so the two can never
 * drift — the whole point of the single topic registry.
 */
import type { ReactNode } from 'react'
import { HELP_TOPICS, topicById, helpHref, type HelpTopic } from '@/help/topics'
import { SHORTCUTS, shortcutById } from '@/help/shortcuts'
import styles from './help.module.css'

export function HelpTopicView({
  topic,
  onNavigate,
  headerAction,
  className,
}: {
  topic: HelpTopic
  /** How a related-topic link should be followed. Omit for a plain anchor (the `/help` page). */
  onNavigate?: (topicId: string) => void
  /** Rendered opposite the title. The `/help` page puts its copy-link button here. */
  headerAction?: ReactNode
  /** Extra class on the article — how `/help` gives a topic its card surface. */
  className?: string | undefined
}) {
  return (
    <article id={topic.id} className={`${styles.topic} ${className ?? ''}`}>
      <div className={styles.topicHeader}>
        <h3 className={styles.topicTitle}>{topic.title}</h3>
        {headerAction}
      </div>
      <p className={styles.topicSummary}>{withCode(topic.summary)}</p>

      {topic.body?.map((block, i) => {
        if (block.kind === 'p') return <p key={i} className={styles.topicBody}>{withCode(block.text)}</p>
        if (block.kind === 'ul') {
          return (
            <ul key={i} className={styles.topicList}>
              {block.items.map((item, j) => <li key={j}>{withCode(item)}</li>)}
            </ul>
          )
        }
        return <ShortcutTable key={i} />
      })}

      {topic.shortcuts && topic.shortcuts.length > 0 && (
        <div className={styles.shortcutChips}>
          {topic.shortcuts.map((id) => {
            const s = shortcutById(id)
            return s ? (
              <span key={id} className={styles.shortcutChip}>
                <kbd className={styles.kbd}>{s.keys}</kbd>
                {s.label}
              </span>
            ) : null
          })}
        </div>
      )}

      {topic.related && topic.related.length > 0 && (
        <div className={styles.relatedRow}>
          <span className={styles.relatedLabel}>See also</span>
          {topic.related.map((id) => {
            const related = topicById(id)
            if (!related) return null
            return onNavigate ? (
              <button
                key={id}
                type="button"
                className={styles.relatedLink}
                onClick={() => onNavigate(id)}
              >
                {related.title}
              </button>
            ) : (
              <a key={id} href={helpHref(related)} className={styles.relatedLink}>
                {related.title}
              </a>
            )
          })}
        </div>
      )}

      {topic.links && topic.links.length > 0 && (
        <div className={styles.relatedRow}>
          <span className={styles.relatedLabel}>Elsewhere</span>
          {topic.links.map((link) => (
            <a
              key={link.href}
              href={link.href}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.externalLink}
            >
              {link.label}
              <span className={styles.externalMark} aria-hidden> ↗</span>
            </a>
          ))}
        </div>
      )}
    </article>
  )
}

/**
 * Renders markdown-style `code` spans in a topic string.
 *
 * `topics.ts` is plain text and there is no markdown pipeline in the client, but query syntax and
 * decklist lines are unreadable without the distinction — and the backticks were rendering as
 * literal backticks. This one inline construct is the whole of the markup the topics use; anything
 * more is a reason to reach for a real renderer rather than to extend this.
 */
function withCode(text: string): ReactNode {
  const parts = text.split(/(`[^`]+`)/g)
  if (parts.length === 1) return text
  return parts.map((part, i) => (
    part.length > 2 && part.startsWith('`') && part.endsWith('`')
      ? <code key={i} className={styles.inlineCode}>{part.slice(1, -1)}</code>
      : part
  ))
}

export function ShortcutTable() {
  return (
    <div className={styles.shortcutTableWrap}>
      <table className={styles.shortcutTable}>
        <thead>
          <tr>
            <th>Key</th>
            <th>Does</th>
            <th>Where</th>
          </tr>
        </thead>
        <tbody>
          {SHORTCUTS.map((s) => (
            <tr key={s.id}>
              <td>
                {s.keys.split(' / ').map((k, i) => (
                  <span key={k}>
                    {i > 0 && <span className={styles.kbdSep}>or</span>}
                    <kbd className={styles.kbd}>{k}</kbd>
                  </span>
                ))}
              </td>
              <td>{s.label}</td>
              <td className={styles.shortcutWhere}>{s.where}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** Every topic, for the drawer's "everything" view. */
export const ALL_TOPICS = HELP_TOPICS
