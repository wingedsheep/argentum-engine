import { useEffect } from 'react'
import { useGameStore } from '@/store/gameStore'

/** How long the notice stays up: long enough to read a short line, short enough never to queue. */
const NOTICE_MS = 4000

/**
 * "Answered for you · Flametongue Kavu: Targeted Flametongue Kavu" — a transient top-centre notice
 * after the client answered a question that had only one legal answer (see `autoAnswer.ts`).
 *
 * The question itself is never shown, so this is how the player learns it was asked and what the
 * answer was — "No Dragon card to reveal", "Fail to find". It doesn't block anything: it fades on
 * its own, and a click dismisses it early.
 */
export function AutoAnswerNotice({ topOffset = 0 }: { topOffset?: number }) {
  const notice = useGameStore((s) => s.autoAnswerNotice)
  const dismiss = useGameStore((s) => s.dismissAutoAnswerNotice)

  useEffect(() => {
    if (!notice) return
    const timer = setTimeout(() => {
      if (useGameStore.getState().autoAnswerNotice?.key === notice.key) dismiss()
    }, NOTICE_MS)
    return () => clearTimeout(timer)
  }, [notice, dismiss])

  if (!notice) return null
  return (
    <div
      key={notice.key}
      role="status"
      aria-live="polite"
      onClick={dismiss}
      title="Only one answer was legal, so it was given for you. You can turn this off in Preferences."
      style={{
        position: 'fixed',
        top: topOffset + 52,
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 120,
        display: 'inline-flex',
        alignItems: 'center',
        gap: 8,
        maxWidth: 'min(640px, calc(100vw - 32px))',
        padding: '6px 14px',
        borderRadius: 999,
        border: '1px solid rgba(148, 163, 184, 0.55)',
        background: 'rgba(12, 12, 18, 0.92)',
        boxShadow: '0 0 14px rgba(148, 163, 184, 0.25)',
        color: '#e8ecf5',
        fontSize: 12,
        fontWeight: 600,
        letterSpacing: '0.02em',
        cursor: 'pointer',
        animation: 'autoAnswerNoticeIn 220ms ease-out',
      }}
    >
      <span style={{ color: '#94a3b8', fontWeight: 700, whiteSpace: 'nowrap' }}>Answered for you</span>
      {notice.sourceName && <span style={{ fontWeight: 800, whiteSpace: 'nowrap' }}>{notice.sourceName}</span>}
      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{notice.text}</span>
      <style>{`
        @keyframes autoAnswerNoticeIn {
          from { opacity: 0; transform: translateX(-50%) translateY(-6px); }
          to { opacity: 1; transform: translateX(-50%) translateY(0); }
        }
      `}</style>
    </div>
  )
}
