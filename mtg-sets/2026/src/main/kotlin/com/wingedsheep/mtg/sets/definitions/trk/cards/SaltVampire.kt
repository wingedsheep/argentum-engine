package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Salt Vampire
 * {1}{B}
 * Creature — Vampire Shapeshifter
 * 2/2
 *
 * Lifelink
 * When this creature enters, look at target opponent's hand. You may choose a nonland card from
 * it. If you do, that player exiles that card, then draws a card.
 *
 * Oildeep Gearhulk's look-choose-trade pipeline, aimed at an opponent, limited to nonland cards,
 * and exiling instead of discarding: [Effects.LookAtHand] (only the controller sees the hand), an
 * up-to-one nonland pick, and an `ifNotEmpty` so the "if you do" rider holds — no card chosen
 * (or no nonland card to choose) means no exile and no draw. The draw is the opponent's.
 */
val SaltVampire = card("Salt Vampire") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shapeshifter"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "When this creature enters, look at target opponent's hand. You may choose a nonland card " +
        "from it. If you do, that player exiles that card, then draws a card."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.LookAtHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosenCard = chooseUpTo(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "You may choose a nonland card for that player to exile",
                alwaysPrompt = true,
                showAllCards = true
            )
            ifNotEmpty(chosenCard) {
                exile(chosenCard, owner = opponent.asPlayer)
                run(Effects.DrawCards(1, opponent))
            }
        }
        description = "When this creature enters, look at target opponent's hand. You may choose a " +
            "nonland card from it. If you do, that player exiles that card, then draws a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Bastien L. Deharme"
        flavorText = "After the collapse of their civilization, the people of M-113 became little " +
            "more than predators, but they never abandoned their need for love."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78faba81-01a3-489f-927e-1adaa8fe4436.jpg?1784036770"
    }
}
