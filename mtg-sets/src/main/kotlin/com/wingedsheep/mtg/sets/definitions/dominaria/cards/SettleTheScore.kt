package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Settle the Score
 * {2}{B}{B}
 * Sorcery
 * Exile target creature. Put two loyalty counters on a planeswalker you control.
 *
 * Rulings:
 * - You don't choose which planeswalker receives loyalty counters until resolution.
 * - If you don't control a planeswalker, you simply exile the creature.
 * - Both loyalty counters go on the same planeswalker.
 */
val SettleTheScore = card("Settle the Score") {
    manaCost = "{2}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Exile target creature. Put two loyalty counters on a planeswalker you control."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Exile(creature)
            .then(Effects.SelectTarget(
                requirement = TargetObject(
                    filter = TargetFilter(GameObjectFilter.Planeswalker.youControl())
                ),
                storeAs = "chosenPW"
            ))
            .then(Effects.AddCountersToCollection("chosenPW", "loyalty", 2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Yongjae Choi"
        flavorText = "\"You bound me with a contract only your death could end—and you thought me the fool?\" —Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b18558f-6b40-4d1d-859a-3ba68950f064.jpg?1562732145"
    }
}
