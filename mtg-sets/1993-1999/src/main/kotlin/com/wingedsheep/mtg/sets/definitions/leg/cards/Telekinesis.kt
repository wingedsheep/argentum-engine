package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Telekinesis
 * {U}{U}
 * Instant
 *
 * Tap target creature. Prevent all combat damage that would be dealt by that creature this turn.
 * It doesn't untap during its controller's next two untap steps.
 */
val Telekinesis = card("Telekinesis") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap target creature. Prevent all combat damage that would be dealt by that creature this turn. " +
        "It doesn't untap during its controller's next two untap steps."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.PreventAllDamageDealtBy(creature, scope = PreventionScope.CombatOnly) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap(count = 2),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5aa920e-b93f-41c2-b505-a9350353be8b.jpg?1783948071"
    }
}
