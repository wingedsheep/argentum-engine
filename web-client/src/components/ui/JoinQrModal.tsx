import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'

/**
 * A compact "QR" button that opens a modal showing a scannable QR code for a lobby join link.
 *
 * Meant to sit beside the textual invite code in a lobby. Scanning the code opens the
 * `/join/:lobbyId` deep link on the other device, which auto-connects and joins the lobby — the
 * fast path for pulling a phone into your game. The button is self-contained (owns its own open
 * state) so any lobby overlay can drop it in next to its invite box.
 */
export function JoinQrModal({ url }: { url: string }) {
  const [open, setOpen] = useState(false)
  const [copied, setCopied] = useState(false)

  // Close on Escape while open — standard modal affordance.
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false) }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  const copyLink = () => {
    navigator.clipboard.writeText(url)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Show QR code to join"
        title="Show QR code to join"
        data-testid="lobby-qr-button"
        style={styles.iconButton}
      >
        <QrGlyph />
      </button>

      {open && (
        <div
          style={styles.backdrop}
          onClick={() => setOpen(false)}
          role="dialog"
          aria-modal="true"
          aria-label="Scan to join lobby"
          data-testid="lobby-qr-modal"
        >
          <div style={styles.panel} onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="Close"
              style={styles.close}
            >
              ×
            </button>
            <h2 style={styles.title}>Scan to join</h2>
            <p style={styles.subtitle}>Point another phone's camera at the code to jump into this lobby.</p>
            <div style={styles.qrCard}>
              <QRCodeSVG value={url} size={232} level="M" marginSize={2} bgColor="#ffffff" fgColor="#0a0a0f" />
            </div>
            <div style={styles.urlRow}>
              <span style={styles.url}>{url}</span>
              <button type="button" onClick={copyLink} style={styles.copyButton}>
                {copied ? 'Copied!' : 'Copy link'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

/** Minimalist QR-code glyph (three finder squares + a few modules) drawn inline so we ship no icon asset. */
function QrGlyph() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M3 3h8v8H3V3zm2 2v4h4V5H5zM3 13h8v8H3v-8zm2 2v4h4v-4H5zM13 3h8v8h-8V3zm2 2v4h4V5h-4z" />
      <path d="M13 13h3v3h-3v-3zm5 0h3v3h-3v-3zm-5 5h3v3h-3v-3zm5 0h3v3h-3v-3z" />
    </svg>
  )
}

const styles: Record<string, React.CSSProperties> = {
  iconButton: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: 40,
    height: 40,
    flexShrink: 0,
    color: 'var(--text-secondary)',
    backgroundColor: 'var(--bg-panel-light)',
    border: '1px solid var(--border-light)',
    borderRadius: 'var(--radius-xl)',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  backdrop: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.78)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 3000,
    padding: 16,
  },
  panel: {
    position: 'relative',
    backgroundColor: '#1a1a2e',
    border: '1px solid #2a2a3e',
    borderRadius: 16,
    padding: '28px 28px 24px',
    width: '100%',
    maxWidth: 340,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 12,
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
  close: {
    position: 'absolute',
    top: 10,
    right: 14,
    background: 'none',
    border: 'none',
    color: '#888',
    fontSize: 26,
    lineHeight: 1,
    cursor: 'pointer',
  },
  title: {
    margin: 0,
    fontSize: 20,
    fontWeight: 600,
    color: '#e0e0e0',
  },
  subtitle: {
    margin: 0,
    fontSize: 13,
    color: '#9a9aae',
    textAlign: 'center',
  },
  qrCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 14,
    lineHeight: 0,
    marginTop: 4,
  },
  urlRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    width: '100%',
    marginTop: 4,
  },
  url: {
    flex: 1,
    minWidth: 0,
    fontFamily: 'monospace',
    fontSize: 12,
    color: '#9a9aae',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  copyButton: {
    flexShrink: 0,
    padding: '8px 14px',
    fontSize: 13,
    fontWeight: 600,
    color: 'white',
    backgroundColor: '#9b59b6',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
}
