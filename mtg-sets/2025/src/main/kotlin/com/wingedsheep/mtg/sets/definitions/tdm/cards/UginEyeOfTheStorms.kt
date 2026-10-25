package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ugin, Eye of the Storms — Tarkir: Dragonstorm #1
 * {7} · Legendary Planeswalker — Ugin · Mythic · Loyalty 7
 *
 * When you cast this spell, exile up to one target permanent that's one or more colors.
 * Whenever you cast a colorless spell, exile up to one target permanent that's one or more colors.
 * +2: You gain 3 life and draw a card.
 * 0: Add {C}{C}{C}.
 * −11: Search your library for any number of colorless nonland cards, exile them, then shuffle.
 *      Until end of turn, you may cast those cards without paying their mana costs.
 */
private val coloredPermanent = TargetFilter(
    GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPermanent, CardPredicate.IsColored))
)

private fun exileUpToOneColoredPermanent() = TargetObject(
    optional = true,
    filter = coloredPermanent,
    id = "target permanent that's one or more colors"
)

private val colorlessSpell = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsColorless))

val UginEyeOfTheStorms = card("Ugin, Eye of the Storms") {
    manaCost = "{7}"
    typeLine = "Legendary Planeswalker — Ugin"
    startingLoyalty = 7
    oracleText = "When you cast this spell, exile up to one target permanent that's one or more colors.\n" +
        "Whenever you cast a colorless spell, exile up to one target permanent that's one or more colors.\n" +
        "+2: You gain 3 life and draw a card.\n" +
        "0: Add {C}{C}{C}.\n" +
        "−11: Search your library for any number of colorless nonland cards, exile them, then " +
        "shuffle. Until end of turn, you may cast those cards without paying their mana costs."

    // When you cast this spell, exile up to one target colored permanent.
    triggeredAbility {
        val target = target(exileUpToOneColoredPermanent())
        trigger = Triggers.self.isCast()
        effect = Effects.Exile(target)
        description = "When you cast this spell, exile up to one target permanent that's one or more colors."
    }

    // Whenever you cast a colorless spell, exile up to one target colored permanent.
    triggeredAbility {
        val target = target(exileUpToOneColoredPermanent())
        trigger = Triggers.you.casts(colorlessSpell)
        effect = Effects.Exile(target)
        description = "Whenever you cast a colorless spell, exile up to one target permanent that's one or more colors."
    }

    // +2: You gain 3 life and draw a card.
    loyaltyAbility(+2) {
        description = "You gain 3 life and draw a card."
        effect = Effects.GainLife(3) then Effects.DrawCards(1)
    }

    // 0: Add {C}{C}{C}.
    loyaltyAbility(0) {
        description = "Add {C}{C}{C}."
        effect = Effects.AddColorlessMana(3)
    }

    // −11: Search library for any number of colorless nonland cards, exile them, then shuffle.
    // Until end of turn, you may cast those cards without paying their mana costs.
    loyaltyAbility(-11) {
        description = "Search your library for any number of colorless nonland cards, exile them, then " +
            "shuffle. Until end of turn, you may cast those cards without paying their mana costs."
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter(cardPredicates = listOf(CardPredicate.IsColorless, CardPredicate.IsNonland))
                ),
                search = true
            )
            val exiled = chooseAnyNumber(
                from = searchable,
                prompt = "Search your library for any number of colorless nonland cards"
            )
            exile(exiled)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(Effects.GrantMayPlayFromExile(exiled, MayPlayExpiry.EndOfTurn))
            run(Effects.GrantPlayWithoutPayingCost(exiled))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64a5d494-efa1-446b-bebe-2ad36e154376.jpg?1761770162"
    }
}
