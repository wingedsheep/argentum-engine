package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pheres-Band Thunderhoof — Journey into Nyx #134 (canonical printing)
 * {4}{G}
 * Creature — Centaur Warrior
 * 3/4
 * Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature.
 */
val PheresBandThunderhoof = card("Pheres-Band Thunderhoof") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 3
    toughness = 4
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Christopher Moeller"
        flavorText = "\"Flesh and bone or star and shadow. It makes no difference to my club.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a19a2e8e-9d8f-4537-838c-ac60e09d78bf.jpg?1783939410"
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
