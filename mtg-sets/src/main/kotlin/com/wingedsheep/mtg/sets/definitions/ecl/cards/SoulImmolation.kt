package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Costs

/**
 * Soul Immolation
 * {3}{R}{R}
 * Sorcery
 * As an additional cost to cast this spell, blight X. X can't be greater than the
 * greatest toughness among creatures you control. (Put X -1/-1 counters on a
 * creature you control.)
 * Soul Immolation deals X damage to each opponent and each creature they control.
 */
val SoulImmolation = card("Soul Immolation") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, blight X. X can't be greater than the greatest toughness among creatures you control. (Put X -1/-1 counters on a creature you control.)\n" +
        "Soul Immolation deals X damage to each opponent and each creature they control."

    additionalCost(Costs.additional.BlightVariable())

    spell {
        effect = Effects.DealDamage(
            DynamicAmount.ContextProperty(ContextPropertyKey.ADDITIONAL_COST_BLIGHT_AMOUNT),
            EffectTarget.PlayerRef(Player.EachOpponent)
        ) then Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesOpponentsControl,
            effect = DealDamageEffect(
                DynamicAmount.ContextProperty(ContextPropertyKey.ADDITIONAL_COST_BLIGHT_AMOUNT),
                EffectTarget.Self
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "156"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/234b70df-8c34-4da7-946e-b8b55a8df390.jpg?1767952116"
    }
}
