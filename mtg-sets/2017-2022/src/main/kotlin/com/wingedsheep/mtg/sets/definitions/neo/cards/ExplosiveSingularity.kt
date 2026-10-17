package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Explosive Singularity — Kamigawa: Neon Dynasty #140 (canonical printing)
 * {8}{R}{R} · Sorcery
 *
 * As an additional cost to cast this spell, you may tap any number of untapped creatures you
 * control. This spell costs {1} less to cast for each creature tapped this way.
 * Explosive Singularity deals 10 damage to any target.
 *
 * Tapping is a cost, not the {T} symbol, so creatures that came under your control this turn may
 * pay it. The discount only takes generic mana, so {R}{R} is always paid.
 */
val ExplosiveSingularity = card("Explosive Singularity") {
    manaCost = "{8}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, you may tap any number of untapped " +
        "creatures you control. This spell costs {1} less to cast for each creature tapped this way.\n" +
        "Explosive Singularity deals 10 damage to any target."

    additionalCost(Costs.additional.TapAnyNumberForCostReduction(GameObjectFilter.Creature, reductionPerUnit = 1))

    spell {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(10, any)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "140"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6cdd822-44a1-4d58-9de4-69fc56eae255.jpg?1783923869"
    }
}
