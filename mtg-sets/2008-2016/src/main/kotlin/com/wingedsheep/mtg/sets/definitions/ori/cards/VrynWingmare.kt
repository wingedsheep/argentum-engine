package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Vryn Wingmare
 * {2}{W}
 * Creature — Pegasus
 * 2/1
 * Flying
 * Noncreature spells cost {1} more to cast.
 */
val VrynWingmare = card("Vryn Wingmare") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Pegasus"
    power = 2
    toughness = 1
    oracleText = "Flying\nNoncreature spells cost {1} more to cast."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.Noncreature),
            modification = CostModification.IncreaseGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Seb McKinnon"
        flavorText = "It's the favored mount of military commanders as well as anyone with a flair for the dramatic."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a34291dc-103f-493d-b217-bd1b0e946d8d.jpg?1783938356"

        ruling("2020-06-23", "The ability affects each spell that's not a creature spell, including your own. Creature spells with another type, such as artifact creature spells, aren't affected.")
        ruling("2020-06-23", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases (such as that of Vryn Wingmare), then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
    }
}
