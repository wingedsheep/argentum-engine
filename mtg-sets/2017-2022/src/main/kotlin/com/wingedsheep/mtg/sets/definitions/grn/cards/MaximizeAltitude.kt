package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Maximize Altitude
 * {U}
 * Sorcery
 *
 * Target creature gets +1/+1 and gains flying until end of turn.
 * Jump-start (You may cast this card from your graveyard by discarding a card in addition to
 * paying its other costs. Then exile this card.)
 */
val MaximizeAltitude = card("Maximize Altitude") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +1/+1 and gains flying until end of turn.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, t) then Effects.GrantKeyword(Keyword.FLYING, t)
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
        collectorNumber = "43"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b5a08a0-b002-4bfe-9cac-d2bf96175f8e.jpg?1783934187"
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
    }
}
