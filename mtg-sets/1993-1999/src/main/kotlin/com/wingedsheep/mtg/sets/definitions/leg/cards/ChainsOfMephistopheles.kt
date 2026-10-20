package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawStepFirstCard
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chains of Mephistopheles
 * {1}{B}
 * Enchantment
 * If a player would draw a card except the first one they draw in each of their draw steps, that
 * player discards a card instead. If the player discards a card this way, they draw a card. If the
 * player doesn't discard a card this way, they mill a card.
 *
 * A per-card draw replacement for every player (Bard, King of Dale's draw-step exemption). The
 * replacement's own "they draw a card" is a new draw event that this Chains can't replace again
 * (CR 614.5) even after the discard pauses for a choice — but a second Chains still applies to it,
 * which is what makes two Chains cumulative.
 */
val ChainsOfMephistopheles = card("Chains of Mephistopheles") {
    manaCost = "{1}{B}"
    typeLine = "Enchantment"
    oracleText = "If a player would draw a card except the first one they draw in each of their " +
        "draw steps, that player discards a card instead. If the player discards a card this way, " +
        "they draw a card. If the player doesn't discard a card this way, they mill a card."

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.IfYouDo(
                action = Effects.Discard(1),
                then = Effects.DrawCards(1),
                otherwise = Patterns.Library.mill(1),
            ),
            appliesTo = EventPattern.DrawEvent(
                player = Player.Each,
                firstInDrawStep = DrawStepFirstCard.EXCLUDED,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bb0e884-5bb4-41f3-b04b-6f638357c166.jpg?1783948068"
        ruling("2007-09-16", "If a spell or ability would cause a player to draw multiple cards, this is treated as a number of individual \"draw one card\" actions. Apply the effect of Chains of Mephistopheles to each one.")
        ruling("2007-09-16", "A player's normal card draw on their turn is exempt from this effect. All other draws will be affected.")
        ruling("2007-09-16", "Here's what happens when Chains of Mephistopheles replaces a player's draw: — If that player has at least one card in their hand, they discard a card and then draws a card. — If that player's hand is empty, they put the top card of their library into their graveyard. The player doesn't draw a card at all.")
        ruling("2004-10-04", "The effect is cumulative. If there are two of these on the battlefield, each of them will modify each draw (after the first one if during the draw step), and will cause the player to discard or to \"mill\" a card from their library. As they resolve in order, the player must discard if possible. Once the player fails to discard and instead \"mills\" a card, all further effects of additional Chains of Mephistopheles will not do anything. This is because the \"mill\" also replaces the draw effect and the player is no longer drawing a card. You handle them in order. Each one makes you discard first and then continue or else mill a card and lose the draw.")
        ruling("2004-10-04", "Cards which are drawn as a cost are affected by this card because replacement effects can alter the payment of costs.")
    }
}
