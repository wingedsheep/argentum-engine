package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Favored Hoplite — Theros #13 (canonical printing)
 * {W}
 * Creature — Human Soldier
 * 1/2
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature
 * and prevent all damage that would be dealt to it this turn.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Skyguard); the shield is [Effects.PreventDamage]
 * on [EffectTarget.Self] with every other parameter at its default (all damage, until end of turn).
 */
val FavoredHoplite = card("Favored Hoplite") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature " +
        "and prevent all damage that would be dealt to it this turn."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.PreventDamage(target = EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature " +
            "and prevent all damage that would be dealt to it this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/251015ed-9408-4941-894a-158551ed2613.jpg?1783939818"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
