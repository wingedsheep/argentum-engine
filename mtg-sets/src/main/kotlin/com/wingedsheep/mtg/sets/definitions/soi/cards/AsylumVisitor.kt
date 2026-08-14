package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Asylum Visitor — Shadows over Innistrad #99. */
val AsylumVisitor = card("Asylum Visitor") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Wizard"
    oracleText = "At the beginning of each player's upkeep, if that player has no cards in hand, " +
        "you draw a card and you lose 1 life.\n" +
        "Madness {1}{B} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = activePlayerHasEmptyHand()
        effect = ConditionalEffect(
            condition = activePlayerHasEmptyHand(),
            effect = Effects.DrawCards(1).then(Effects.LoseLife(1)),
        )
    }

    madness("{1}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Bastien L. Deharme"
        flavorText = "\"The ravings of the mad are laced with eldritch knowledge.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55a4bf6c-167a-4122-a55b-7bc28ca4f0d4.jpg?1783937781"
        ruling("2016-04-08", "Asylum Visitor's triggered ability checks the active player's hand as the upkeep begins and as the trigger resolves. If that player has a card in hand as it resolves, you won't draw a card or lose 1 life.")
    }
}

private fun activePlayerHasEmptyHand() = Compare(
    DynamicAmount.Count(Player.TriggeringPlayer, Zone.HAND),
    ComparisonOperator.EQ,
    DynamicAmount.Fixed(0),
)
