package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Centaur Battlemaster — Theros #154 (canonical printing)
 * {3}{G}{G}
 * Creature — Centaur Warrior
 * 3/3
 * Heroic — Whenever you cast a spell that targets this creature, put three +1/+1 counters on this creature.
 *
 * Heroic is the `TargetsSource` cast trigger (Fabled Hero).
 */
val CentaurBattlemaster = card("Centaur Battlemaster") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 3
    toughness = 3
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put three +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put three +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Kev Walker"
        flavorText = "\"A herd is only as strong as its weakest. Our weakest just killed his third minotaur raider—today.\"\n—Braulios of Pheres Band"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2975a0b4-7436-4588-8f79-c001a291c459.jpg?1783939748"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
