package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * History of Benalia
 * {1}{W}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Create a 2/2 white Knight creature token with vigilance.
 * III — Knights you control get +2/+1 until end of turn.
 */
val HistoryOfBenalia = card("History of Benalia") {
    manaCost = "{1}{W}{W}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Create a 2/2 white Knight creature token with vigilance.\n" +
        "III — Knights you control get +2/+1 until end of turn."

    sagaChapter(1) {
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE)
        )
    }

    sagaChapter(2) {
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE)
        )
    }

    sagaChapter(3) {
        effect = ForEachInGroupEffect(
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Knight").youControl()),
            effect = ModifyStatsEffect(
                DynamicAmount.Fixed(2),
                DynamicAmount.Fixed(1),
                EffectTarget.Self
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "21"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d134385d-b01c-41c7-bb2d-30722b44dc5a.jpg?1562743350"
    }
}
