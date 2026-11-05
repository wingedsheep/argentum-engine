package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scribe of the Mindful
 * {2}{U}
 * Creature — Human Cleric
 * 2/2
 * {1}, {T}, Sacrifice this creature: Return target instant or sorcery card from your graveyard to
 * your hand.
 */
val ScribeOfTheMindful = card("Scribe of the Mindful") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "{1}, {T}, Sacrifice this creature: Return target instant or sorcery card from your " +
        "graveyard to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        val t = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Move(t, Zone.HAND)
        description = "Return target instant or sorcery card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Bastien L. Deharme"
        flavorText = "\"The best answers solve more than one riddle.\"\n—Kefnet, god of knowledge"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34ffaefe-1b9e-418d-9760-b7d546f03542.jpg"
    }
}
