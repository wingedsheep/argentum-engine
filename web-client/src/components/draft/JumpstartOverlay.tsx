import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore'
import { getCdnArtCropUrl } from '@/utils/cardImages'
import type { SealedCardInfo } from '@/types'
import styles from './JumpstartOverlay.module.css'

function PackList({ cards }: { cards: readonly SealedCardInfo[] }) {
  const groups = new Map<string, { card: SealedCardInfo; count: number }>()
  for (const card of cards) {
    const group = groups.get(card.name)
    groups.set(card.name, { card, count: (group?.count ?? 0) + 1 })
  }
  return <ul className={styles.cardList}>{Array.from(groups.values()).map(({ card, count }) => (
    <li key={card.name}>
      <span className={styles.count}>{count}×</span>
      <details className={styles.cardDetail}>
        <summary>{card.name}</summary>
        <div className={styles.cardPreview}>
          {card.imageUri && <img src={card.imageUri} alt={card.name} loading="lazy" />}
          <div><p>{card.typeLine}</p><p className={styles.oracle}>{card.oracleText}</p></div>
        </div>
      </details>
    </li>
  ))}</ul>
}

/** Pack legality, offers and deck assembly all come from the server. */
export function JumpstartOverlay() {
  const lobby = useGameStore((s) => s.lobbyState)
  const pick = useGameStore((s) => s.pickJumpstartPack)
  const leave = useGameStore((s) => s.leaveLobby)
  const error = useGameStore((s) => s.lastError)
  const connection = useGameStore((s) => s.connectionStatus)
  const [pending, setPending] = useState<string | null>(null)
  const state = lobby?.jumpstart
  const roundKey = `${lobby?.lobbyId}:${state?.pickNumber}`
  useEffect(() => { setPending(null) }, [roundKey, error, connection])
  if (!state || !lobby) return null
  const busy = pending !== null
  const ready = state.selectedPacks.length === 2
  const setName = lobby.settings.setNames.join(' + ')
  return (
    <div className={styles.overlay}>
      <main className={styles.content}>
        <header className={styles.header}>
          <div><span className={styles.eyebrow}>JUMP IN</span><span className={styles.setName}>{setName}</span></div>
          <button className={styles.leave} onClick={leave}>Leave lobby</button>
        </header>
        <ol className={styles.steps} aria-label="Your deck progress">
          {['First theme', 'Second theme', 'Play'].map((label, i) => <li key={label}
            className={state.selectedPacks.length >= i ? styles.currentStep : ''}
            aria-current={state.selectedPacks.length === i ? 'step' : undefined}>
            <span>{state.selectedPacks.length > i ? '✓' : i + 1}</span>{label}
          </li>)}
        </ol>
        <h1>{ready ? 'Two themes. One deck. Let’s play.' : `Choose your ${state.pickNumber === 1 ? 'first' : 'second'} theme`}</h1>
        <p className={styles.intro}>
          {ready ? 'Your 40-card deck is ready. The game starts when everyone has chosen.'
            : state.pickNumber === 1 ? 'Find a theme you love. You’ll pair it with a second pack to make your deck — all the lands are included.'
              : 'Add a second theme to complete your deck. Mix colors, or double down on a favorite.'}
        </p>
        {state.selectedPacks.length > 0 && <div className={styles.selected} aria-label="Chosen themes">
          {state.selectedPacks.map((name, i) => <span key={`${i}-${name}`}>✓ {name}</span>)}
          {!ready && <span className={styles.emptySlot}>+ Your next theme</span>}
        </div>}
        {connection !== 'connected' && <p role="status" className={styles.intro}>Reconnecting… Your choices are saved.</p>}
        {error && <p className={styles.error} role="alert">{error.message}</p>}
        <div className={styles.packs} aria-busy={busy}>
          {state.offers.map((offer) => {
            const face = offer.cards.find((card) => card.rarity === 'RARE' || card.rarity === 'MYTHIC') ?? offer.cards[0]
            return <article className={styles.pack} key={`${state.pickNumber}-${offer.id}`}>
              <div className={styles.artFrame}>
                {face?.imageUri && <img src={getCdnArtCropUrl(face.imageUri) ?? face.imageUri} alt="" className={styles.art} />}
                <span className={styles.packLabel}>20 CARDS · LANDS INCLUDED</span>
              </div>
              <div className={styles.packBody}>
                <h2>{offer.theme}</h2>
                <p className={styles.featured}>{face?.name}</p>
                <button className={styles.choose} disabled={busy || connection !== 'connected'} onClick={() => {
                  if (busy) return
                  setPending(offer.id)
                  pick(offer.id, state.pickNumber)
                }}>{pending === offer.id ? 'Choosing…' : `Choose ${offer.theme}`}</button>
                <details className={styles.list}>
                  <summary>Explore this pack <span aria-hidden>⌄</span></summary>
                  <PackList cards={offer.cards} />
                </details>
              </div>
            </article>
          })}
        </div>
        {ready && <section className={styles.waiting} aria-label="Player readiness">
          <h2>At the table</h2>
          <ul>{lobby.players.map((player) => <li key={player.playerId}>
            <span>{player.playerName}{player.isAi ? ' · AI' : ''}</span>
            <span className={player.deckSubmitted ? styles.ready : styles.picking}>
              {player.deckSubmitted ? '✓ Ready to play' : 'Choosing themes…'}
            </span>
          </li>)}</ul>
          <p role="status">Waiting for the rest of the table</p>
        </section>}
        <p className={styles.note}>Published Jumpstart packs · Two picks · No deckbuilding</p>
      </main>
    </div>
  )
}
