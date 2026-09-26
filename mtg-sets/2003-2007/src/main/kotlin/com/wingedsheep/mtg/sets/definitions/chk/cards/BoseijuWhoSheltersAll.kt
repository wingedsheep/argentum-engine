package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Boseiju, Who Shelters All
 * Legendary Land
 * Boseiju enters tapped.
 * {T}, Pay 2 life: Add {C}. If that mana is spent on an instant or sorcery spell, that spell can't
 * be countered.
 *
 * Unlike Cavern of Souls the {C} is unrestricted — it pays for anything — so the instant-or-sorcery
 * test lives on the rider, not on a spending restriction: spent on a creature spell it simply does
 * nothing.
 */
val BoseijuWhoSheltersAll = card("Boseiju, Who Shelters All") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Legendary Land"
    oracleText = "Boseiju enters tapped.\n" +
        "{T}, Pay 2 life: Add {C}. If that mana is spent on an instant or sorcery spell, that spell can't be countered."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(2))
        effect = Effects.AddColorlessMana(
            1,
            riders = setOf(ManaSpellRider.MakesSpellUncounterable(GameObjectFilter.InstantOrSorcery))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "273"
        artist = "Ralph Horsley"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0180d9a8-992c-4d55-8ac4-33a587786993.jpg?1783944274"
        ruling("2004-12-01", "The spell can't be countered if the mana produced by Boseiju is spent to cover any cost of the spell, even an additional cost such as a splice cost. This is true even if you pay an additional cost while casting a spell \"without paying its mana cost.\"")
        ruling("2004-12-01", "Suppose mana produced by Boseiju is spent on a spell and an effect creates a copy of that spell. The copy can be countered, even though the original can't.")
    }
}
