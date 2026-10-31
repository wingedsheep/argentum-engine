package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tentative Connection
 * {3}{R}
 * Sorcery
 * This spell costs {3} less to cast if you control a creature with menace.
 * Gain control of target creature until end of turn. Untap that creature.
 * It gains haste until end of turn.
 */
val TentativeConnection = card("Tentative Connection") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell costs {3} less to cast if you control a creature with menace.\n" +
        "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfControlFilter(
                    amount = 3,
                    filter = GameObjectFilter.Creature.withKeyword(Keyword.MENACE)
                )
            )
        )
    }

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Kieran Yanner"
        flavorText = "Lukka was ordered to kill a threat. He chose to make a friend."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c40fd33-bb95-48f3-a10f-a33eb27c48f1.jpg?1783931042"
    }
}
