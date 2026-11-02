package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Chemister's Insight
 * {3}{U}
 * Instant
 *
 * Draw two cards.
 * Jump-start (You may cast this card from your graveyard by discarding a card in addition to
 * paying its other costs. Then exile this card.)
 *
 * Jump-start is spelled the way CR 702.133a defines it: a self graveyard-cast permission owing
 * "discard a card" on top of the mana cost, whose spell is exiled whenever it leaves the stack
 * (`CastThisWayExile.ON_ANY_STACK_EXIT`) — resolved, countered, or bounced. Normal timing applies.
 */
val ChemistersInsight = card("Chemister's Insight") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        effect = Effects.DrawCards(2)
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.DiscardCards(1),
            exileCastThisWay = CastThisWayExile.ON_ANY_STACK_EXIT
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Josh Hass"
        flavorText = "\"My specialty is theoretical universal limits and ways to circumvent them.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e13a295-0d51-4b56-8c58-0896eb41c567.jpg?1783934192"
        ruling(
            "2018-10-05",
            "If a card with jump-start is put into your graveyard during your turn, you'll be able to cast it " +
                "right away if it's legal to do so, before an opponent can take any actions."
        )
        ruling(
            "2018-10-05",
            "You must still follow any timing restrictions and permissions when casting a spell with jump-start, " +
                "including those based on the card's type. For instance, you can cast a sorcery using jump-start " +
                "only when you could normally cast a sorcery."
        )
        ruling(
            "2018-10-05",
            "A spell cast using jump-start will always be exiled afterward, whether it resolves, it's countered, " +
                "or it leaves the stack in some other way."
        )
        ruling(
            "2018-10-05",
            "If an effect allows you to pay an alternative cost rather than a spell's mana cost, you may pay that " +
                "alternative cost when you jump-start a spell. You'll still discard a card as an additional cost " +
                "to cast it."
        )
    }
}
