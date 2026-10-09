/**
 * Landing-page reminder shown to guests (not signed in) listing what a free account adds:
 * cloud-saved decks, friends, and ranked play with stats. Points at the existing magic-link
 * login flow via the parent's LoginModal.
 *
 * Dismissal is permanent (localStorage) — the AuthWidget's "Log in" pill and the name-entry
 * nudge remain as quieter entry points afterwards.
 */
import { useState } from 'react'
import type React from 'react'
import { useAuthStore } from '@/store/authStore'

const DISMISS_KEY = 'argentum-account-benefits-dismissed'

const BENEFITS: { icon: string; text: string }[] = [
  { icon: '☁️', text: 'Save your decks in the cloud, on any device' },
  { icon: '👥', text: 'Add friends, see when they’re online and message them' },
  { icon: '🏆', text: 'Ranked play with ELO and win/loss stats' },
  { icon: '🎬', text: 'Rewatch and share your game replays' },
]

export function AccountBenefitsCallout({ onCreateAccount }: { onCreateAccount: () => void }) {
  const status = useAuthStore((s) => s.status)
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const [dismissed, setDismissed] = useState(() => localStorage.getItem(DISMISS_KEY) === '1')

  // Only render once auth has resolved to "not signed in" — no flash for logged-in users.
  if (!accountsEnabled || status !== 'anonymous' || dismissed) return null

  const dismiss = () => {
    localStorage.setItem(DISMISS_KEY, '1')
    setDismissed(true)
  }

  return (
    <div style={styles.banner}>
      <div style={styles.text}>
        <strong>Playing as a guest.</strong> Create a free account — one magic link, no password —
        and get:
        <ul style={styles.list}>
          {BENEFITS.map((b) => (
            <li key={b.text} style={styles.listItem}>
              <span style={styles.icon} aria-hidden="true">{b.icon}</span>
              {b.text}
            </li>
          ))}
        </ul>
      </div>
      <div style={styles.actions}>
        <button type="button" style={styles.primary} onClick={onCreateAccount}>
          Create free account
        </button>
        <button type="button" style={styles.secondary} onClick={dismiss}>
          Not now
        </button>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  banner: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    // Glass over the card art, like the panels beside it: opaque enough to read on any background.
    backgroundColor: 'rgba(10, 12, 20, 0.78)',
    backdropFilter: 'blur(18px) saturate(140%)',
    WebkitBackdropFilter: 'blur(18px) saturate(140%)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderRadius: 16,
    padding: '14px 16px',
    textAlign: 'left',
  },
  text: { color: '#e6e9f2', fontSize: 13.5, lineHeight: 1.5, flex: '1 1 260px' },
  list: { margin: '6px 0 0', padding: 0, listStyle: 'none' },
  listItem: { display: 'flex', alignItems: 'baseline', gap: 8, marginTop: 2 },
  icon: { width: 18, textAlign: 'center', flexShrink: 0 },
  actions: { display: 'flex', gap: 8 },
  primary: {
    minHeight: 38,
    padding: '0 16px',
    borderRadius: 10,
    border: 'none',
    backgroundColor: '#eef0f6',
    color: '#10121a',
    fontWeight: 600,
    fontSize: 13.5,
    cursor: 'pointer',
  },
  secondary: {
    minHeight: 38,
    padding: '0 16px',
    borderRadius: 10,
    border: '1px solid rgba(255, 255, 255, 0.2)',
    backgroundColor: 'transparent',
    color: '#d9dde8',
    fontSize: 13.5,
    cursor: 'pointer',
  },
}
