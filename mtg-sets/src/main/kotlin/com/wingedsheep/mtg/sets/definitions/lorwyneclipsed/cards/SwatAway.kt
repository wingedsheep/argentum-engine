package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SpellCostReduction
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Swat Away
 * {2}{U}{U}
 * Instant
 *
 * This spell costs {2} less to cast if a creature is attacking you.
 * The owner of target spell or creature puts it on their choice of the top
 * or bottom of their library.
 */
val SwatAway = card("Swat Away") {
    manaCost = "{2}{U}{U}"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if a creature is attacking you.\n" +
        "The owner of target spell or creature puts it on their choice of the top or bottom of their library."

    spell {
        val t = target(
            "target spell or creature",
            TargetSpellOrPermanent(permanentFilter = GameObjectFilter.Creature)
        )
        effect = Effects.PutOnTopOrBottomOfLibrary(t)
    }

    staticAbility {
        ability = SpellCostReduction(
            CostReductionSource.FixedIfCreatureAttackingYou(amount = 2)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Julie Dillon"
        flavorText = "Some beings shouldn't be toyed with."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fb0ea3f-2f6d-4b64-a9d7-e822c8854a03.jpg?1767732569"
    }
}
