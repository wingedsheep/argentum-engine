package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCantLoseGameFromLife
import com.wingedsheep.sdk.scripting.NoMaximumHandSize

/**
 * Marina Vendrell's Grimoire (DSK 64)
 * {5}{U}
 * Legendary Artifact — Book
 *
 * When Marina Vendrell's Grimoire enters, if you cast it, draw five cards.
 * You have no maximum hand size and don't lose the game for having 0 or less life.
 * Whenever you gain life, draw that many cards.
 * Whenever you lose life, discard that many cards. Then if you have no cards in hand, you lose the game.
 *
 * The "don't lose for 0 or less life" clause is the narrow [GrantCantLoseGameFromLife] static, NOT
 * the broad Platinum-Angel [com.wingedsheep.sdk.scripting.GrantCantLoseGame]: the controller can
 * still lose to poison, an empty library, or — crucially — this card's own "if you have no cards in
 * hand, you lose the game" clause (a direct [Effects.LoseGame], not the 704.5a state-based action).
 */
val MarinaVendrellsGrimoire = card("Marina Vendrell's Grimoire") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact — Book"
    oracleText = "When Marina Vendrell's Grimoire enters, if you cast it, draw five cards.\n" +
        "You have no maximum hand size and don't lose the game for having 0 or less life.\n" +
        "Whenever you gain life, draw that many cards.\n" +
        "Whenever you lose life, discard that many cards. Then if you have no cards in hand, you lose the game."

    // When ~ enters, if you cast it, draw five cards.
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Effects.DrawCards(5)
    }

    // You have no maximum hand size.
    staticAbility {
        ability = NoMaximumHandSize()
    }

    // ...and don't lose the game for having 0 or less life.
    staticAbility {
        ability = GrantCantLoseGameFromLife
    }

    // Whenever you gain life, draw that many cards.
    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.DrawCards(DynamicAmounts.triggerLifeGained())
    }

    // Whenever you lose life, discard that many cards. Then if you have no cards in hand, you lose the game.
    triggeredAbility {
        trigger = Triggers.you.losesLife()
        effect = Effects.Discard(DynamicAmounts.triggerLifeLost()) then
            Effects.If(
                condition = Conditions.EmptyHand,
                then = Effects.LoseGame()
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Denys Tsiperko"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ab1aef7-4171-4869-8eb5-fe42e3ca9e45.jpg?1726286094"
    }
}
