package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Staunch-Hearted Warrior — Theros #179 (canonical printing)
 * {3}{G}
 * Creature — Human Warrior
 * 2/2
 * Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature.
 */
val StaunchHeartedWarrior = card("Staunch-Hearted Warrior") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Greg Staples"
        flavorText = "As soon as she faces a monster, she begins composing its epitaph."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3485eb83-f83c-4776-b164-abd72f3c9547.jpg?1783939737"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
