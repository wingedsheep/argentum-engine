package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gollum, Scheming Guide
 * {1}{B}
 * Legendary Creature — Halfling Horror
 * 2/1
 *
 * Whenever Gollum attacks, look at the top two cards of your library, put them back in any order,
 * then choose land or nonland. An opponent guesses whether the top card of your library is the
 * chosen kind. Reveal that card. If they guessed right, remove Gollum from combat. Otherwise, you
 * draw a card and Gollum can't be blocked this turn.
 *
 * The attack trigger composes a look-at-top-2-and-reorder ([Patterns.Library.lookAtTopAndReorder])
 * with the reusable opponent-guess primitive ([Effects.OpponentGuessesTopCardKind]): the controller
 * picks the framing land/nonland kind, an opponent guesses the top card's actual kind, that card is
 * revealed and compared, and the matching branch resolves — remove Gollum from combat on a correct
 * guess, or (on a wrong guess) draw a card and make Gollum unblockable this turn.
 */
val GollumSchemingGuide = card("Gollum, Scheming Guide") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Halfling Horror"
    power = 2
    toughness = 1
    oracleText = "Whenever Gollum attacks, look at the top two cards of your library, put them back " +
        "in any order, then choose land or nonland. An opponent guesses whether the top card of " +
        "your library is the chosen kind. Reveal that card. If they guessed right, remove Gollum " +
        "from combat. Otherwise, you draw a card and Gollum can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Library.lookAtTopAndReorder(count = 2) then
            Effects.OpponentGuessesTopCardKind(
                onGuessedRight = Effects.RemoveFromCombat(EffectTarget.Self),
                onGuessedWrong = Effects.DrawCards(1) then
                    Effects.GrantKeyword(
                        AbilityFlag.CANT_BE_BLOCKED,
                        target = EffectTarget.Self,
                        duration = Duration.EndOfTurn
                    )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "292"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f93ad17-b655-4a10-990e-b26ead90d221.jpg?1687424790"
    }
}
