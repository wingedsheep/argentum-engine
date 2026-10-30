package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Daybreak Chimera
 * {3}{W}{W}
 * Creature — Chimera
 * 3/3
 * This spell costs {X} less to cast, where X is your devotion to white.
 * Flying
 *
 * Tazri's self-cast [ModifySpellCost] with a [CostReductionSource.Dynamic] over
 * [DynamicAmounts.devotionTo] white, read when the cost is locked in. It only removes generic mana,
 * so the Chimera never costs less than {W}{W} (ruling); the card itself, being on the stack, adds
 * nothing to the devotion.
 */
val DaybreakChimera = card("Daybreak Chimera") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Chimera"
    power = 3
    toughness = 3
    oracleText = "This spell costs {X} less to cast, where X is your devotion to white. (Each {W} in the " +
        "mana costs of permanents you control counts toward your devotion to white.)\n" +
        "Flying"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.devotionTo(Color.WHITE))
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9c1de31-73d4-4c32-9f99-e15df48638e9.jpg?1783931600"
        ruling(
            "2020-01-24",
            "The cost reduction ability reduces only the generic mana in Daybreak Chimera's cost. The " +
                "colored mana must still be paid."
        )
        ruling(
            "2020-01-24",
            "Once you announce that you're casting a spell, no player may take actions until the spell has " +
                "been paid for. Notably, opponents can't try to change by how much Daybreak Chimera's cost " +
                "is reduced."
        )
    }
}
