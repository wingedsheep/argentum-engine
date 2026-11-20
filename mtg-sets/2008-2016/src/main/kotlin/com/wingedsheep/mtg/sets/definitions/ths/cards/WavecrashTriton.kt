package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wavecrash Triton — Theros #74 (canonical printing)
 * {2}{U}
 * Creature — Merfolk Wizard
 * 1/4
 * Heroic — Whenever you cast a spell that targets this creature, tap target creature an opponent
 * controls. That creature doesn't untap during its controller's next untap step.
 *
 * Heroic is the `TargetsSource` cast trigger (Fabled Hero); the tap + untap lock is Niblis of
 * Frost's shape, keyed to the tapped creature's controller's next untap step.
 */
val WavecrashTriton = card("Wavecrash Triton") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 4
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, tap target creature an " +
        "opponent controls. That creature doesn't untap during its controller's next untap step."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap(),
            )
        description = "Heroic — Whenever you cast a spell that targets this creature, tap target creature " +
            "an opponent controls. That creature doesn't untap during its controller's next untap step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7336ca1e-13ef-4e49-a526-3f285bc339bb.jpg?1783939786"
        ruling("2013-09-15", "Wavecrash Triton's ability can target a creature that's already tapped. That creature still won't untap during its controller's next untap step.")
        ruling("2013-09-15", "Wavecrash Triton's ability tracks the creature, but not its controller. If the creature changes controllers before its first controller's next untap step has come around, then it won't untap during its new controller's next untap step.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
