package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.triggers.OnCycle

/**
 * Solar Blast
 * {3}{R}
 * Instant
 * Solar Blast deals 3 damage to any target.
 * Cycling {1}{R}{R}
 * When you cycle Solar Blast, you may have it deal 1 damage to any target.
 */
val SolarBlast = card("Solar Blast") {
    manaCost = "{3}{R}"
    typeLine = "Instant"
    oracleText = "Solar Blast deals 3 damage to any target.\nCycling {1}{R}{R}\nWhen you cycle Solar Blast, you may have it deal 1 damage to any target."

    spell {
        target = Targets.Any
        effect = DealDamageEffect(3, EffectTarget.ContextTarget(0))
    }

    keywordAbility(KeywordAbility.cycling("{1}{R}{R}"))

    triggeredAbility {
        trigger = OnCycle(controllerOnly = true)
        target = Targets.Any
        effect = MayEffect(
            DealDamageEffect(1, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "234"
        artist = "Greg Staples"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b36fc40c-6a68-4192-91d9-2031c7d32e05.jpg?1562937333"
    }
}
