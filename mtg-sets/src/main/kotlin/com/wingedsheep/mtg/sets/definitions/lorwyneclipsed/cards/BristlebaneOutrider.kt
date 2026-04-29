package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bristlebane Outrider
 * {3}{G}
 * Creature — Kithkin Knight
 * 3/5
 *
 * This creature can't be blocked by creatures with power 2 or less.
 * As long as another creature entered the battlefield under your control this turn,
 * this creature gets +2/+0.
 */
val BristlebaneOutrider = card("Bristlebane Outrider") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Kithkin Knight"
    power = 3
    toughness = 5
    oracleText = "This creature can't be blocked by creatures with power 2 or less.\n" +
        "As long as another creature entered the battlefield under your control this turn, this creature gets +2/+0."

    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.powerAtMost(2)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStatsEffect(
                target = StaticTarget.SourceCreature,
                powerBonus = DynamicAmount.Fixed(2),
                toughnessBonus = DynamicAmount.Fixed(0)
            ),
            condition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature.enteredThisTurn(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Ryan Pancoast"
        flavorText = "Memories of oil and blood haunt her rides. She patrols kithkin lands driven by the fear of losing another doun."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38b17a3c-4457-47e3-986e-ff0b94c41b1a.jpg?1767862561"
    }
}
