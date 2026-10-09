/**
 * The way into direct messages from any screen: a chat icon in the top-bar chrome, beside the
 * fullscreen button, styled like it so it reads as one more quiet control. Unread messages put a small
 * amber count on its corner — while alerts are on here (see `useMessageAlertsOn`). Clicking toggles
 * the chat dock's panel, which drops down from it.
 *
 * `bar` is the page and home top bars; `game` sits in the table's top-left row, after the
 * preferences gear. Renders nothing for guests, without accounts, and on the Messages page itself.
 */
import { useEffect, useRef } from 'react'
import type React from 'react'
import { useLocation } from 'react-router-dom'
import { useResponsiveContext } from '@/components/game/board/shared'
import { useAuthStore } from '@/store/authStore'
import { unreadBadgeCount, useMessagesStore } from '@/store/messagesStore'
import ui from '@/components/ui/GameUI.module.css'
import { useChatDock, useMessageAlertsOn } from './chatDockStore'
import d from './ChatDock.module.css'

/** Left offset of the in-game button: after fullscreen and the preferences gear. */
export const GAME_CHAT_BUTTON_LEFT = { mobile: 84, desktop: 96 } as const

export function ChatButton({ variant = 'bar' }: { variant?: 'bar' | 'game' }) {
  const { pathname } = useLocation()
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const signedIn = useAuthStore((s) => s.status === 'authenticated')
  if (!accountsEnabled || !signedIn || pathname.startsWith('/messages')) return null
  return variant === 'game' ? <GameChatButton /> : <ChatToggle className={ui.fullscreenButtonCompact} iconSize={18} />
}

function GameChatButton() {
  const responsive = useResponsiveContext()
  const size = responsive.isMobile ? 30 : 34
  return (
    <ChatToggle
      iconSize={responsive.isMobile ? 15 : 17}
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 8 : 12,
        left: responsive.isMobile ? GAME_CHAT_BUTTON_LEFT.mobile : GAME_CHAT_BUTTON_LEFT.desktop,
        zIndex: 100,
        width: size,
        height: size,
        padding: 0,
        backgroundColor: 'var(--chrome-bg)',
        backdropFilter: 'var(--chrome-blur)',
        WebkitBackdropFilter: 'var(--chrome-blur)',
        color: 'var(--chrome-text)',
        border: '1px solid var(--chrome-border)',
        borderRadius: 'var(--chrome-radius)',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    />
  )
}

function ChatToggle({ className, style, iconSize }: { className?: string | undefined; style?: React.CSSProperties; iconSize: number }) {
  const ref = useRef<HTMLButtonElement | null>(null)
  const open = useChatDock((s) => s.open)
  const toggle = useChatDock((s) => s.toggle)
  const addAnchor = useChatDock((s) => s.addAnchor)
  const removeAnchor = useChatDock((s) => s.removeAnchor)
  const openChatId = useChatDock((s) => (s.open && s.view.kind === 'chat' ? s.view.accountId : null))
  // The conversation on screen is being read; it doesn't count.
  const unread = useMessagesStore((s) => unreadBadgeCount(s.threads.filter((t) => t.other.accountId !== openChatId)))
  const alerts = useMessageAlertsOn()
  const showCount = alerts && unread > 0

  useEffect(() => {
    const el = ref.current
    if (!el) return
    addAnchor(el)
    return () => removeAnchor(el)
  }, [addAnchor, removeAnchor])

  const label = showCount ? `Messages, ${unread} unread` : 'Messages'
  return (
    <button
      ref={ref}
      type="button"
      className={`${className ?? ''} ${d.button}`}
      style={style}
      onClick={toggle}
      aria-label={label}
      aria-expanded={open}
      aria-haspopup="dialog"
      title={showCount ? `Messages · ${unread} unread` : 'Messages'}
      data-open={open}
      data-testid="chat-button"
    >
      <svg viewBox="0 0 24 24" width={iconSize} height={iconSize} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
        <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
      </svg>
      {/* Re-keyed on the count so a new message pops it. */}
      {showCount && <span key={unread} className={d.badge}>{unread > 99 ? '99+' : unread}</span>}
    </button>
  )
}
