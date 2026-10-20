package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Subdue
 * {G}
 * Instant
 *
 * Prevent all combat damage that would be dealt by target creature this turn. That creature gets
 * +0/+X until end of turn, where X is its mana value.
 *
 * X reads the *target's* mana value at resolution, so it is `manaValueOf(creature)` rather than
 * anything sourced off the spell.
 */
val Subdue = card("Subdue") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt by target creature this turn. " +
        "That creature gets +0/+X until end of turn, where X is its mana value."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.PreventAllDamageDealtBy(creature, scope = PreventionScope.CombatOnly) then
            Effects.ModifyStats(DynamicAmounts.fixed(0), DynamicAmounts.manaValueOf(creature), creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/123d6097-8021-46cd-a8c3-01013245e347.jpg?1783948043"
    }
}
