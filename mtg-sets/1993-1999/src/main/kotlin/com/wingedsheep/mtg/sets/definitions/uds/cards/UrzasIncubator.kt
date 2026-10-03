package com.wingedsheep.mtg.sets.definitions.uds.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Urza's Incubator
 * {3}
 * Artifact
 *
 * As this artifact enters, choose a creature type.
 * Creature spells of the chosen type cost {2} less to cast.
 *
 * The reduction applies to every player's creature spells of the chosen type, not just its
 * controller's — hence [SpellCostTarget.AnyCaster].
 */
val UrzasIncubator = card("Urza's Incubator") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose a creature type.\n" +
        "Creature spells of the chosen type cost {2} less to cast."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.AnyCaster(GameObjectFilter.Creature.withChosenSubtype()),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Pete Venters"
        flavorText = "\"Stop thinking like an artificer, Urza, and start thinking like a father!\"\n—Rayne, Academy Chancellor"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdf96c2c-b3d6-4d84-9572-fb115a795bed.jpg?1783946053"

        ruling("2004-10-04", "Multiple Incubators are cumulative.")
        ruling("2004-10-04", "You choose a creature type right as it enters, before any continuous effects are applied or trigged abilities trigger.")
    }
}
