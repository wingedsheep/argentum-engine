package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Frost Lynx — Magic 2015 #55
 * {2}{U} · Creature — Elemental Cat · 2/2
 *
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's next untap step.
 *
 * The freeze is a `DOESNT_UNTAP` grant bounded to exactly one untap step of the creature's
 * controller ([Duration.UntilAfterAffectedControllersNextUntap]).
 */
val FrostLynx = card("Frost Lynx") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Cat"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, tap target creature an opponent controls. " +
        "That creature doesn't untap during its controller's next untap step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Izzy"
        flavorText = "It readily attacks much larger prey, knowing retaliation is impossible."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/619268d2-f7a8-48cd-b17b-197e82474b13.jpg"
    }
}
