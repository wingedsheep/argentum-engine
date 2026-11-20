package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fabled Hero — Theros #12 (canonical printing)
 * {1}{W}{W}
 * Creature — Human Soldier
 * 2/2
 * Double strike
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it.
 *
 * Heroic is the `TargetsSource` cast trigger (Favored Hoplite).
 */
val FabledHero = card("Fabled Hero") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Double strike\n" +
        "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Aaron Miller"
        flavorText = "\"You. Poet. Be sure to write this down.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/642d9a5c-5697-4747-adce-5a71bcf1c086.jpg?1783939820"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
