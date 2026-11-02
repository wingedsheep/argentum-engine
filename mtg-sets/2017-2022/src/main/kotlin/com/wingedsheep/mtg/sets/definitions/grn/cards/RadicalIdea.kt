package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Radical Idea
 * {1}{U}
 * Instant
 *
 * Draw a card.
 * Jump-start (You may cast this card from your graveyard by discarding a card in addition to
 * paying its other costs. Then exile this card.)
 *
 * Jump-start mirrors Quasiduplicate — a self graveyard-cast permission owing "discard a card",
 * exiled on any stack exit.
 */
val RadicalIdea = card("Radical Idea") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.DiscardCards(1),
            exileCastThisWay = CastThisWayExile.ON_ANY_STACK_EXIT
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Izzy"
        flavorText = "Genius is finding the edge of what's possible, then jumping over it."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9570734-5e9b-46ff-b606-9759b5195756.jpg?1783934183"
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
        ruling(
            "2018-10-05",
            "If a card with jump-start is put into your graveyard during your turn, you'll be able to cast it " +
                "right away if it's legal to do so, before an opponent can take any actions."
        )
    }
}
