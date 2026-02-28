package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Gempalm Incinerator
 * {2}{R}
 * Creature — Goblin
 * 2/1
 * Cycling {1}{R}
 * When you cycle Gempalm Incinerator, you may have it deal X damage to target creature,
 * where X is the number of Goblins on the battlefield.
 */
val GempalmIncinerator = card("Gempalm Incinerator") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin"
    oracleText = "Cycling {1}{R}\nWhen you cycle Gempalm Incinerator, you may have it deal X damage to target creature, where X is the number of Goblins on the battlefield."
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.cycling("{1}{R}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val t = target("target", Targets.Creature)
        effect = MayEffect(
            DealDamageEffect(
                DynamicAmounts.creaturesWithSubtype(Subtype("Goblin")),
                t
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Luca Zontini"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2687c311-fd0c-4fe0-bce8-e3f412216796.jpg?1562902848"
    }
}
