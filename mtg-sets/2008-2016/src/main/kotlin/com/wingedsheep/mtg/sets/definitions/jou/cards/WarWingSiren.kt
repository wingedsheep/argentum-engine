package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * War-Wing Siren — Journey into Nyx #57 (canonical printing)
 * {2}{U}
 * Creature — Siren Soldier
 * 1/3
 * Flying
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature.
 *
 * Heroic is the `TargetsSource` cast trigger (Satyr Hoplite, Akroan Skyguard).
 */
val WarWingSiren = card("War-Wing Siren") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren Soldier"
    power = 1
    toughness = 3
    oracleText = "Flying\nHeroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Magali Villeneuve"
        flavorText = "Once she sang sailors to their doom. Now she leads them to glory."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d88c942-3a69-4975-a08d-80405a549beb.jpg?1783939441"
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
