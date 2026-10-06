package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
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
 * Hooded Assassin
 * {2}{B}
 * Creature — Human Assassin
 * 1/2
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on this creature.
 * • Destroy target creature that was dealt damage this turn.
 */
val HoodedAssassin = card("Hooded Assassin") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on this creature.\n" +
        "• Destroy target creature that was dealt damage this turn."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                "Put a +1/+1 counter on this creature",
            ),
            mode("Destroy target creature that was dealt damage this turn") {
                val creature = target(TargetFilter.Creature.wasDealtDamageThisTurn())
                effect = Effects.Destroy(creature)
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05d4da1d-5b8c-43be-aa30-22df69c0cc23.jpg?1783938696"
        ruling(
            "2014-11-24",
            "If a creature was dealt damage but regenerated (which removes all damage from it), it will " +
                "still be a legal target for the second mode of the triggered ability."
        )
        ruling(
            "2014-11-24",
            "You choose which mode you're using as you put the ability on the stack, after the creature " +
                "has entered the battlefield. Once you've chosen a mode, you can't change that mode even if " +
                "the creature leaves the battlefield in response to that ability."
        )
        ruling(
            "2014-11-24",
            "If a mode requires a target and there are no legal targets available, that mode can't be " +
                "chosen. You must choose another mode if able."
        )
    }
}
