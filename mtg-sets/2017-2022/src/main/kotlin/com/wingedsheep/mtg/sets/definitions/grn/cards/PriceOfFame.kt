package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Price of Fame (GRN 83)
 * {3}{B} — Instant
 *
 * This spell costs {2} less to cast if it targets a legendary creature.
 * Destroy target creature.
 * Surveil 2.
 *
 * The reduction is Dire Downdraft's target-conditional `FixedIfAnyTargetMatches`; the mana value
 * stays 4 either way. Surveil happens even if the target is indestructible.
 */
val PriceOfFame = card("Price of Fame") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it targets a legendary creature.\n" +
        "Destroy target creature.\n" +
        "Surveil 2. (Look at the top two cards of your library, then put any number of them into " +
        "your graveyard and the rest on top of your library in any order.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.Surveil(2)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Creature.legendary(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61b52152-0f7c-4466-9e49-033477028f67.jpg?1783934170"
        ruling("2018-10-05", "Price of Fame's mana value is 4, even if it costs {2} less to cast.")
        ruling("2018-10-05", "You'll surveil before resolving any abilities that trigger on the target creature dying.")
        ruling("2018-10-05", "Price of Fame can target a creature that's indestructible. It won't be destroyed, but you'll surveil.")
    }
}
