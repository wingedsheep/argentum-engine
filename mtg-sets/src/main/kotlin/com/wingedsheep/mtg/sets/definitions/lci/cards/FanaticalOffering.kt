package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Fanatical Offering — LCI #105
 * {1}{B} Instant — Common
 * Artist: Raluca Marinescu
 *
 * "As an additional cost to cast this spell, sacrifice an artifact or creature.
 *  Draw two cards and create a Map token. (It's an artifact with "{1}, {T}, Sacrifice this
 *  token: Target creature you control explores. Activate only as a sorcery.")"
 */
val FanaticalOffering = card("Fanatical Offering") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\n" +
        "Draw two cards and create a Map token. (It's an artifact with \"{1}, {T}, Sacrifice this token: " +
        "Target creature you control explores. Activate only as a sorcery.\")"

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.CreatureOrArtifact))

    spell {
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(2),
                Effects.CreateMapToken()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Raluca Marinescu"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d896dd52-b134-4b55-ab91-ccb05ecc50f4.jpg?1782694527"
    }
}
