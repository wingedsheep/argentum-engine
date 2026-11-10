package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Creeping Dread (Shadows over Innistrad #104)
 * {3}{B} · Enchantment
 *
 * At the beginning of your upkeep, each player discards a card. Each opponent who discarded a card
 * that shares a card type with the card you discarded loses 3 life. (Players reveal the discarded
 * cards simultaneously.)
 *
 * The discard is Balance's "each player chooses, then all discard at once" shape:
 * `forEachPlayerCollecting(ActivePlayerFirst)` has every player, in APNAP order, pick one card
 * from their own hand while it stays hidden there, and a single `discard` then moves all the
 * picks at once — so nobody sees another player's card before choosing (the 2016-04-08 ruling).
 * A player with an empty hand contributes nothing.
 *
 * The payoff reads the discards after they land: the controller's own card is the one in the
 * aggregate they own, and each opponent's card that `sharingCardTypeWith` it is collected. Every
 * player discards at most one card, so one `LoseLife(3)` per matching card charged to its owner is
 * exactly "each opponent who discarded … loses 3 life" — an artifact creature sharing two types
 * still costs only 3. If you discarded nothing, the `PipelineTarget` reference resolves to nothing
 * and `SharesCardTypeWith` matches nothing, so no one loses life. Only card types are compared
 * (supertypes and subtypes aren't), and a double-faced card in the graveyard shows its front face.
 */
val CreepingDread = card("Creeping Dread") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, each player discards a card. Each opponent who " +
        "discarded a card that shares a card type with the card you discarded loses 3 life. " +
        "(Players reveal the discarded cards simultaneously.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val (discarded) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                listOf(
                    chooseExactly(
                        1,
                        from = hand,
                        chooser = Chooser.Controller,
                        prompt = "Choose a card to discard"
                    )
                )
            }
            discard(discarded)
            val yours = filter(discarded, GameObjectFilter.Any.ownedByYou())
            val matching = filter(
                discarded,
                GameObjectFilter.Any.ownedByOpponent().sharingCardTypeWith(yours.asTarget)
            )
            run(
                Effects.ForEachInCollection(
                    matching,
                    Effects.LoseLife(3, EffectTarget.PlayerRef(Player.OwnerOfIterationEntity))
                )
            )
        }
        description = "At the beginning of your upkeep, each player discards a card. Each opponent " +
            "who discarded a card that shares a card type with the card you discarded loses 3 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b3376c4-369a-4d63-bd48-966bcfb0c9b9.jpg?1783937777"
        ruling(
            "2016-04-08",
            "When a spell or ability instructs each player to discard a card, starting with the player " +
                "whose turn it is and proceeding in turn order, each player selects a card from their hand " +
                "without revealing it, sets it aside, and then all of those cards are revealed and " +
                "discarded at once."
        )
        ruling(
            "2016-04-08",
            "If a player has no cards in hand, that player doesn't discard a card while everyone else " +
                "does. If you didn't discard a card, no player can have discarded a card that shares a " +
                "card type with the card you discarded, so no player loses 3 life."
        )
        ruling(
            "2016-04-08",
            "If an opponent discards a card that shares two card types with the card you discarded, " +
                "such as an artifact creature, that player still only loses 3 life."
        )
    }
}
