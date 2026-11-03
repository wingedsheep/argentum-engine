package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Cloak and Phaser
 * {8}{U}
 * Sorcery
 *
 * Strange new worlds — This spell costs {X} less to cast, where X is the number of differently
 * named lands you control.
 * Create two 2/2 black Romulan creature tokens. Draw two cards, then discard a card.
 *
 * "Strange new worlds" is an ability word with no rules meaning.
 */
val CloakAndPhaser = card("Cloak and Phaser") {
    manaCost = "{8}{U}"
    typeLine = "Sorcery"
    colorIdentity = "U"
    oracleText = "Strange new worlds — This spell costs {X} less to cast, where X is the number of differently named lands you control.\n" +
        "Create two 2/2 black Romulan creature tokens. Draw two cards, then discard a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.DifferentlyNamedPermanentsYouControl(Filters.Land),
            ),
        )
    }

    spell {
        effect = Effects.CreateToken(
            power = 2, toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Romulan"),
            count = 2,
        ) then Effects.DrawCards(2) then Effects.Discard(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "David Szabo"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d9fb7e7-582d-41e7-be9e-98372505ea44.jpg?1793187986"
    }
}
