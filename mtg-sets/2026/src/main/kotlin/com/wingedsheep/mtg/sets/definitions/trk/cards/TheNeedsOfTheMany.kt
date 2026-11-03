package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Needs of the Many
 * {1}{W}
 * Instant
 * As an additional cost to cast this spell, sacrifice a creature.
 * Creatures you control gain hexproof and indestructible until end of turn. You gain hexproof until
 * end of turn. Prevent all damage that would be dealt to you this turn. Exile The Needs of the Many.
 *
 * - The sacrifice is Culling the Weak's additional cost.
 * - The creature grant is Heroic Intervention's shape: the affected set is locked in as the spell
 *   resolves, so creatures you begin to control later in the turn get nothing.
 * - "You gain hexproof" is the player form of [Effects.GrantHexproof]; the damage shield is the
 *   default [Effects.PreventDamage] (all damage to you, this turn).
 * - "Exile The Needs of the Many" replaces the graveyard as its destination on resolution.
 */
val TheNeedsOfTheMany = card("The Needs of the Many") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Creatures you control gain hexproof and indestructible until end of turn. You gain hexproof " +
        "until end of turn. Prevent all damage that would be dealt to you this turn. " +
        "Exile The Needs of the Many."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
        ) then
            Effects.GrantHexproof(EffectTarget.Controller) then
            Effects.PreventDamage(EffectTarget.Controller)
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Carlos C Diaz"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c779279f-681b-4107-b62c-5202c544d3e8.jpg?1793187981"
    }
}
