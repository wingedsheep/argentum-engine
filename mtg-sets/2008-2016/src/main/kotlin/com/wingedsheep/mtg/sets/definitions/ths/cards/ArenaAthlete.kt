package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arena Athlete — Theros #113 (canonical printing)
 * {1}{R}
 * Creature — Human
 * 2/1
 * Heroic — Whenever you cast a spell that targets this creature, target creature an opponent
 * controls can't block this turn.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Crusader, Elite Skirmisher); the effect is the
 * targeted end-of-turn can't-block of Fervent Cathar.
 */
val ArenaAthlete = card("Arena Athlete") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human"
    power = 2
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, target creature an opponent " +
        "controls can't block this turn."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantBlock(creature)
        description = "Heroic — Whenever you cast a spell that targets this creature, target creature an " +
            "opponent controls can't block this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Jason Chan"
        flavorText = "The Iroan Games award no medals. Athletes vie for a visit from Iroas, god of victory."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e201d9e6-d611-4dc4-b665-9062b449f859.jpg?1783939768"
        ruling("2018-12-07", "Resolving Arena Athlete's ability after a creature has blocked won't remove the blocking creature from combat or cause the creature it blocked to become unblocked.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
