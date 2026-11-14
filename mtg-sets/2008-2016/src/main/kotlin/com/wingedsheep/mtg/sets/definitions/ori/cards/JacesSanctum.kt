package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Jace's Sanctum
 * {3}{U}
 * Enchantment
 * Instant and sorcery spells you cast cost {1} less to cast.
 * Whenever you cast an instant or sorcery spell, scry 1.
 *
 * Goblin Electromancer's discount plus Prescient Chimera's cast trigger.
 */
val JacesSanctum = card("Jace's Sanctum") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Instant and sorcery spells you cast cost {1} less to cast.\n" +
        "Whenever you cast an instant or sorcery spell, scry 1."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c3fe038-a982-4cbf-a158-c45fdc3d727e.jpg?1783938350"
        ruling(
            "2015-06-22",
            "The first ability of Jace's Sanctum can't reduce the colored mana requirement of an " +
                "instant or sorcery spell.",
        )
        ruling(
            "2015-06-22",
            "If there are additional costs to cast an instant or sorcery spell, apply those before " +
                "applying cost reductions.",
        )
        ruling(
            "2015-06-22",
            "Jace's Sanctum can reduce alternative costs such as miracle or overload costs.",
        )
        ruling(
            "2015-06-22",
            "Jace's Sanctum's scry ability will resolve before the instant or sorcery spell that " +
                "caused it to trigger.",
        )
    }
}
