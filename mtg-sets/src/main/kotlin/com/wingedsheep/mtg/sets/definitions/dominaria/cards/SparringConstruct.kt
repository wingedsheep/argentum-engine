package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sparring Construct
 * {1}
 * Artifact Creature — Construct
 * 1/1
 * When this creature dies, put a +1/+1 counter on target creature you control.
 */
val SparringConstruct = card("Sparring Construct") {
    manaCost = "{1}"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, put a +1/+1 counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.Dies
        target = Targets.CreatureYouControl
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "232"
        artist = "Mark Behm"
        flavorText = "The trainers were a gift of gratitude from the wizards of Tolaria West to the knights of New Benalia for their aid during the Talas Incursion."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/badc7db8-386e-4fb6-aefa-591e99747eb2.jpg?1562741935"
    }
}
