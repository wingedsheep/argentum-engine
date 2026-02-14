package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.OnOtherCreatureEnters

/**
 * Death Match
 * {3}{B}
 * Enchantment
 * Whenever a creature enters, that creature's controller may have target creature
 * of their choice get -3/-3 until end of turn.
 */
val DeathMatch = card("Death Match") {
    manaCost = "{3}{B}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature enters, that creature's controller may have target creature of their choice get -3/-3 until end of turn."

    triggeredAbility {
        trigger = OnOtherCreatureEnters(youControlOnly = false)
        controlledByTriggeringEntityController = true
        target = Targets.Creature
        effect = MayEffect(
            ModifyStatsEffect(
                powerModifier = -3,
                toughnessModifier = -3,
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Mark Brill"
        flavorText = "When one enters the arena, another feels the pain."
        imageUri = "https://cards.scryfall.io/large/front/1/4/143e9057-267a-4c78-b72a-4f8018b627a8.jpg?1562899865"
    }
}
