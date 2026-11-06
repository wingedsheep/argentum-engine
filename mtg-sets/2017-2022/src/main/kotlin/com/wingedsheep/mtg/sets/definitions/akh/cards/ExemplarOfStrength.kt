package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Exemplar of Strength — Amonkhet #165 (canonical printing)
 * {1}{G} · Creature — Human Warrior · 4/4
 *
 * When this creature enters, put three -1/-1 counters on target creature you control.
 * Whenever this creature attacks, remove a -1/-1 counter from it. If you do, you gain 1 life.
 *
 * The removal is mandatory; "if you do" gates the life gain on a counter actually coming off
 * (`SuccessCriterion.CountersRemoved`), so an Exemplar with no -1/-1 counters, or one that left the
 * battlefield in response (ruling 2017-04-18), gains nothing.
 */
val ExemplarOfStrength = card("Exemplar of Strength") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, put three -1/-1 counters on target creature you control.\n" +
        "Whenever this creature attacks, remove a -1/-1 counter from it. If you do, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 3, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.IfYouDo(
            action = Effects.RemoveCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self),
            then = Effects.GainLife(1),
            successCriterion = SuccessCriterion.CountersRemoved,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Sara Winters"
        flavorText = "\"No one is born worthy.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d30501c5-f6d9-47b3-b12b-8bd1c5ea6a9b.jpg?1783936476"
        ruling("2017-04-18", "If Exemplar of Strength leaves the battlefield in response to its second triggered ability, you won't be able to remove a counter from it, and you won't gain 1 life.")
    }
}
