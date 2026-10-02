package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Abstruse Appropriation
 * {2}{W}{B}
 * Instant
 * Devoid (This card has no color.)
 * Exile target nonland permanent. You may cast that card for as long as it remains exiled, and
 * you may spend colorless mana as though it were mana of any color to cast that spell.
 */
val AbstruseAppropriation = card("Abstruse Appropriation") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Exile target nonland permanent. You may cast that card for as long as it remains exiled, " +
        "and you may spend colorless mana as though it were mana of any color to cast that spell."

    keywords(Keyword.DEVOID)

    spell {
        target(TargetFilter.NonlandPermanent)
        effect = Effects.Pipeline {
            val appropriated = gather(CardSource.ChosenTargets)
            exile(appropriated)
            run(Effects.GrantMayPlayFromExile(
                from = appropriated,
                expiry = MayPlayExpiry.Permanent,
                // "cast", not "play" — a land card exiled this way can't be played.
                nonLandOnly = true,
                colorlessAsAnyColor = true
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Jason Rainville"
        flavorText = "\"Kozilek experiences reality on his own terms.\"\n—Ayli, high priest of the Eternal Pilgrims"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7149359-bb3f-413c-b21d-e4cd88618e56.jpg?1783911253"
        ruling("2024-06-07", "If a token is exiled this way, it will cease to exist and won't return to the battlefield. You can't cast it.")
        ruling("2024-06-07", "Abstruse Appropriation doesn't change when you can cast the exiled card. For example, if you exile a creature card without flash, you can cast it only during your main phase when the stack is empty.")
        ruling("2024-06-07", "In the rare case where the exiled card is a land card (probably because that land became a copy of a permanent while it was still on the battlefield), you won't be able to play it from exile.")
    }
}
