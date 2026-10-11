package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Deadly Dispute
 * {1}{B}
 * Instant
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Draw two cards and create a Treasure token. (It's an artifact with
 * "{T}, Sacrifice this token: Add one mana of any color.")
 */
val DeadlyDispute = card("Deadly Dispute") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\n" +
        "Draw two cards and create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")"

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact.or(GameObjectFilter.Creature)))

    spell {
        effect = Effects.DrawCards(2) then Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Irina Nordsol"
        flavorText = "\"That's a pretty ring! Trade you my knife for it.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7373fe95-ad1c-44b9-8c7f-464ce8cbffc6.jpg"
    }
}
