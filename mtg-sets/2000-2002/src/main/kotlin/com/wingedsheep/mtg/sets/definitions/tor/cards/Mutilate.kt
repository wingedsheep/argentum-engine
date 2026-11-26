package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mutilate — Torment #73
 * {2}{B}{B} · Sorcery
 *
 * All creatures get -1/-1 until end of turn for each Swamp you control.
 *
 * Nausea's sweep with Defile's amount: a resolution-time [Effects.ModifyStats] over the count of
 * Swamps you control (projected subtype, so nonbasic Swamps count), applied to each creature on
 * the battlefield as Mutilate resolves. Later changes to your Swamp count don't alter it.
 */
val Mutilate = card("Mutilate") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -1/-1 until end of turn for each Swamp you control."

    spell {
        val negSwamps = -DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Land.withSubtype(Subtype.SWAMP)
        ).count()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.ModifyStats(negSwamps, negSwamps, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Eric Peterson"
        flavorText = "\"They appreciate my handiwork. I hear their joy in every scream.\"\n—Chainer, dementia master"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6189cab3-1963-4590-9cbc-7ab4a693d7c6.jpg?1783945154"
        ruling(
            "2012-07-01",
            "The effect is based on the number of Swamps you control when Mutilate resolves. Only creatures on " +
                "the battlefield at that time will be affected. The effect won't change later in the turn, even " +
                "if the number of Swamps you control changes."
        )
    }
}
