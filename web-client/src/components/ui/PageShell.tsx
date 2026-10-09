/**
 * The frame every standalone page shares with the landing screen: the random card art behind a
 * darker tint than home's (these pages are for reading), the wordmark linking home, an optional
 * page title, and the account menu. Content goes in a capped column; `PageShell.module.css` also
 * exports the glass `panel` surface pages build their sections from.
 *
 * `fit` pins the page to the viewport (no page scroll) for screens that scroll their own regions —
 * the deckbuilder, the replay viewer. Without it the page scrolls as one document.
 */
import { useEffect } from 'react'
import type React from 'react'
import { Link } from 'react-router-dom'
import { randomBackground } from '@/utils/background'
import { AuthWidget } from '@/components/auth/AuthWidget'
import { useAuthStore } from '@/store/authStore'
import { ArgentumMark } from './ArgentumMark'
import { FullscreenButton } from './FullscreenButton'
import { ChatButton } from '@/components/messages/ChatButton'
import shell from './PageShell.module.css'

export { shell as pageStyles }

/** The `plain` backdrop, for full-screen views that don't render inside the shell. */
export const PLAIN_BACKDROP = 'radial-gradient(120% 80% at 50% 0%, #161a26 0%, #0b0d14 55%, #08090e 100%)'

interface PageShellProps {
  /** Shown beside the wordmark; also the document heading unless the page renders its own `h1`. */
  title?: React.ReactNode
  /** Extra controls at the end of the top bar, before the account menu. */
  actions?: React.ReactNode
  /** Pin to the viewport and let the page scroll its own regions. */
  fit?: boolean
  /** Column width cap. `wide` is for tables and grids; `full` for tools that use the whole screen. */
  width?: 'narrow' | 'normal' | 'wide' | 'full'
  /** A plain dark backdrop instead of the card art — for dense data pages where the art is noise. */
  plain?: boolean
  /** Hide the account menu (pages that are themselves about signing in). */
  hideAccount?: boolean
  children: React.ReactNode
}

export function PageShell({ title, actions, fit = false, width = 'normal', hideAccount = false, plain = false, children }: PageShellProps) {
  // The landing screen bootstraps the session through `useConnectName`; a page opened directly
  // has to, or the account menu never learns who is signed in.
  const authStatus = useAuthStore((s) => s.status)
  const initAuth = useAuthStore((s) => s.init)
  useEffect(() => {
    if (authStatus === 'idle') void initAuth()
  }, [authStatus, initAuth])

  return (
    <div className={shell.page} data-fit={fit} data-plain={plain} style={plain ? undefined : { backgroundImage: `url(${randomBackground})` }}>
      {!plain && <div className={shell.artTint} aria-hidden />}
      <header className={shell.topBar} data-width={width}>
        <Link to="/" className={shell.brand} aria-label="Argentum — home">
          <BrandMark />
          <span className={shell.brandText}>Argentum</span>
        </Link>
        {title && (
          <>
            <span className={shell.crumbSep} aria-hidden>/</span>
            <span className={shell.crumb}>{title}</span>
          </>
        )}
        <div className={shell.topBarEnd}>
          {actions}
          <ChatButton />
          <FullscreenButton compact />
          {!hideAccount && <AuthWidget />}
        </div>
      </header>
      <main className={shell.main} data-width={width}>
        {children}
      </main>
    </div>
  )
}

/** The Argentum mark beside the wordmark. */
export function BrandMark({ size = 32 }: { size?: number }) {
  return <ArgentumMark size={size} className={shell.brandMark} />
}
