package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soratami Savant
 * {2}{U}{U}
 * Creature — Moonfolk Wizard
 * 2/2
 * Flying
 * {3}, Return a land you control to its owner's hand: Counter target spell unless its
 * controller pays {3}.
 */
val SoratamiSavant = card("Soratami Savant") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Moonfolk Wizard"
    oracleText = "Flying\n{3}, Return a land you control to its owner's hand: Counter target " +
        "spell unless its controller pays {3}."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    activatedAbility {
        val spell = target(TargetFilter.SpellOnStack)
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.ReturnToHand(Filters.Land))
        effect = Effects.CounterUnlessPays("{3}")
        description = "{3}, Return a land you control to its owner's hand: Counter target spell " +
            "unless its controller pays {3}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Jim Nelson"
        flavorText = "\"To prevent, one must first predict.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43ea2db8-67c6-4bcd-8ba1-f620b8e8a8c4.jpg?1783944321"
    }
}
