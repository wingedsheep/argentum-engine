package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Kapow!
 * {2}{G}
 * Instant
 * Put a +1/+1 counter on target creature you control. Then it fights target creature
 * an opponent controls.
 */
val Kapow = card("Kapow!") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control. Then it fights target creature an opponent controls."

    spell {
        val yourCreature = target("creature you control", Targets.CreatureYouControl)
        val theirCreature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        // Counter is only placed when the fight target is still legal on resolution;
        // if theirCreature was stripped (e.g. gained hexproof in response), index 1 is
        // absent from the validated-target list and the condition returns false.
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.opponentControls(), targetIndex = 1
            ),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, yourCreature)
        ).then(Effects.Fight(yourCreature, theirCreature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Jessica Fong"
        flavorText = "\"People are in danger—I don't have time for your games!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cec575f6-43c9-41c6-a996-bb806bf82185.jpg?1757377442"
    }
}
