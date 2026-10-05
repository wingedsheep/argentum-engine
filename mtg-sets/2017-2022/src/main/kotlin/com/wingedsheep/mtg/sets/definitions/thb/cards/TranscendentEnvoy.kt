package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Transcendent Envoy
 * {1}{W}
 * Enchantment Creature — Griffin
 * 1/2
 * Flying
 * Aura spells you cast cost {1} less to cast.
 *
 * The discount is the same `ModifySpellCost` static the Medallions use, keyed on the Aura
 * subtype. `ReduceGeneric` only shaves generic mana, so an Aura's coloured pips are untouched.
 */
val TranscendentEnvoy = card("Transcendent Envoy") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Griffin"
    power = 1
    toughness = 2
    oracleText = "Flying\nAura spells you cast cost {1} less to cast."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype("Aura")),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Zezhou Chen"
        flavorText = "\"The first griffins were made by the gods to capture falling stars; they were given " +
            "the keenest eyes and the swiftest wings, and sent to keep watch above the clouds.\"\n—*The Cosmogony*"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9241289-28d2-4827-970e-81bdecbb5c16.jpg?1783931589"
    }
}
