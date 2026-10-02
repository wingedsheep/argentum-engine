package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kapow!
 * {2}{G}
 * Sorcery
 * Put a +1/+1 counter on target creature you control. It fights target creature
 * an opponent controls. (Each deals damage equal to its power to the other.)
 */
val Kapow = card("Kapow!") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control. It fights target creature an opponent controls. (Each deals damage equal to its power to the other.)"

    spell {
        val yourCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        // An illegal target resolves to nothing (CR 608.2b), so the fight does nothing on its own.
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, yourCreature) then
            Effects.Fight(yourCreature, theirCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Jessica Fong"
        flavorText = "\"People are in danger—I don't have time for your games!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cec575f6-43c9-41c6-a996-bb806bf82185.jpg?1757377442"
    }
}
