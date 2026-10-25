package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Archaeomancer's Map
 * {2}{W}
 * Artifact
 * When this artifact enters, search your library for up to two basic Plains cards, reveal them,
 * put them into your hand, then shuffle.
 * Whenever a land an opponent controls enters, if that player controls more lands than you, you
 * may put a land card from your hand onto the battlefield.
 *
 * "That player" is the entering land's controller — [Player.ControllerOfTriggeringEntity] — so
 * the intervening-if (CR 603.4) compares that one opponent's lands against yours, on trigger and
 * again on resolution. The "you may" is the up-to-one hand selection of
 * [Patterns.Hand.putFromHand] (Burgeoning's shape).
 */
val ArchaeomancersMap = card("Archaeomancer's Map") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, search your library for up to two basic Plains cards, reveal them, put them into your hand, then shuffle.\n" +
        "Whenever a land an opponent controls enters, if that player controls more lands than you, you may put a land card from your hand onto the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            count = 2,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.opponentControls()).enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.ControllerOfTriggeringEntity, GameObjectFilter.Land).count(),
            ComparisonOperator.GT,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land).count(),
        )
        effect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Ovidio Cartagena"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/703d0bdc-01a0-4ba8-8536-e7425dfb3b1f.jpg?1783927609"
        ruling("2021-04-16", "The last ability triggers only if that opponent controls more lands than you after the land enters the battlefield. The ability will check again as it tries to resolve. If that opponent doesn't still control more lands than you at that time, the ability won't resolve.")
    }
}
