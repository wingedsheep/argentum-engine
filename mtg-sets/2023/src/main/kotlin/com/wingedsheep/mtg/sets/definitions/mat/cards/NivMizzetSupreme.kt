package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastThisWayExile
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Niv-Mizzet, Supreme — March of the Machine: The Aftermath #40
 * {W}{U}{B}{R}{G} · Legendary Creature — Dragon Avatar · 5/5 · Rare
 *
 * The granted jump-start is spelled the way CR 702.133a defines it: a graveyard-cast permission
 * that owes "discard a card" in addition to the spell's other costs, whose spell is exiled instead
 * of going anywhere else whenever it leaves the stack (`CastThisWayExile.ON_ANY_STACK_EXIT`) —
 * resolved, countered, or bounced. Normal timing applies, and "exactly two colors" reads the card's
 * colors in the graveyard (CR 105.2c).
 */
val NivMizzetSupreme = card("Niv-Mizzet, Supreme") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Dragon Avatar"
    oracleText = "Flying, hexproof from monocolored\n" +
        "Each instant and sorcery card in your graveyard that's exactly two colors has jump-start. " +
        "(You may cast that card from your graveyard by discarding a card in addition to paying its " +
        "other costs. Then exile it.)"
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.Monocolored))

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.InstantOrSorcery.withCardPredicate(CardPredicate.HasExactlyColors(2)),
            additionalCost = Costs.additional.DiscardCards(1),
            exileCastThisWay = CastThisWayExile.ON_ANY_STACK_EXIT
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da1ff886-d3c8-43e5-8bf0-ba4f0b259781.jpg?1783916512"
        ruling(
            "2023-05-12",
            "You must still follow any timing restrictions and permissions, including those based on " +
                "the card's type. For instance, you can cast a sorcery using jump-start only when you " +
                "could normally cast a sorcery."
        )
        ruling(
            "2023-05-12",
            "A spell cast using jump-start will always be exiled afterward, whether it resolves, it's " +
                "countered, or it leaves the stack in some other way."
        )
        ruling(
            "2023-05-12",
            "If an effect allows you to pay an alternative cost rather than a spell's mana cost, you " +
                "may pay that alternative cost when you jump-start a spell. You'll still discard a card " +
                "as an additional cost to cast it."
        )
        ruling(
            "2023-05-12",
            "If an instant or sorcery card is put into your graveyard during your turn, you'll be able " +
                "to cast it right away if it's legal to do so, before any player may attempt to remove " +
                "that card from your graveyard."
        )
    }
}
