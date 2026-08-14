/**
 * `/help` — the full guide. Deep-linkable as `/help/<section>#<topic-id>`, which is what every
 * inline {@link HelpTip}'s "Read more" points at.
 *
 * Content comes entirely from `src/help/topics.ts`; this file is layout only. What it adds on top
 * of rendering a section is the three things a 40-topic guide needs to be usable: a search box that
 * spans every section, a per-section topic index, and a copy-link button on each topic — the deep
 * links existed but there was no way to get one out of the page.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  HELP_SECTIONS,
  topicsInSection,
  topicById,
  searchTopics,
  sectionMeta,
  helpHref,
  type HelpSection,
  type HelpTopic,
} from '@/help/topics'
import { HelpTopicView } from '@/components/help/HelpTopicView'
import styles from './HelpPage.module.css'

const DEFAULT_SECTION: HelpSection = 'getting-started'

/** How long a topic stays ringed after being jumped to, so you can see where you landed. */
const HIGHLIGHT_MS = 2200

function isSection(value: string | undefined): value is HelpSection {
  return HELP_SECTIONS.some((s) => s.id === value)
}

export function HelpPage() {
  const { section: sectionParam } = useParams<{ section?: string }>()
  const navigate = useNavigate()
  const section: HelpSection = isSection(sectionParam) ? sectionParam : DEFAULT_SECTION
  const meta = sectionMeta(section)

  const [query, setQuery] = useState('')
  const searchRef = useRef<HTMLInputElement>(null)
  // `#root` is overflow:hidden for the game board, so the page — not the window — is what scrolls.
  const pageRef = useRef<HTMLDivElement>(null)
  const [highlighted, setHighlighted] = useState<string | null>(null)

  const trimmed = query.trim()
  const results = useMemo(() => (trimmed ? searchTopics(trimmed) : null), [trimmed])
  const sectionTopics = useMemo(() => topicsInSection(section), [section])
  const topics = results ?? sectionTopics

  const index = HELP_SECTIONS.findIndex((s) => s.id === section)
  const previous = index > 0 ? HELP_SECTIONS[index - 1] : undefined
  const next = index < HELP_SECTIONS.length - 1 ? HELP_SECTIONS[index + 1] : undefined

  /** Ring a topic for a moment after scrolling to it, then let it fade back. */
  const flag = useCallback((id: string) => {
    setHighlighted(id)
    window.setTimeout(() => setHighlighted((current) => (current === id ? null : current)), HIGHLIGHT_MS)
  }, [])

  const scrollToTopic = useCallback((id: string) => {
    // One frame is not enough when the section changed — the topic has to be in the DOM first.
    window.setTimeout(() => {
      document.getElementById(id)?.scrollIntoView({ block: 'start', behavior: 'smooth' })
      flag(id)
    }, 60)
  }, [flag])

  // Honour the #topic-id fragment once the section has rendered.
  useEffect(() => {
    const id = window.location.hash.slice(1)
    if (!id) return
    const timer = window.setTimeout(() => {
      document.getElementById(id)?.scrollIntoView({ block: 'start', behavior: 'smooth' })
      setHighlighted(id)
      window.setTimeout(() => setHighlighted((current) => (current === id ? null : current)), HIGHLIGHT_MS)
    }, 50)
    return () => window.clearTimeout(timer)
  }, [section])

  // `/` focuses search the way it does on most docs sites; Esc gives the field back.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null
      const typing = target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA'
      if (e.key === '/' && !typing) {
        e.preventDefault()
        searchRef.current?.focus()
      } else if (e.key === 'Escape' && typing) {
        setQuery('')
        searchRef.current?.blur()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  /** Follow a topic link: switch section if needed, clear any search, scroll and ring it. */
  const goToTopic = useCallback((id: string) => {
    const target = topicById(id)
    if (!target) return
    setQuery('')
    navigate(`/help/${target.section}#${id}`)
    scrollToTopic(id)
  }, [navigate, scrollToTopic])

  const goToSection = (id: HelpSection) => {
    setQuery('')
    navigate(`/help/${id}`)
    pageRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className={styles.page} ref={pageRef}>
      <div className={styles.topBar}>
        <div className={styles.topBarInner}>
          <button type="button" className={styles.backButton} onClick={() => navigate('/')}>
            ← Menu
          </button>
          <span className={styles.topBarTitle}>Argentum Help</span>
          <div className={styles.searchBox}>
            <svg className={styles.searchIcon} viewBox="0 0 16 16" aria-hidden="true">
              <circle cx="7" cy="7" r="4.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
              <path d="M10.5 10.5 L14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
            <input
              ref={searchRef}
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search the guide"
              aria-label="Search help topics"
              className={styles.searchInput}
            />
            {trimmed
              ? (
                <button
                  type="button"
                  className={styles.searchClear}
                  onClick={() => { setQuery(''); searchRef.current?.focus() }}
                  aria-label="Clear search"
                >
                  ✕
                </button>
                )
              : <kbd className={styles.searchHint}>/</kbd>}
          </div>
        </div>
      </div>

      <div className={styles.body}>
        <header className={styles.header}>
          <h1 className={styles.title}>Help</h1>
          <p className={styles.subtitle}>
            For players who know Magic and are new to Argentum. This does not teach the rules — it
            explains what this app does with them.
          </p>
        </header>

        <div className={styles.layout}>
          <nav className={styles.nav} aria-label="Help sections">
            <div className={styles.navSections}>
              {HELP_SECTIONS.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  className={`${styles.navItem} ${s.id === section && !results ? styles.navItemActive : ''}`}
                  onClick={() => goToSection(s.id)}
                >
                  <span className={styles.navItemHead}>
                    <span className={styles.navItemTitle}>{s.title}</span>
                    <span className={styles.navItemCount}>{topicsInSection(s.id).length}</span>
                  </span>
                  <span className={styles.navItemBlurb}>{s.blurb}</span>
                </button>
              ))}
            </div>

            {/* Long sections are otherwise a wall — 14 topics under Game modes with no way to see
                what is coming. Hidden while searching, where the result list is already the index. */}
            {!results && sectionTopics.length > 1 && (
              <div className={styles.onThisPage}>
                <span className={styles.onThisPageLabel}>On this page</span>
                {sectionTopics.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    className={styles.onThisPageLink}
                    onClick={() => goToTopic(t.id)}
                  >
                    {t.title}
                  </button>
                ))}
              </div>
            )}
          </nav>

          <main className={styles.content}>
            {results
              ? (
                <div className={styles.sectionIntro}>
                  <h2 className={styles.sectionTitle}>
                    {results.length} {results.length === 1 ? 'result' : 'results'}
                  </h2>
                  <p className={styles.sectionBlurb}>
                    {results.length > 0
                      ? <>Topics matching “{trimmed}”, across every section.</>
                      : <>Nothing matches “{trimmed}”. Try a shorter query, or browse the sections.</>}
                  </p>
                </div>
                )
              : (
                <div className={styles.sectionIntro}>
                  <h2 className={styles.sectionTitle}>{meta.title}</h2>
                  <p className={styles.sectionBlurb}>{meta.blurb}</p>
                </div>
                )}

            <div className={styles.topics}>
              {topics.map((topic) => (
                <HelpTopicView
                  key={topic.id}
                  topic={topic}
                  className={`${styles.topicCard} ${highlighted === topic.id ? styles.topicCardFlagged : ''}`}
                  onNavigate={goToTopic}
                  headerAction={
                    <>
                      {results && (
                        <span className={styles.resultSection}>{sectionMeta(topic.section).title}</span>
                      )}
                      <CopyLinkButton topic={topic} />
                    </>
                  }
                />
              ))}
            </div>

            {!results && (previous || next) && (
              <div className={styles.pager}>
                {previous
                  ? (
                    <button type="button" className={styles.pagerLink} onClick={() => goToSection(previous.id)}>
                      <span className={styles.pagerDirection}>← Previous</span>
                      <span className={styles.pagerTitle}>{previous.title}</span>
                    </button>
                    )
                  : <span />}
                {next && (
                  <button
                    type="button"
                    className={`${styles.pagerLink} ${styles.pagerLinkNext}`}
                    onClick={() => goToSection(next.id)}
                  >
                    <span className={styles.pagerDirection}>Next →</span>
                    <span className={styles.pagerTitle}>{next.title}</span>
                  </button>
                )}
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  )
}

/** Copies the topic's deep link. The links already worked; there was no way to obtain one. */
function CopyLinkButton({ topic }: { topic: HelpTopic }) {
  const [copied, setCopied] = useState(false)
  const url = new URL(helpHref(topic), window.location.origin).href

  const copy = () => {
    void navigator.clipboard?.writeText(url).then(
      () => {
        setCopied(true)
        window.setTimeout(() => setCopied(false), 1400)
      },
      () => { /* clipboard blocked — the anchor is still in the address bar after a click */ },
    )
  }

  return (
    <button
      type="button"
      className={`${styles.copyLink} ${copied ? styles.copyLinkCopied : ''}`}
      onClick={copy}
      title={`Copy a link to “${topic.title}”`}
      aria-label={`Copy a link to ${topic.title}`}
    >
      {copied ? 'Copied' : '#'}
    </button>
  )
}
