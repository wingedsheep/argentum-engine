package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Satyr Hoplite — Journey into Nyx #110 (canonical printing)
 * {R}
 * Creature — Satyr Soldier
 * 1/1
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Skyguard, Hero of the Nyxborn).
 */
val SatyrHoplite = card("Satyr Hoplite") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Satyr Soldier"
    power = 1
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Mark Zug"
        flavorText = "\"Xenagos has become what he once despised: a tyrant and an oppressor.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8754d66-facb-432e-a6c2-91430a6dec94.jpg?1783939420"
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
