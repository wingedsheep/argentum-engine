package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quasiduplicate
 * {1}{U}{U}
 * Sorcery
 *
 * Create a token that's a copy of target creature you control.
 * Jump-start (You may cast this card from your graveyard by discarding a card in addition to
 * paying its other costs. Then exile this card.)
 *
 * The copy is `Effects.CreateTokenCopyOfTarget` on a creature-you-control target; jump-start
 * mirrors Chemister's Insight — a self graveyard-cast permission owing "discard a card", exiled
 * on any stack exit. Sorcery timing still applies to the jump-started cast.
 */
val Quasiduplicate = card("Quasiduplicate") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Create a token that's a copy of target creature you control.\n" +
        "Jump-start (You may cast this card from your graveyard by discarding a card in addition to " +
        "paying its other costs. Then exile this card.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(creature)
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.DiscardCards(1),
            exileCastThisWay = CastThisWayExile.ON_ANY_STACK_EXIT
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Dmitry Burmak"
        flavorText = "\"My greatest creation—me!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f069646c-fae2-40bd-92bc-35459a84d847.jpg?1783934184"
        ruling(
            "2018-10-05",
            "The token copies exactly what is printed on the creature and nothing else (unless that creature is " +
                "copying something else or is a token; see below). It doesn't copy whether that creature is tapped " +
                "or untapped, whether it has any counters on it or Auras and/or Equipment attached to it, or any " +
                "non-copy effects that changed its power, toughness, types, color, and so on."
        )
        ruling("2018-10-05", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling(
            "2018-10-05",
            "If the copied creature is copying something else, the token enters the battlefield as whatever that " +
                "creature is copying."
        )
        ruling(
            "2018-10-05",
            "If the copied creature is itself a token, the token created by Quasiduplicate copies the original " +
                "characteristics of that token as stated by the effect that created it."
        )
        ruling(
            "2018-10-05",
            "Any enters-the-battlefield abilities of the copied creature will trigger when the token enters the " +
                "battlefield. Any \"As [this creature] enters the battlefield\" or \"[This creature] enters the " +
                "battlefield with\" abilities of the copied creature will also work."
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
        ruling(
            "2018-10-05",
            "If a card with jump-start is put into your graveyard during your turn, you'll be able to cast it " +
                "right away if it's legal to do so, before an opponent can take any actions."
        )
    }
}
