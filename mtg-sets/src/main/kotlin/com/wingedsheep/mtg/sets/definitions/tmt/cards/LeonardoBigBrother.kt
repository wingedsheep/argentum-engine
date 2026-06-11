package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Leonardo, Big Brother
 * {2}{W}
 * Legendary Creature — Mutant Ninja Turtle
 * 1/3
 *
 * Sneak {W} (You may cast this spell for {W} if you also return an unblocked
 * attacker you control to hand during the declare blockers step. He enters
 * tapped and attacking.)
 * Leonardo gets +1/+0 for each other creature you control.
 */
val LeonardoBigBrother = card("Leonardo, Big Brother") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Sneak {W} (You may cast this spell for {W} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nLeonardo gets +1/+0 for each other creature you control."
    power = 1
    toughness = 3

    sneak("{W}")

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature,
                excludeSelf = true
            ),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "InHyuk Lee"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e802838f-cc8c-4313-8c3b-32a6a7248e64.jpg?1771502512"
    }
}
