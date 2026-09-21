package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

val GeistOfSaintThalia = card("Geist of Saint Thalia") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Spirit Cleric"
    oracleText = "Flying\nNoncreature spells you cast cost {1} less to cast."
    power = 1
    toughness = 2

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Noncreature),
            modification = CostModification.ReduceGeneric(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Ryan Pancoast"
        flavorText = "\"Be at peace, child. The horrors will not claim you this night.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c334530-0880-46b5-a358-9603eee3cecf.jpg?1789128026"
        inBooster = false
    }
}
