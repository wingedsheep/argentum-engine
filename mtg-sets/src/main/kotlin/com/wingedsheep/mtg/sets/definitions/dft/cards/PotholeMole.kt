package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pothole Mole
 * {2}{G}
 * Creature — Mole — Common (DFT #176)
 * 2/3
 *
 * When this creature enters, mill three cards, then you may return a land card from your graveyard
 * to your hand.
 *
 * Functional reprint of Eccentric Farmer (MID #185) under a new name, so it gets its own
 * `CardDefinition` with the same script shape: mill first (the freshly milled cards are legal
 * picks), then a `chooseUpTo(1)` over the land cards in your graveyard. "You may return a … card"
 * has no "target" in the oracle text, so it is a resolution-time choice modelled as "up to one" —
 * declining means choosing zero, and an empty pool auto-skips the prompt.
 */
val PotholeMole = card("Pothole Mole") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Mole"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, mill three cards, then you may return a land card from " +
        "your graveyard to your hand. (To mill three cards, put the top three cards of your library " +
        "into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(3))
            val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, Filters.Land))
            val chosen = chooseUpTo(
                1,
                from = lands,
                showAllCards = true,
                prompt = "You may return a land card from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
        description = "When this creature enters, mill three cards, then you may return a land card " +
            "from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Daren Bader"
        flavorText = "\"Someone please call pest control. Again.\"\n—Termin, track designer"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21c7b59f-fdae-4a11-9784-497a8165d75a.jpg?1783907868"
    }
}
