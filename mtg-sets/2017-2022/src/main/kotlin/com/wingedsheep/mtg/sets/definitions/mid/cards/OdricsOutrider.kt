package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Odric's Outrider
 * {3}{W}
 * Creature — Human Knight
 * 2/4
 * Whenever this creature or another creature you control dies, put a +1/+1 counter on target
 * creature you control.
 *
 * "This creature or another creature you control" is the ANY binding ([Triggers.a]).
 */
val OdricsOutrider = card("Odric's Outrider") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "Whenever this creature or another creature you control dies, put a +1/+1 counter on target creature you control."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Cristi Balanescu"
        flavorText = "Terms like \"safe route\" and \"cleared district\" are a source of dark humor to the cathars who patrol the ruins of Thraben."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9016f837-cc19-43f1-adb9-96bdd3195407.jpg"
    }
}
