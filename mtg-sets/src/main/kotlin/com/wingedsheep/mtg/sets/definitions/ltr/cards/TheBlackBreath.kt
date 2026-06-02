package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Black Breath
 * {2}{B}
 * Sorcery
 *
 * Creatures your opponents control get -1/-1 until end of turn. The Ring tempts you.
 */
val TheBlackBreath = card("The Black Breath") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Creatures your opponents control get -1/-1 until end of turn. The Ring tempts you."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesOpponentsControl,
            effect = ModifyStatsEffect(
                powerModifier = -1,
                toughnessModifier = -1,
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            )
        ).then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Chris Cold"
        flavorText = "\"When the black breath blows, death's shadow grows.\"\n—A rhyme of old days"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e63983d-c36b-4440-b9e1-baaa6c7c0ba9.jpg?1686968386"
    }
}
