package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Iymrith, Desert Doom
 * {3}{U}{U}
 * Legendary Creature — Dragon
 * 5/5
 * Flying
 * Iymrith has ward {4} as long as it's untapped.
 * Whenever Iymrith deals combat damage to a player, draw a card. Then if you have fewer than three
 * cards in hand, draw cards equal to the difference.
 *
 * The ward is a self-grant gated on [Conditions.SourceIsUntapped], so it stops applying the moment
 * Iymrith taps to attack. The refill amount is read when the second draw resolves — after the first
 * card is already in hand — and [DynamicAmounts.nonNegative] floors it at zero.
 */
val IymrithDesertDoom = card("Iymrith, Desert Doom") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Dragon"
    oracleText = "Flying\nIymrith has ward {4} as long as it's untapped.\nWhenever Iymrith deals combat damage to a player, draw a card. Then if you have fewer than three cards in hand, draw cards equal to the difference."
    power = 5
    toughness = 5
    keywords(Keyword.FLYING)
    staticAbility {
        condition = Conditions.SourceIsUntapped
        ability = GrantWard(WardCost.Mana("{4}"), Filters.Self)
    }
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1) then Effects.DrawCards(
            DynamicAmounts.nonNegative(3 - DynamicAmounts.cardsInYourHand())
        )
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "62"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3e6025d-d273-4ae4-9929-5e588612f1b3.jpg?1783926511"
    }
}
