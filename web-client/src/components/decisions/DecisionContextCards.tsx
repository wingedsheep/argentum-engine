import type { ClientCard, ClientGameState, DecisionContext, EntityId } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import styles from './DecisionUI.module.css'

/**
 * The cards a decision prompt can illustrate itself with, resolved against the (already masked)
 * client state — the server sends only ids, so a face-down permanent stays face-down here.
 *
 * Three distinct roles, deliberately not collapsed into one:
 *  - `source` — the spell or ability doing the asking (Killing Wave).
 *  - `subject` — the object *this* instance of the prompt is about, when one effect asks the same
 *    question once per object. Without it a board of five creatures produces five
 *    character-identical prompts and the player is guessing which one they're answering for.
 *  - `triggering` — what caused the ability to trigger (the blocked creature, the aura's host).
 */
export interface DecisionCards {
  readonly targets?: readonly {
    id: EntityId
    name: string
    entry?: { card: ClientCard; imageUrl: string } | undefined
  }[] | undefined
  readonly source?: { card: ClientCard; imageUrl: string } | undefined
  readonly subject?: { card: ClientCard; imageUrl: string } | undefined
  readonly triggering?: { card: ClientCard; imageUrl: string } | undefined
}

function resolve(
  entityId: EntityId | undefined,
  gameState: ClientGameState | null,
): { card: ClientCard; imageUrl: string } | undefined {
  if (!entityId) return undefined
  const card = gameState?.cards[entityId]
  if (!card) return undefined
  const imageUrl = getCardImageUrl(card.name, card.imageUri)
  return imageUrl ? { card, imageUrl } : undefined
}

export function resolveDecisionCards(
  context: DecisionContext,
  gameState: ClientGameState | null,
): DecisionCards {
  const source = resolve(context.sourceId, gameState)
  const subject = resolve(context.subjectEntityId, gameState)
  const triggering = resolve(context.triggeringEntityId, gameState)
  const targets = (context.targetIds ?? []).flatMap((id) => {
    const card = gameState?.cards[id]
    const player = gameState?.players.find((candidate) => candidate.playerId === id)
    const name = card?.name ?? player?.name
    return name ? [{ id, name, entry: resolve(id, gameState) }] : []
  })
  return {
    targets,
    source,
    subject,
    // The subject and the triggering entity are the same card often enough (a per-entity loop over
    // the very thing that triggered) that showing it twice would just be noise.
    triggering: triggering && triggering.card.id !== source?.card.id && triggering.card.id !== subject?.card.id
      ? triggering
      : undefined,
  }
}

/** A context card image, turned to read landscape when its shown face is printed sideways. */
function ContextCardImage({ entry, className }: { entry: { card: ClientCard; imageUrl: string }; className: string | undefined }) {
  const landscape = entry.card.isLandscapeFace === true
  return (
    <img
      src={entry.imageUrl}
      alt={entry.card.name}
      className={landscape ? `${className ?? ''} ${styles.contextCardLandscape}` : className}
    />
  )
}

export function hasDecisionContextCards(cards: DecisionCards): boolean {
  return cards.source != null || cards.subject != null || cards.triggering != null || (cards.targets?.length ?? 0) > 0
}

/**
 * The card strip above a decision prompt. The subject is rendered largest and ringed in
 * `--color-decision-subject` — the same orange `GameCard` puts around it on the battlefield, so
 * minimizing the modal to look at the board keeps the connection.
 */
export function DecisionContextCards({ cards, showSourceBackFace = false }: {
  cards: DecisionCards
  /**
   * Also show the source's back face — for "cast it transformed" (a defeated Siege, CR 310.12b),
   * where the face being offered is the one the player can't see on the source card.
   */
  showSourceBackFace?: boolean
}) {
  if (!hasDecisionContextCards(cards)) return null

  const source = cards.source
  const backFaceUrl = showSourceBackFace && source?.card.backFaceImageUri
    ? getCardImageUrl(source.card.backFaceName ?? source.card.name, source.card.backFaceImageUri)
    : null
  const backFace = source && backFaceUrl
    ? {
        card: { ...source.card, name: source.card.backFaceName ?? source.card.name, isLandscapeFace: source.card.backFaceIsLandscape === true },
        imageUrl: backFaceUrl,
      }
    : null

  // A lone source card needs no caption — the prompt right below it already says what it does.
  // Once a second role is on screen, every card gets labelled so the roles can't be confused.
  const labelled = [cards.source, cards.subject, cards.triggering, backFace].filter(Boolean).length + (cards.targets?.length ?? 0) > 1

  return (
    <div className={styles.contextCards}>
      {cards.source && (
        <div className={styles.contextCard}>
          {labelled && <p className={styles.contextCardLabel}>Source</p>}
          <ContextCardImage entry={cards.source} className={styles.contextCardImage} />
        </div>
      )}

      {backFace && (
        <div className={styles.contextCard}>
          <p className={styles.contextCardLabelSubject}>Casts as</p>
          <ContextCardImage entry={backFace} className={styles.contextCardImageSubject} />
          <p className={styles.contextCardName}>{backFace.card.name}</p>
        </div>
      )}

      {cards.triggering && (
        <div className={styles.contextCard}>
          <p className={styles.contextCardLabel}>Triggered by</p>
          <ContextCardImage entry={cards.triggering} className={styles.contextCardImageSecondary} />
        </div>
      )}

      {cards.targets?.map((target, index) => (
        <div className={styles.contextCard} key={`${target.id}-${index}`}>
          <p className={styles.contextCardLabel}>Target</p>
          {target.entry && <ContextCardImage entry={target.entry} className={styles.contextCardImageSecondary} />}
          <p className={styles.contextCardName}>{target.name}</p>
        </div>
      ))}

      {cards.subject && (
        <div className={styles.contextCard}>
          <p className={styles.contextCardLabelSubject}>Deciding for</p>
          <ContextCardImage entry={cards.subject} className={styles.contextCardImageSubject} />
          <p className={styles.contextCardName}>{cards.subject.card.name}</p>
        </div>
      )}
    </div>
  )
}
