package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phyresis Outbreak
 * {2}{B}
 * Sorcery
 * Each opponent gets a poison counter. Then each creature your opponents control gets -1/-1 until
 * end of turn for each poison counter its controller has.
 *
 * The poison half is Prologue to Phyresis's `ForEachPlayer(EachOpponent)` grant. The shrink is a
 * per-creature `ForEachInGroup` whose amount reads the **iterated creature's controller's** poison
 * total (`Player.ControllerOfIterationEntity`), counted after the new counters land. Each
 * `ModifyStats` resolves its amount once, so the reduction is locked in at resolution (ruling) and
 * doesn't move if that player later gets more poison counters or the creature changes control.
 */
val PhyresisOutbreak = card("Phyresis Outbreak") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each opponent gets a poison counter. Then each creature your opponents control gets -1/-1 " +
        "until end of turn for each poison counter its controller has."

    spell {
        val controllersPoison = DynamicAmounts.playerCounterCount(CounterType.POISON, Player.ControllerOfIterationEntity)
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.You)),
        ) then Patterns.Group.modifyStatsForAll(
            -controllersPoison,
            -controllersPoison,
            GroupFilter.AllCreaturesOpponentsControl,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Matthew G. Lewis"
        flavorText = "\"It is amusing that the fleshlings believe we are at war. In truth, they are nothing but " +
            "scurrying pests to be eradicated.\"\n—Azax-Azog, the Demon Thane"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/812498c8-974c-460c-b782-506f5f735346.jpg?1783918160"
        ruling(
            "2023-02-04",
            "The amount a creature's power and toughness are reduced is determined only once, at the time Phyresis " +
                "Outbreak resolves. It won't change if that creature's controller later gets more poison counters or " +
                "if another player takes control of it later in the same turn."
        )
    }
}
