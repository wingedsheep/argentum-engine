package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aven Surveyor
 * {3}{U}{U}
 * Creature — Bird Scout
 * 2/2
 * Flying
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • Return target creature to its owner's hand.
 *
 * The bounce mode's target is chosen only when that mode is picked, so the trigger goes on the
 * stack with the counter mode even when no creature can be targeted.
 */
val AvenSurveyor = card("Aven Surveyor") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Scout"
    oracleText = "Flying\n" +
        "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• Return target creature to its owner's hand."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            mode("Return target creature to its owner's hand") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ReturnToHand(creature)
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbb2185d-46c6-44c1-a67c-9b6af872e8eb.jpg"
    }
}
