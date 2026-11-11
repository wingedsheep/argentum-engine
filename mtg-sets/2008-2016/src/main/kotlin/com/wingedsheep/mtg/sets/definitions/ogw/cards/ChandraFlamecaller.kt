package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chandra, Flamecaller
 * {4}{R}{R}
 * Legendary Planeswalker — Chandra
 * Loyalty 4
 * +1: Create two 3/1 red Elemental creature tokens with haste. Exile them at the beginning of the
 * next end step.
 * 0: Discard all the cards in your hand, then draw that many cards plus one.
 * −X: Chandra deals X damage to each creature.
 *
 * +1 is Lightning Coils' token shape (two tokens, each exiled by `exileAtStep = Step.END`). 0 is
 * Tolarian Winds' gather → discard → draw pipeline with Into the Night's "plus one": the hand is
 * gathered and its size counted whether or not a replacement (madness) keeps a card out of the
 * graveyard, and an empty hand still draws one. −X is Chandra Nalaar's `loyaltyAbilityX` feeding
 * Savage Twister's group damage.
 */
val ChandraFlamecaller = card("Chandra, Flamecaller") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "+1: Create two 3/1 red Elemental creature tokens with haste. Exile them at the beginning of the next end step.\n" +
        "0: Discard all the cards in your hand, then draw that many cards plus one.\n" +
        "−X: Chandra deals X damage to each creature."

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            count = 2,
            power = 3,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            keywords = setOf(Keyword.HASTE),
            exileAtStep = Step.END,
            imageUri = "https://cards.scryfall.io/normal/front/b/c/bc6f27f7-0248-4c04-8022-41073966e4d8.jpg?1783937859",
        )
        description = "Create two 3/1 red Elemental creature tokens with haste. Exile them at the beginning of the next end step."
    }

    loyaltyAbility(0) {
        effect = Effects.Pipeline {
            val discarded = gather(CardSource.FromZone(Zone.HAND, Player.You))
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
            run(Effects.DrawCards(discarded.count + 1))
        }
        description = "Discard all the cards in your hand, then draw that many cards plus one."
    }

    loyaltyAbilityX {
        effect = Patterns.Group.dealDamageToAll(DynamicAmounts.xValue(), GroupFilter.AllCreatures)
        description = "Chandra deals X damage to each creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "104"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49fdd0d3-7140-4c12-a41c-37eedd986d9e.jpg?1783937909"
        ruling("2016-04-08", "For Chandra's second ability, it doesn't matter if the cards don't go to your graveyard because of a replacement effect. For example, cards with madness discarded this way will still count toward the number of cards you discarded.")
        ruling("2016-01-22", "You can activate Chandra's second ability while you have an empty hand. If you do, you'll draw one card.")
    }
}
