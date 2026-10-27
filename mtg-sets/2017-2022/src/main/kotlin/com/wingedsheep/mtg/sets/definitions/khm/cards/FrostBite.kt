package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Frost Bite
 * {R}
 * Snow Instant
 * Frost Bite deals 2 damage to target creature or planeswalker. If you control three or more snow
 * permanents, it deals 3 damage instead.
 *
 * The snow-count check happens on resolution (ruling), so it's an [Effects.If] over
 * [Conditions.YouControlAtLeast] choosing between 3 and 2 damage to the one target.
 */
val FrostBite = card("Frost Bite") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Snow Instant"
    oracleText = "Frost Bite deals 2 damage to target creature or planeswalker. " +
        "If you control three or more snow permanents, it deals 3 damage instead."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.YouControlAtLeast(3, GameObjectFilter.Permanent.snow()),
            then = Effects.DealDamage(3, t),
            otherwise = Effects.DealDamage(2, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Caio Monteiro"
        flavorText = "\"Don't wander far—it's a bit nippy out there!\"\n—Leidurr, expedition leader"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9423318a-c5a8-48d2-92f5-280d15a050a6.jpg?1783928228"

        ruling("2021-02-05", "Whether or not you control three or more snow permanents is checked as Frost Bite is resolving.")
    }
}
