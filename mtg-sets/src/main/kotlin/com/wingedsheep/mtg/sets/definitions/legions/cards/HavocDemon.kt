package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect

/**
 * Havoc Demon
 * {5}{B}{B}
 * Creature — Demon
 * 5/5
 * Flying
 * When Havoc Demon dies, all creatures get -5/-5 until end of turn.
 */
val HavocDemon = card("Havoc Demon") {
    manaCost = "{5}{B}{B}"
    typeLine = "Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "Flying\nWhen Havoc Demon dies, all creatures get -5/-5 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreatures,
            effect = ModifyStatsEffect(-5, -5, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Thomas M. Baxa"
        flavorText = "A cry of pain as it enters this world. A chorus of screams as it leaves."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6477802a-349d-41e1-b050-58da0d806abf.jpg?1562915373"
        ruling("2016-06-08", "All creatures on the battlefield when Havoc Demon's triggered ability resolves are affected. Ones that enter the battlefield or become creatures later in the turn are not.")
    }
}
