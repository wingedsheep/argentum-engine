package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra, Flame's Catalyst
 * {4}{R}{R}
 * Legendary Planeswalker — Chandra
 * Loyalty: 5
 *
 * +1: Chandra deals 3 damage to each opponent.
 * −2: You may cast target red instant or sorcery card from your graveyard. If that spell would be
 *     put into your graveyard, exile it instead.
 * −8: Discard your hand, then draw seven cards. Until end of turn, you may cast spells from your
 *     hand without paying their mana costs.
 *
 * The −2 casts during resolution, paying the spell's costs (Emet-Selch of the Third Seat's shape).
 * The −8's free casting is a turn-scoped permission held on the game state, so it outlasts Chandra
 * (who usually dies to it) and changes no timing.
 */
val ChandraFlamesCatalyst = card("Chandra, Flame's Catalyst") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 5
    oracleText = "+1: Chandra deals 3 damage to each opponent.\n" +
        "−2: You may cast target red instant or sorcery card from your graveyard. If that spell would be " +
        "put into your graveyard, exile it instead.\n" +
        "−8: Discard your hand, then draw seven cards. Until end of turn, you may cast spells from your " +
        "hand without paying their mana costs."

    loyaltyAbility(+1) {
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Chandra deals 3 damage to each opponent."
    }

    loyaltyAbility(-2) {
        target(
            TargetFilter(
                GameObjectFilter.InstantOrSorcery.withColor(Color.RED).ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.Pipeline {
            val card = gather(CardSource.ChosenTargets)
            run(Effects.May(
                Effects.CastFromCollection(card, insteadOfGraveyard = AfterResolveDestination.EXILE),
                descriptionOverride = "You may cast target red instant or sorcery card from your graveyard.",
            ))
        }
        description = "You may cast target red instant or sorcery card from your graveyard. If that spell " +
            "would be put into your graveyard, exile it instead."
    }

    loyaltyAbility(-8) {
        effect = Patterns.Hand.discardHand() then
            Effects.DrawCards(7) then
            Effects.GrantFreeCastThisTurn(fromZones = setOf(Zone.HAND))
        description = "Discard your hand, then draw seven cards. Until end of turn, you may cast spells " +
            "from your hand without paying their mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "332"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e49ce44-5286-4310-88c2-f8a1402b113b.jpg?1783930619"
        ruling("2020-06-23", "You must pay the costs for the spell you wish to cast with Chandra's second ability. If that spell has any alternative costs, you may pay those.")
        ruling("2020-06-23", "If you wish to cast the target card, you must cast it while Chandra's second ability is resolving. You can't cast it later in the turn.")
        ruling("2020-06-23", "If you have no cards in hand while Chandra's last ability is resolving, you still draw seven cards and may cast spells from your hand without paying their mana costs this turn.")
        ruling("2020-06-23", "Chandra's last ability doesn't change when you may cast those spells. For example, you can't cast a sorcery spell from your hand during combat.")
        ruling("2020-06-23", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2020-06-23", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
