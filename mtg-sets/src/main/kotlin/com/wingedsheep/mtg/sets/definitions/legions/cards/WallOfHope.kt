package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wall of Hope
 * {W}
 * Creature — Wall
 * 0/3
 * Defender (This creature can't attack.)
 * Whenever this creature is dealt damage, you gain that much life.
 */
val WallOfHope = card("Wall of Hope") {
    manaCost = "{W}"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 3
    oracleText = "Defender (This creature can't attack.)\nWhenever this creature is dealt damage, you gain that much life."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.TakesDamage
        effect = GainLifeEffect(
            amount = DynamicAmount.TriggerDamageAmount,
            target = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "David Martin"
        flavorText = "\"What cage would you rather be inside of than out?\" —Daru riddle"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b463b3e1-e314-4a65-a89e-0712f630b016.jpg?1562931313"
    }
}
