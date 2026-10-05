package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Corpse Churn
 * {1}{B}
 * Instant — Common (OGW #83)
 *
 * "Mill three cards, then you may return a creature card from your graveyard to your hand."
 *
 * Implementation: the same shape as Grapple with the Past — mill first so the milled cards are
 * legal picks (per the ruling below), then a `chooseUpTo(1)` over the graveyard's creature cards.
 * "You may return" is "up to one" (declining = choosing zero); an empty pool auto-skips the prompt.
 * No "target", so the choice is made on resolution.
 */
val CorpseChurn = card("Corpse Churn") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Mill three cards, then you may return a creature card from your graveyard to your hand. " +
        "(To mill three cards, put the top three cards of your library into your graveyard.)"

    spell {
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(3))
            val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Creature))
            val chosen = chooseUpTo(
                1,
                from = creatures,
                showAllCards = true,
                prompt = "You may return a creature card from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Magali Villeneuve"
        flavorText = "\"The distraction of your life has ended. Now your service begins.\"\n—Kalitas, thrall of Ulamog"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b7e6054-c6b8-4b29-83c5-a2e4e295bdf3.jpg?1783937912"
        ruling("2020-11-10", "Because Corpse Churn doesn't target the creature card in your graveyard, you may choose one of the three cards you put there from your library if applicable.")
    }
}
