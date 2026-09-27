package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Atraxa's Fall — March of the Machine #176
 * {1}{G} · Sorcery
 *
 * Destroy target artifact, battle, enchantment, or creature with flying.
 *
 * The four arms are one heterogeneous [GameObjectFilter.or] union; only the creature arm carries
 * the flying restriction.
 */
val AtraxasFall = card("Atraxa's Fall") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, battle, enchantment, or creature with flying."

    spell {
        val t = target(
            TargetFilter(
                GameObjectFilter.Artifact or GameObjectFilter.Battle or GameObjectFilter.Enchantment or
                    GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
            )
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Xavier Ribeiro"
        flavorText = "\"Fallen angels should stay fallen.\"\n—Henzie \"Toolbox\" Torre"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e08ed414-77bf-402a-82a8-9d4e1bd627a1.jpg?1783916977"
    }
}
