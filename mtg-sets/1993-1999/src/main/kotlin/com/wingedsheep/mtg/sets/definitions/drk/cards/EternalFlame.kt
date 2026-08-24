package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Eternal Flame
 * {2}{R}{R}
 * Sorcery
 * Eternal Flame deals X damage to target opponent or planeswalker and half X damage, rounded up,
 * to you, where X is the number of Mountains you control.
 *
 * X is not a cast-time choice here — it is a board count read at resolution, so both halves derive
 * from the same `mountains` amount rather than from an announced value. The kickback is
 * `Divide(mountains, 2, roundUp = true)`, expressing "half X" in terms of X instead of a second,
 * independently-drifting count.
 *
 * "Mountains", the land subtype: a nonbasic land with the Mountain type counts, which is why the
 * filter is a subtype test rather than a basic-land one.
 */
val EternalFlame = card("Eternal Flame") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Eternal Flame deals X damage to target opponent or planeswalker and half X " +
        "damage, rounded up, to you, where X is the number of Mountains you control."

    spell {
        val victim = target("target opponent or planeswalker", Targets.OpponentOrPlaneswalker)
        val mountains = DynamicAmounts
            .battlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN))
            .count()
        effect = Effects.Composite(
            Effects.DealDamage(mountains, victim),
            Effects.DealDamage(
                DynamicAmount.Divide(mountains, DynamicAmount.Fixed(2), roundUp = true),
                EffectTarget.PlayerRef(Player.You),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d646feea-3c20-4737-8d20-ffad42258ced.jpg?1783947935"
    }
}
