package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace, Wielder of Mysteries
 * {1}{U}{U}{U}
 * Legendary Planeswalker — Jace
 * Starting Loyalty: 4
 *
 * If you would draw a card while your library has no cards in it, you win the game instead.
 * +1: Target player mills two cards. Draw a card.
 * −8: Draw seven cards. Then if your library has no cards in it, you win the game.
 *
 * The static is Laboratory Maniac's draw replacement. The −8's explicit win check is Fblthp's
 * post-draw `Exists(..., negate = true)` gate: it covers the ruling where Jace has left the
 * battlefield before the ability resolves (no replacement), since drawing from an empty library
 * only loses at the next state-based-action check, after the ability has finished resolving.
 */
val JaceWielderOfMysteries = card("Jace, Wielder of Mysteries") {
    manaCost = "{1}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 4
    oracleText = "If you would draw a card while your library has no cards in it, you win the game instead.\n" +
        "+1: Target player mills two cards. Draw a card.\n" +
        "−8: Draw seven cards. Then if your library has no cards in it, you win the game."

    // If you would draw a card while your library has no cards in it, you win the game instead.
    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.WinGame(),
            restrictions = listOf(
                Exists(player = Player.You, zone = Zone.LIBRARY, negate = true)
            )
        )
    )

    // +1: Target player mills two cards. Draw a card.
    loyaltyAbility(+1) {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player) then Effects.DrawCards(1)
    }

    // −8: Draw seven cards. Then if your library has no cards in it, you win the game.
    loyaltyAbility(-8) {
        effect = Effects.DrawCards(7) then
            Effects.If(
                condition = Exists(player = Player.You, zone = Zone.LIBRARY, negate = true),
                then = Effects.WinGame(message = "Jace, Wielder of Mysteries: your library has no cards in it.")
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb7d73-4482-4930-8497-cffd169b57e2.jpg?1783933463"

        ruling("2019-05-03", "If for some reason you can't win the game (because your opponent controls Platinum Angel, for example), you won't lose for having tried to draw a card from a library with no cards in it. The draw was still replaced.")
        ruling("2019-05-03", "If two or more players control Jace, Wielder of Mysteries and each player is instructed to draw a number of cards, first the player whose turn it is draws that many cards. If this causes that player to win the game instead, the game is immediately over. If the game isn't over yet, repeat this process for each other player in turn order.")
        ruling("2019-05-03", "If the target player is an illegal target when Jace's first loyalty ability tries to resolve, it doesn't resolve. You won't draw a card.")
        ruling("2019-05-03", "Follow the instructions in the order listed on Jace's first loyalty ability: if you target yourself, you'll put the top two cards of your library into your graveyard and then draw a card.")
        ruling("2019-05-03", "If your library has fewer than seven cards in it while resolving Jace's last ability, and Jace has already left the battlefield, you'll draw as many cards as you can and then win the game before state-based actions would cause you to lose the game for trying to draw from an empty library.")
    }
}
