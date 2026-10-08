package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Goblin Anarchomancer
 * {R}{G}
 * Creature — Goblin Shaman
 * 2/2
 *
 * Each spell you cast that's red or green costs {1} less to cast.
 *
 * The Medallion shape (Ruby Medallion) over a two-colour OR filter: `withAnyColor` is one
 * predicate, so a spell that's both red and green matches once and costs only {1} less (ruling).
 */
val GoblinAnarchomancer = card("Goblin Anarchomancer") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Goblin Shaman"
    power = 2
    toughness = 2
    oracleText = "Each spell you cast that's red or green costs {1} less to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withAnyColor(Color.RED, Color.GREEN)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "200"
        artist = "Joe Slucher"
        flavorText = "A good Gruul shaman can work a whole clan into a frenzy. A great one will wait until the enemy's nearby."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/633a3423-501d-4b22-95a6-743233be521e.jpg?1783926814"
        ruling("2021-06-18", "A spell that's both red and green costs {1} less to cast.")
    }
}
