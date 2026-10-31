package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Boreal Elemental (M20 #49)
 * {4}{U}  Creature — Elemental  3/4
 *
 * Flying
 * Spells your opponents cast that target this creature cost {2} more to cast.
 *
 * Same tax shape as Charix, the Raging Isle — applies once however many times the spell targets it.
 */
val BorealElemental = card("Boreal Elemental") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    oracleText = "Flying\n" +
        "Spells your opponents cast that target this creature cost {2} more to cast."
    power = 3
    toughness = 4

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Victor Adame Minguez"
        flavorText = "Winter's breath given form."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47996512-d547-491f-b481-a9c83dff04db.jpg?1783933014"
        ruling("2019-07-12", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases (such as that of Boreal Elemental), then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2019-07-12", "An opponent's spell that targets Boreal Elemental more than once costs only {2} more to cast.")
    }
}
