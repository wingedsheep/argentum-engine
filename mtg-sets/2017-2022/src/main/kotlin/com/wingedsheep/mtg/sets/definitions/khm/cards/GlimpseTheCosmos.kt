package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Glimpse the Cosmos
 * {1}{U}
 * Sorcery
 *
 * Look at the top three cards of your library. Put one of them into your hand and the rest on the
 * bottom of your library in any order.
 * As long as you control a Giant, you may cast this card from your graveyard by paying {U} rather
 * than paying its mana cost. If you cast this card this way and it would be put into your
 * graveyard, exile it instead.
 */
val GlimpseTheCosmos = card("Glimpse the Cosmos") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. Put one of them into your hand and the rest on the " +
        "bottom of your library in any order.\nAs long as you control a Giant, you may cast this card from your " +
        "graveyard by paying {U} rather than paying its mana cost. If you cast this card this way and it would be " +
        "put into your graveyard, exile it instead."

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            condition = Conditions.ControlPermanentOfType(Subtype.GIANT),
            alternativeManaCost = ManaCost.parse("{U}"),
            exileCastThisWay = CastThisWayExile.INSTEAD_OF_GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Lorenzo Mastroianni"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc96ebe8-f4b3-4788-a6f5-2a50b6c0d935.jpg?1783928262"
    }
}
