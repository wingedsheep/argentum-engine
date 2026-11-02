package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceDestructionWith
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dr. McCoy, Surly Surgeon
 * {2}{W}{W}
 * Legendary Creature — Human Doctor
 * 2/2
 *
 * When Dr. McCoy enters, put a +1/+1 counter on another target creature you control.
 * If another creature you control with a +1/+1 counter on it would be destroyed, instead remove all
 * +1/+1 counters from it and all damage already dealt to it is healed.
 *
 * The replacement covers destroy effects and the lethal-damage / deathtouch state-based actions alike;
 * the creature isn't tapped or removed from combat (it isn't regeneration).
 */
val DrMcCoySurlySurgeon = card("Dr. McCoy, Surly Surgeon") {
    manaCost = "{2}{W}{W}"
    typeLine = "Legendary Creature — Human Doctor"
    colorIdentity = "W"
    oracleText = "When Dr. McCoy enters, put a +1/+1 counter on another target creature you control.\n" +
        "If another creature you control with a +1/+1 counter on it would be destroyed, instead remove " +
        "all +1/+1 counters from it and all damage already dealt to it is healed."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    replacementEffect(
        ReplaceDestructionWith(
            replacementEffect = Effects.RemoveAllCountersOfType(
                CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.TriggeringEntity
            ) then Effects.HealDamage(EffectTarget.TriggeringEntity),
            appliesTo = EventPattern.DestroyEvent(
                GameObjectFilter.Creature.youControl()
                    .withCounter(CounterType.PLUS_ONE_PLUS_ONE)
                    .notSourceItself()
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "David Robert Hovey"
        flavorText = "\"What is this, the Dark Ages?!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b300d814-cf74-44ba-b253-ec179ec54a20.jpg?1792668584"
    }
}
