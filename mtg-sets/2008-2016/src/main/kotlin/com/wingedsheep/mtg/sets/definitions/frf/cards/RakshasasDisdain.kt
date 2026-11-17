package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rakshasa's Disdain — Fate Reforged #45
 * {2}{U} · Instant
 *
 * Counter target spell unless its controller pays {1} for each card in your graveyard.
 *
 * "Your" is Rakshasa's Disdain's controller, so the tax counts the counterspeller's graveyard while
 * the spell's controller pays it. [Effects.CounterUnlessDynamicPays] evaluates the amount as the
 * counter resolves, when Rakshasa's Disdain is still on the stack and so does not count itself.
 */
val RakshasasDisdain = card("Rakshasa's Disdain") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1} for each card in your graveyard."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.cardsInYourGraveyard())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Seb McKinnon"
        flavorText = "\"Every failure, every death only makes my strength greater.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd9e8a25-2e71-431b-897f-8b62520a3ce9.jpg?1783938704"
        ruling(
            "2014-11-24",
            "Count the number of cards in your graveyard as Rakshasa's Disdain resolves to determine how much " +
                "mana the controller of the target spell must pay to avoid the spell being countered. " +
                "Rakshasa's Disdain is still on the stack at this time and won't count toward this number."
        )
    }
}
