package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.effects.DestroyAllSharingTypeWithSacrificedEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Endemic Plague
 * {3}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * Destroy all creatures that share a creature type with the sacrificed creature.
 * They can't be regenerated.
 */
val EndemicPlague = card("Endemic Plague") {
    manaCost = "{3}{B}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature. Destroy all creatures that share a creature type with the sacrificed creature. They can't be regenerated."

    additionalCost(AdditionalCost.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = DestroyAllSharingTypeWithSacrificedEffect(noRegenerate = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Nelson DeCastro"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15326971-a53b-45f2-8f1d-1b82935286e1.jpg?1562900082"
    }
}
