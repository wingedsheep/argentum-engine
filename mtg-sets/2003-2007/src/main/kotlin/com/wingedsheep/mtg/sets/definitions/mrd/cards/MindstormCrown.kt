package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mindstorm Crown — Mirrodin #207 (canonical printing)
 * {3} · Artifact
 *
 * At the beginning of your upkeep, draw a card if you had no cards in hand at the beginning of
 * this turn. If you had a card in hand, this artifact deals 1 damage to you.
 *
 * A card that pays you for emptying your hand and bills you for hoarding — and the whole design
 * hinges on *when* it looks. It is not "if you have no cards in hand": by the upkeep you may have
 * discarded to hand size, or been made to discard, or drawn from another upkeep trigger. The
 * measurement is frozen at the turn's start, which is why this needed
 * [Conditions.YouHadNoCardsInHandAtTurnStart] — a snapshot taken in the untap step (CR 502), not a
 * live hand read. `Conditions.EmptyHand` is the lookalike that resolves wrong on any turn where
 * something touched your hand before the upkeep.
 *
 * The two branches are exhaustive on the card's own terms ("no cards" versus "a card"), so one
 * [ConditionalEffect] covers both halves; the damage is dealt by the artifact to its own
 * controller, which is the default source, so no `damageSource` override is needed.
 */
val MindstormCrown = card("Mindstorm Crown") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, draw a card if you had no cards in hand at " +
        "the beginning of this turn. If you had a card in hand, this artifact deals 1 damage to you."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = ConditionalEffect(
            condition = Conditions.YouHadNoCardsInHandAtTurnStart,
            effect = Effects.DrawCards(1),
            elseEffect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
        )
        description = "At the beginning of your upkeep, draw a card if you had no cards in hand " +
            "at the beginning of this turn. If you had a card in hand, this artifact deals 1 " +
            "damage to you."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/347c1442-036b-43e7-9f86-b81a54d6bc41.jpg?1783944513"
    }
}
