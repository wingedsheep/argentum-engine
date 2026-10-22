package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Moonrager's Slash — Innistrad: Midnight Hunt #148.
 * {2}{R} · Instant
 *
 * The night discount is a self-cast [ModifySpellCost] whose amount is
 * [DynamicAmounts.conditional] on [Conditions.IsNight]: {2} while the game's designation is night,
 * {0} during day *and* when the game has no day/night designation yet. It's read as the spell is
 * cast, so a later change to day doesn't matter.
 */
val MoonragersSlash = card("Moonrager's Slash") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it's night.\n" +
        "Moonrager's Slash deals 3 damage to any target."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(
                    DynamicAmounts.conditional(
                        Conditions.IsNight,
                        DynamicAmounts.fixed(2),
                        DynamicAmounts.fixed(0),
                    )
                )
            )
        )
    }

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Lars Grant-West"
        flavorText = "The cathars fell like leaves as Tovolar himself joined the fray."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb4f266b-c41c-4047-ae6f-b2226c7459e8.jpg?1783925596"
        ruling("2021-09-24", "The phrase \"if it's night\" refers to the day or night designation the game can have. It does not refer to whether or not it's dark outside as you play the game.")
    }
}
