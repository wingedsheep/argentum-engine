package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * It That Heralds the End
 * {1}{C}
 * Creature — Eldrazi Drone
 * 2/2
 *
 * Colorless spells you cast with mana value 7 or greater cost {1} less to cast.
 * Other colorless creatures you control get +1/+1.
 *
 * Modelling: the discount is the Medallion `ModifySpellCost` static over a colorless +
 * mana-value-at-least-7 spell filter; `ReduceGeneric` only shaves generic mana. The anthem is a
 * `ModifyStats` over colorless creatures you control, excluding itself.
 */
val ItThatHeraldsTheEnd = card("It That Heralds the End") {
    manaCost = "{1}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Colorless spells you cast with mana value 7 or greater cost {1} less to cast.\n" +
        "Other colorless creatures you control get +1/+1."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(
                GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless).manaValueAtLeast(7)
            ),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).youControl(),
                excludeSelf = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "9"
        artist = "Alex Konstad"
        flavorText = "\"At first we ignored the little ones. By the time we realized our error, it was too late.\"\n—General Tazri, allied commander"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8c47679-0fac-466f-be3c-794f23576e55.jpg?1783911307"
    }
}
