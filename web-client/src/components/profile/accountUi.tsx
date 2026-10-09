/**
 * Building blocks shared by the account pages (profile, stats, public profiles, friends): the page
 * frame, the recent-games and tournament lists, and the glass modal the deck and tournament viewers
 * open in. Styles live in `account.module.css`; panels, buttons and inputs come from the page shell.
 */
import { useEffect } from 'react'
import type React from 'react'
import type { GameHistoryEntry, UserTournamentEntry } from '@/api/account'
import { colorForIdentity, colorLabel, gameModeLabel } from '@/components/admin/statFormat'
import { TournamentStatusBadge } from '@/components/tournament/TournamentStatusBadge'
import { PageShell, pageStyles } from '@/components/ui/PageShell'
import { formatDateTime } from '@/utils/datetime'
import { AvatarArt, hasAvatarArt } from './AvatarArt'
import { initialOf } from './avatars'
import { EloCell, OpponentCell } from './gameHistoryCells'
import a from './account.module.css'

export { a as accountStyles }

/** The page shell at the width an account page wants. */
export function AccountPage({
  title,
  width = 'normal',
  plain = false,
  children,
}: {
  title?: string
  width?: 'narrow' | 'normal' | 'wide'
  /** Dark backdrop instead of card art — for the chart-heavy stats views. */
  plain?: boolean
  children: React.ReactNode
}) {
  return (
    <PageShell title={title} width={width} plain={plain}>
      {children}
    </PageShell>
  )
}

/** A centred glass card for signed-out, loading and error states. */
export function MessageCard({ children, center = false }: { children: React.ReactNode; center?: boolean }) {
  return (
    <div className={a.centerWrap}>
      <div className={`${pageStyles.panel} ${a.messageCard}`} data-center={center}>
        {children}
      </div>
    </div>
  )
}

/**
 * An account's avatar: its chosen preset portrait, or the display name's initial in a circle when it
 * has none (or picked one this client has no art for).
 */
export function Avatar({
  name,
  avatar,
  small = false,
  online,
}: {
  name: string
  avatar?: string | null | undefined
  small?: boolean
  online?: boolean | undefined
}) {
  const art = hasAvatarArt(avatar)
  return (
    <span className={small ? a.avatarSmall : a.avatar} data-portrait={art} aria-hidden>
      {art ? <AvatarArt avatar={avatar} /> : initialOf(name)}
      {online !== undefined && <span className={a.presence} data-online={online} />}
    </span>
  )
}

/**
 * Recent games as rows: result, mode + date, opponent + colours, rating, and (on the owner's own
 * profile) the deck / replay actions. Collapses to a two-line card on a phone.
 */
export function GameHistoryList({
  games,
  renderActions,
}: {
  games: GameHistoryEntry[]
  renderActions?: (game: GameHistoryEntry) => React.ReactNode
}) {
  return (
    <ul className={a.list}>
      {games.map((g, i) => {
        const mode = gameModeLabel(g.gameMode, g.format)
        return (
          <li key={`${g.gameId}-${i}`} className={a.gameRow} data-compact={!renderActions}>
            <span className={a.result} data-won={g.won}>{g.won ? 'Win' : 'Loss'}</span>
            <span className={a.cellStack}>
              <span className={a.cellMain}>
                {mode.primary}
                {mode.variant ? <span className={a.dim}> › {mode.variant}</span> : null}
              </span>
              <span className={a.cellSub}>{formatDateTime(g.endedAt)}</span>
            </span>
            <span className={a.cellStack}>
              <span className={a.cellMain}>
                <span className={a.dim}>vs </span>
                <OpponentCell entry={g} />
              </span>
              <span className={a.cellSub}>
                {g.colors ? (
                  <>
                    <span className={a.colorDot} style={{ backgroundColor: colorForIdentity(g.colors) }} />
                    {colorLabel(g.colors)}
                  </>
                ) : (
                  'Colors unknown'
                )}
              </span>
            </span>
            <span className={a.cellSub}>
              <EloCell entry={g} />
            </span>
            {renderActions && <span className={a.rowActions}>{renderActions(g)}</span>}
          </li>
        )
      })}
    </ul>
  )
}

/** Tournaments as clickable rows: name + mode and date, status, final placement. */
export function TournamentList({
  tournaments,
  onOpen,
}: {
  tournaments: UserTournamentEntry[]
  onOpen: (id: number) => void
}) {
  return (
    <ul className={a.list}>
      {tournaments.map((t, i) => {
        const mode = gameModeLabel(t.gameMode, t.format)
        const done = t.status === 'COMPLETED'
        return (
          <li key={`${t.id}-${i}`}>
            <button type="button" className={a.tournamentRow} onClick={() => onOpen(t.id)}>
              <span className={a.cellStack}>
                <span className={a.cellMain}>{t.name?.trim() || 'Tournament'}</span>
                <span className={a.cellSub}>
                  {mode.variant ?? mode.primary} · {formatDateTime(t.endedAt)}
                </span>
              </span>
              <span>{!done && <TournamentStatusBadge status={t.status} />}</span>
              <span className={a.place} data-first={done && t.placement === 1}>
                {done ? (
                  <>
                    {t.placement === 1 ? '🏆 ' : ''}
                    {t.placement}
                    <span className={a.dim} style={{ fontSize: 13 }}>/{t.playerCount}</span>
                  </>
                ) : (
                  <span className={a.dim}>—</span>
                )}
              </span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}

/** A glass dialog over a blurred backdrop; Escape or a backdrop click closes it. */
export function AccountModal({
  title,
  titleExtra,
  size = 'normal',
  onClose,
  children,
}: {
  title: React.ReactNode
  titleExtra?: React.ReactNode
  size?: 'small' | 'normal' | 'large' | 'xl'
  onClose: () => void
  children: React.ReactNode
}) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className={a.backdrop} onClick={onClose} role="presentation">
      <div className={a.dialog} data-size={size} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <div className={a.dialogHead}>
          <span className={a.dialogTitleRow}>
            <h2 className={a.dialogTitle}>{title}</h2>
            {titleExtra}
          </span>
          <button type="button" className={a.close} onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
