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
        val yourCreature = target("creature you control", Targets.CreatureYouControl)
        val theirCreature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        // Per CR 608.2b and the Savage Stomp ruling: if only the fight target is illegal on
        // resolution, the +1/+1 counter still goes on the creature you control. AddCounters
        // handles partial-resolution on target #0; the fight requires both targets legal.
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, yourCreature)
            .then(
                ConditionalEffect(
                    condition = Conditions.All(
                        Conditions.TargetMatchesFilter(
                            GameObjectFilter.Creature.youControl(), targetIndex = 0
                        ),
                        Conditions.TargetMatchesFilter(
                            GameObjectFilter.Creature.opponentControls(), targetIndex = 1
                        )
                    ),
                    effect = Effects.Fight(yourCreature, theirCreature)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Jessica Fong"
        flavorText = "\"People are in danger—I don't have time for your games!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cec575f6-43c9-41c6-a996-bb806bf82185.jpg?1757377442"
    }
}
