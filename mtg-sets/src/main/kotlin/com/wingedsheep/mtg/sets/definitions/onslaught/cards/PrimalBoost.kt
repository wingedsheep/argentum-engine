package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Primal Boost
 * {2}{G}
 * Instant
 * Target creature gets +4/+4 until end of turn.
 * Cycling {2}{G}
 * When you cycle Primal Boost, you may have target creature get +1/+1 until end of turn.
 */
val PrimalBoost = card("Primal Boost") {
    manaCost = "{2}{G}"
    typeLine = "Instant"
    oracleText = "Target creature gets +4/+4 until end of turn.\nCycling {2}{G}\nWhen you cycle Primal Boost, you may have target creature get +1/+1 until end of turn."

    spell {
        val t = target("target", Targets.Creature)
        effect = ModifyStatsEffect(
            powerModifier = 4,
            toughnessModifier = 4,
            target = t
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}{G}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val t = target("target", Targets.Creature)
        effect = MayEffect(
            ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 1,
                target = t
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "277"
        artist = "Eric Peterson"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1b91a5a-9328-4fc6-a2f6-a7879281e145.jpg?1562952412"
    }
}
