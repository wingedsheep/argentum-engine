package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifySpecialActionCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.SpecialActionKind

/**
 * Inquisitive Glimmer
 * {W}{U}
 * Enchantment Creature — Fox Glimmer
 * 2/3
 *
 * Enchantment spells you cast cost {1} less to cast.
 * Unlock costs you pay cost {1} less.
 *
 * The enchantment-spell discount is a standard [ModifySpellCost]; the unlock discount is the
 * dedicated [ModifySpecialActionCost] (CR 709.5e), routed through the same `SpecialActionCostReducer` the
 * unlock special action pays through.
 */
val InquisitiveGlimmer = card("Inquisitive Glimmer") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment Creature — Fox Glimmer"
    power = 2
    toughness = 3
    oracleText = "Enchantment spells you cast cost {1} less to cast.\n" +
        "Unlock costs you pay cost {1} less."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Enchantment),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    staticAbility {
        ability = ModifySpecialActionCost(
            action = SpecialActionKind.UNLOCK_DOOR,
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Julie Dillon"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1f66e3e-9f1f-4601-aa30-30b66805a5a8.jpg?1726286675"
    }
}
