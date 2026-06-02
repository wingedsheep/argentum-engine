package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.GroupPatterns
import com.wingedsheep.sdk.dsl.MiscPatterns

/**
 * The First Eruption
 * {2}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — The First Eruption deals 1 damage to each creature without flying.
 * II — Add {R}{R}.
 * III — Sacrifice a Mountain. If you do, The First Eruption deals 3 damage to each creature.
 */
val TheFirstEruption = card("The First Eruption") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — This Saga deals 1 damage to each creature without flying.\n" +
        "II — Add {R}{R}.\n" +
        "III — Sacrifice a Mountain. If you do, this Saga deals 3 damage to each creature."

    sagaChapter(1) {
        effect = GroupPatterns.dealDamageToAll(1, GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING))
    }

    sagaChapter(2) {
        effect = Effects.AddMana(Color.RED, 2)
    }

    sagaChapter(3) {
        effect = MiscPatterns.reflexiveTrigger(
            action = SacrificeEffect(Filters.MountainCard),
            optional = false,
            whenYouDo = GroupPatterns.dealDamageToAll(3, GroupFilter.AllCreatures)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0efc241f-64b5-4e28-b14a-b3f19ca6e7b5.jpg?1562731396"
        ruling("2018-04-27", "While resolving The First Eruption's final chapter ability, you must sacrifice one Mountain if able. You can't sacrifice multiple Mountains to deal more damage.")
    }
}
