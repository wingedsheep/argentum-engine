package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Greenbelt Radical — Murders at Karlov Manor #163
 * {3}{G} · Creature — Centaur Citizen · 4/4
 *
 * Disguise {5}{G}{G}
 * When this creature is turned face up, put a +1/+1 counter on each creature you control.
 * Creatures you control gain trample until end of turn.
 *
 * Hard-cast for {3}{G} it's a vanilla 4/4; the whole card is the disguise line. Cast face down for
 * {3}, flip for {5}{G}{G} mid-combat, and the team grows permanently *and* gets trample for the
 * swing — turning face up is a special action (CR 702.168c) that uses no stack, so the opponent's
 * only response window is after the trigger is already on the stack.
 *
 * Both halves iterate the same group in one [Effects.ForEachInGroup] pass. That is not a shortcut:
 * "creatures you control" is evaluated once, on resolution, for the whole ability — a creature that
 * arrives later gets neither the counter nor the trample. The Radical itself *is* in the group,
 * because it is on the battlefield (face up) before the trigger resolves.
 *
 * The counter is permanent and the trample is [com.wingedsheep.sdk.core.Duration.EndOfTurn] (the
 * `Effects.GrantKeyword` default), so a bounce or a second flip later in the turn leaves the
 * counters behind and only the trample wears off in cleanup.
 */
val GreenbeltRadical = card("Greenbelt Radical") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Citizen"
    power = 4
    toughness = 4
    oracleText = "Disguise {5}{G}{G} (You may cast this card face down for {3} as a 2/2 creature " +
        "with ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, put a +1/+1 counter on each creature you control. " +
        "Creatures you control gain trample until end of turn."

    disguise = "{5}{G}{G}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Composite(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            ),
        )
        description = "When this creature is turned face up, put a +1/+1 counter on each creature " +
            "you control. Creatures you control gain trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Andreia Ugrai"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88e62346-cc62-4938-970c-b56beeb79fa6.jpg?1783912865"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
