package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Glass of the Guildpact — Ravnica Allegiance #233 (canonical printing)
 * {2} · Artifact
 *
 * Multicolored creatures you control get +1/+1.
 */
val GlassOfTheGuildpact = card("Glass of the Guildpact") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Multicolored creatures you control get +1/+1."

    staticAbility {
        ability = ModifyStats(
            1,
            1,
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Dimitar Marinski"
        flavorText = "\"Counterbalanced forces sustain this city. No faction above others. A beautiful idea.\"\n—Emmara"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1022d41-c1d0-42bf-b3e5-d6fb02d47119.jpg?1783933624"
    }
}
