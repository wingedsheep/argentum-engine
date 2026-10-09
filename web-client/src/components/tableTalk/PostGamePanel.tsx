import { useState } from 'react'
import type { PostGameMessage } from '@/types'
import { AvatarArt, hasAvatarArt } from '@/components/profile/AvatarArt'
import { initialOf } from '@/components/profile/avatars'
import { useGameStore } from '@/store/gameStore'
import { useTableTalkStore } from '@/store/tableTalkStore'
import styles from './TableTalk.module.css'

/**
 * Your opponent, on the result screen of a human 1v1: rematch, add friend, block. Everything shown
 * is the server's [PostGameMessage]; the buttons only ask. The rematch is the card's one primary
 * action — when your opponent has already asked for one it says so and glows, because answering
 * is all that's left.
 */
export function PostGamePanel({ postGame }: { postGame: PostGameMessage }) {
  const requestRematch = useTableTalkStore((s) => s.requestRematch)
  const addFriend = useTableTalkStore((s) => s.addFriend)
  const block = useTableTalkStore((s) => s.block)
  const [confirmingBlock, setConfirmingBlock] = useState(false)
  // A 1v1, so the opponent's portrait is the one seat avatar that isn't ours.
  const opponentAvatar = useGameStore((s) =>
    Object.entries(s.avatarByPlayerId).find(([id]) => id !== s.playerId)?.[1],
  )
  const portrait = hasAvatarArt(opponentAvatar)
  const name = postGame.opponentName
  const { rematch, opponentLeft, blocked } = postGame

  return (
    <section className={styles.postGame} aria-label={`Your opponent, ${name}`} data-testid="post-game">
      <div className={styles.opponentRow}>
        <span className={styles.avatar} data-portrait={portrait} aria-hidden>
          {portrait ? <AvatarArt avatar={opponentAvatar} /> : initialOf(name)}
        </span>
        <span className={styles.opponentText}>
          <span className={styles.opponentName}>{name}</span>
          <span className={styles.opponentStatus} data-tone={statusTone(postGame)}>{statusLine(postGame)}</span>
        </span>
        {!blocked && <FriendChip postGame={postGame} onAdd={addFriend} />}
      </div>

      {postGame.notice && <p className={styles.notice} role="status">{postGame.notice}</p>}

      {postGame.canRematch && !blocked && (
        <RematchButton
          name={name}
          rematch={rematch}
          disabled={opponentLeft}
          onRequest={() => requestRematch(true)}
          onWithdraw={() => requestRematch(false)}
        />
      )}

      <div className={styles.blockRow}>
        {blocked ? (
          <>
            <span className={styles.blockedNote}>
              Blocked — you won’t be matched with {name} again or see their emotes.
            </span>
            <button type="button" className={styles.linkButton} onClick={() => block(false)}>Undo</button>
          </>
        ) : confirmingBlock ? (
          <>
            <span className={styles.blockedNote}>Block {name}? You won’t be matched again or see their emotes.</span>
            <button type="button" className={styles.linkButton} onClick={() => setConfirmingBlock(false)}>Cancel</button>
            <button
              type="button"
              className={`${styles.linkButton} ${styles.danger}`}
              onClick={() => { block(true); setConfirmingBlock(false) }}
              data-testid="post-game-block-confirm"
            >
              Block
            </button>
          </>
        ) : (
          <button
            type="button"
            className={styles.linkButton}
            onClick={() => setConfirmingBlock(true)}
            data-testid="post-game-block"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" aria-hidden fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
              <circle cx="12" cy="12" r="9" />
              <path d="M5.6 5.6l12.8 12.8" />
            </svg>
            Block player
          </button>
        )}
      </div>
    </section>
  )
}

function RematchButton({
  name,
  rematch,
  disabled,
  onRequest,
  onWithdraw,
}: {
  name: string
  rematch: PostGameMessage['rematch']
  disabled: boolean
  onRequest: () => void
  onWithdraw: () => void
}) {
  if (rematch.starting) {
    return (
      <button type="button" className={styles.rematch} disabled data-state="starting">
        <span className={styles.rematchSpinner} aria-hidden />
        Starting rematch…
      </button>
    )
  }
  if (disabled) {
    return (
      <button type="button" className={styles.rematch} disabled data-state="gone">
        Rematch unavailable
      </button>
    )
  }
  if (rematch.you) {
    return (
      <button
        type="button"
        className={styles.rematch}
        data-state="waiting"
        onClick={onWithdraw}
        title="Click to withdraw"
        data-testid="post-game-rematch"
      >
        <span className={styles.rematchSpinner} aria-hidden />
        <span>Waiting for {name}…</span>
        <span className={styles.rematchHint}>Cancel</span>
      </button>
    )
  }
  return (
    <button
      type="button"
      className={styles.rematch}
      data-state={rematch.opponent ? 'invited' : 'idle'}
      onClick={onRequest}
      autoFocus
      data-testid="post-game-rematch"
    >
      <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
        <path d="M3 12a9 9 0 0 1 15.5-6.3L21 8" />
        <path d="M21 3v5h-5" />
        <path d="M21 12a9 9 0 0 1-15.5 6.3L3 16" />
        <path d="M3 21v-5h5" />
      </svg>
      {rematch.opponent ? 'Accept rematch' : 'Rematch'}
    </button>
  )
}

function FriendChip({ postGame, onAdd }: { postGame: PostGameMessage; onAdd: () => void }) {
  switch (postGame.friendship) {
    case 'UNAVAILABLE':
      return null
    case 'FRIENDS':
      return <span className={styles.friendChip} data-state="done">✓ Friends</span>
    case 'REQUEST_SENT':
      return <span className={styles.friendChip} data-state="done">Request sent</span>
    case 'REQUEST_RECEIVED':
      return (
        <button type="button" className={styles.friendChip} data-state="invited" onClick={onAdd} data-testid="post-game-add-friend">
          Accept friend
        </button>
      )
    case 'NONE':
      return (
        <button type="button" className={styles.friendChip} onClick={onAdd} data-testid="post-game-add-friend">
          <svg width="13" height="13" viewBox="0 0 24 24" aria-hidden fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
            <circle cx="9" cy="8" r="4" />
            <path d="M2 21a7 7 0 0 1 14 0" />
            <path d="M19 8v6M16 11h6" />
          </svg>
          Add friend
        </button>
      )
  }
}

function statusLine(pg: PostGameMessage): string {
  if (pg.blocked) return 'Blocked'
  if (pg.rematch.starting) return 'Rematch starting'
  if (pg.opponentLeft) return 'Left the table'
  if (pg.rematch.opponent && !pg.rematch.you) return 'Wants a rematch!'
  if (pg.friendship === 'REQUEST_RECEIVED') return 'Sent you a friend request'
  if (pg.rematch.you) return 'Deciding…'
  return 'Still at the table'
}

function statusTone(pg: PostGameMessage): 'live' | 'gone' | 'quiet' {
  if (pg.blocked || pg.opponentLeft) return 'gone'
  if ((pg.rematch.opponent && !pg.rematch.you) || pg.friendship === 'REQUEST_RECEIVED') return 'live'
  return 'quiet'
}
