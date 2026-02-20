package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect

/**
 * Death's-Head Buzzard
 * {1}{B}{B}
 * Creature — Bird
 * 2/1
 * Flying
 * When Death's-Head Buzzard dies, all creatures get -1/-1 until end of turn.
 */
val DeathsHeadBuzzard = card("Death's-Head Buzzard") {
    manaCost = "{1}{B}{B}"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen Death's-Head Buzzard dies, all creatures get -1/-1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreatures,
            effect = ModifyStatsEffect(-1, -1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Marcelo Vignali"
        flavorText = "Infested with vermin, ever hungering, dropping from night's sky."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a8d4fd9-1f9e-41f0-9114-d1a698506ad9.jpg?1562531685"
    }
}
