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
  return {
    source,
    subject,
    // The subject and the triggering entity are the same card often enough (a per-entity loop over
    // the very thing that triggered) that showing it twice would just be noise.
    triggering: triggering && triggering.card.id !== source?.card.id && triggering.card.id !== subject?.card.id
      ? triggering
      : undefined,
  }
}

export function hasDecisionContextCards(cards: DecisionCards): boolean {
  return cards.source != null || cards.subject != null || cards.triggering != null
}

/**
 * The card strip above a decision prompt. The subject is rendered largest and ringed in
 * `--color-decision-subject` — the same orange `GameCard` puts around it on the battlefield, so
 * minimizing the modal to look at the board keeps the connection.
 */
export function DecisionContextCards({ cards }: { cards: DecisionCards }) {
  if (!hasDecisionContextCards(cards)) return null

  // A lone source card needs no caption — the prompt right below it already says what it does.
  // Once a second role is on screen, every card gets labelled so the roles can't be confused.
  const labelled = [cards.source, cards.subject, cards.triggering].filter(Boolean).length > 1

  return (
    <div className={styles.contextCards}>
      {cards.source && (
        <div className={styles.contextCard}>
          {labelled && <p className={styles.contextCardLabel}>Source</p>}
          <img
            src={cards.source.imageUrl}
            alt={cards.source.card.name}
            className={styles.contextCardImage}
          />
        </div>
      )}

      {cards.triggering && (
        <div className={styles.contextCard}>
          <p className={styles.contextCardLabel}>Triggered by</p>
          <img
            src={cards.triggering.imageUrl}
            alt={cards.triggering.card.name}
            className={styles.contextCardImageSecondary}
          />
        </div>
      )}

      {cards.subject && (
        <div className={styles.contextCard}>
          <p className={styles.contextCardLabelSubject}>Deciding for</p>
          <img
            src={cards.subject.imageUrl}
            alt={cards.subject.card.name}
            className={styles.contextCardImageSubject}
          />
          <p className={styles.contextCardName}>{cards.subject.card.name}</p>
        </div>
      )}
    </div>
  )
}
