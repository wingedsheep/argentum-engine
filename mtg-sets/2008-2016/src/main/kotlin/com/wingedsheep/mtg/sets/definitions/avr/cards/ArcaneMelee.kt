package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Arcane Melee
 * {4}{U}
 * Enchantment
 * Instant and sorcery spells cost {2} less to cast.
 *
 * Every player's instants and sorceries get the discount — hence [SpellCostTarget.AnyCaster],
 * the same global reducer Urza's Incubator uses. `ReduceGeneric` only shaves generic mana, as the
 * rulings require.
 */
val ArcaneMelee = card("Arcane Melee") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Instant and sorcery spells cost {2} less to cast."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Jaime Jones"
        flavorText = "Debates between wizards are never purely academic."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32355574-34ed-4427-86d9-72295d32ac4f.jpg?1783940724"

        ruling("2012-05-01", "Arcane Melee can't reduce the colored mana requirement of an instant or sorcery spell.")
        ruling(
            "2012-05-01",
            "If there are additional costs to cast a spell, such as a kicker cost or a cost imposed by another " +
                "effect (such as Thalia, Guardian of Thraben's ability), apply those increases before applying " +
                "cost reductions."
        )
        ruling("2012-05-01", "Arcane Melee can reduce alternative costs such as miracle costs.")
    }
}
