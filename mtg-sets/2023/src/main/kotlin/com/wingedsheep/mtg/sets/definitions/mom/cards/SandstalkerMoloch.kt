package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sandstalker Moloch — March of the Machine #203.
 * {1}{G}{G} · Creature — Lizard 4/2
 *
 * Flash; an intervening-if ETB dig (top four, a permanent card to hand, rest to the bottom in a
 * random order) gated on an opponent's cast history this turn. The count sums every opponent's
 * cast records, so "an opponent" holds in multiplayer too; the colour check reads the colours
 * captured at cast time, so a countered blue spell still counts.
 */
val SandstalkerMoloch = card("Sandstalker Moloch") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard"
    power = 4
    toughness = 2
    oracleText = "Flash\n" +
        "When this creature enters, if an opponent cast a blue and/or black spell this turn, look " +
        "at the top four cards of your library. You may reveal a permanent card from among them and " +
        "put it into your hand. Put the rest on the bottom of your library in a random order."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastThisTurn(
                player = Player.EachOpponent,
                filter = GameObjectFilter.Any.withAnyColor(Color.BLUE, Color.BLACK),
            ),
            ComparisonOperator.GTE,
            1
        )
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Permanent,
            prompt = "You may reveal a permanent card from among them and put it into your hand",
        )
        description = "When this creature enters, if an opponent cast a blue and/or black spell " +
            "this turn, look at the top four cards of your library. You may reveal a permanent card " +
            "from among them and put it into your hand. Put the rest on the bottom of your library " +
            "in a random order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb17b083-413b-4a09-a935-ac27594e3bd6.jpg?1783916962"
    }
}
