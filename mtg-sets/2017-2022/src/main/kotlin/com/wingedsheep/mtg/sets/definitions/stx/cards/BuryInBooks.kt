package com.wingedsheep.mtg.sets.definitions.stx.cards

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
 * Bury in Books
 * {4}{U}
 * Instant
 * This spell costs {2} less to cast if it targets an attacking creature.
 * Put target creature into its owner's library second from the top.
 *
 * The discount is [SpellCostTarget.SelfCast] over [CostReductionSource.FixedIfAnyTargetMatches]
 * keyed on an attacking-creature filter, so the {2} comes off only the generic part and only when
 * the announced target is attacking (CR 601.2f).
 */
val BuryInBooks = card("Bury in Books") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it targets an attacking creature.\nPut target creature into its owner's library second from the top."

    spell {
        val creature = target(TargetFilter.Creature)
        // 0-indexed: position 1 = second from the top.
        effect = Effects.PutIntoLibraryNthFromTop(creature, 1)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Creature.attacking(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Zoltan Boros"
        flavorText = "There are no weapons allowed in the Biblioplex, but a clever mage is never truly defenseless."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac2a2cf5-80cf-4c06-8b04-bc04a5460de5.jpg?1783927380"
    }
}
