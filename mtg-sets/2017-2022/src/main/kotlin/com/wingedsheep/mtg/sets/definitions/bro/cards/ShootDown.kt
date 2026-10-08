package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shoot Down
 * {3}{G}
 * Sorcery
 * Exile target artifact, enchantment, or creature with flying.
 */
val ShootDown = card("Shoot Down") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Exile target artifact, enchantment, or creature with flying."

    spell {
        val target = target(
            TargetFilter(
                GameObjectFilter.Artifact or
                    GameObjectFilter.Enchantment or
                    GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
            ),
        )
        effect = Effects.Exile(target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Lius Lasahido"
        flavorText = "Gwenna stayed her hand only once, to save young Harbin's life—a mistake she would never repeat."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20014a1c-197c-4e04-94b2-874886183e2f.jpg"
    }
}
