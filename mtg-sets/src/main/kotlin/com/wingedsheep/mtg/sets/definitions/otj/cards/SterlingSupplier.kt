package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Sterling Supplier
 * {4}{W}
 * Creature — Bird Soldier
 * 3/4
 * Flying
 * When this creature enters, put a +1/+1 counter on another target creature you control.
 */
val SterlingSupplier = card("Sterling Supplier") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, put a +1/+1 counter on another target creature you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Camille Alquier"
        flavorText = "The Sterling Company can always rely on him to bring the thunder."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6000be7-67db-440e-87ba-276df20b803e.jpg?1712355360"
    }
}
