package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plague Nurse
 * {3}{G}
 * Creature — Phyrexian Cleric
 * 3/4
 * Toxic 2
 * {2}{G}: Each other creature you control with toxic gains toxic 1 until end of turn. Activate
 * only once each turn.
 *
 * The group is fixed on resolution (CR 611.2c) by `ForEachInGroup`; each member gets the granted
 * `TOXIC_1` keyword ([Effects.GrantToxic]), which sums with its existing toxic (CR 702.164b).
 */
val PlagueNurse = card("Plague Nurse") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Cleric"
    power = 3
    toughness = 4
    oracleText = "Toxic 2\n" +
        "{2}{G}: Each other creature you control with toxic gains toxic 1 until end of turn. " +
        "Activate only once each turn. (A player dealt combat damage by a creature with toxic also " +
        "gets poison counters equal to that creature's total toxic value.)"

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 2))

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.TOXIC)).other(),
            Effects.GrantToxic(1, EffectTarget.IterationEntity)
        )
        description = "Each other creature you control with toxic gains toxic 1 until end of turn. " +
            "Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Marcela Bolívar"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e7eef9e-68ae-4743-ad89-64f6fafbe365.jpg?1783918012"
        ruling(
            "2023-02-04",
            "Multiple instances of toxic are cumulative. For example, if a creature has toxic 2 and gains toxic 1 due to another effect, combat damage that creature deals to a player will cause that player to get 3 poison counters."
        )
    }
}
