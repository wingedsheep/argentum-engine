package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawStepFirstCard
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Notion Thief
 * {2}{U}{B}
 * Creature — Human Rogue
 * 3/1
 *
 * Flash
 * If an opponent would draw a card except the first one they draw in each of their draw steps,
 * instead that player skips that draw and you draw a card.
 *
 * Alhammarret's Archive's per-card draw replacement (`DrawStepFirstCard.EXCLUDED`, which exempts
 * exactly the first card each player draws in their own draw step) aimed at opponents instead of
 * you. A draw replacement's effect runs with the drawing player as its controller, so the card
 * you draw names its player explicitly: [Player.ControllerOfSource], the Thief's controller.
 * Only the draw is replaced — the rest of the opponent's instruction ("draw a card, then discard
 * a card") still happens.
 */
val NotionThief = card("Notion Thief") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 1
    oracleText = "Flash\n" +
        "If an opponent would draw a card except the first one they draw in each of their draw " +
        "steps, instead that player skips that draw and you draw a card."

    keywords(Keyword.FLASH)

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.ControllerOfSource)),
            appliesTo = EventPattern.DrawEvent(
                player = Player.EachOpponent,
                firstInDrawStep = DrawStepFirstCard.EXCLUDED,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/728e660b-ad8b-49d2-a7e5-6588e496519b.jpg?1783940024"
        ruling(
            "2018-03-16",
            "If an opponent is instructed to draw a card then discard a card, and Notion Thief causes you to draw a card instead, that opponent still discards a card. The same is true of any other actions that opponent is instructed to do."
        )
        ruling(
            "2018-03-16",
            "If two or more players each control a Notion Thief and a player would draw a card other than the first one in their draw step, that player chooses one of the applicable Notion Thief effects to apply. Then the player whose Notion Thief's effect was chosen repeats this process among the remaining Notion Thief effects, and so on, until there are no more possible such effects to apply. Each effect can be applied to the card draw only once this way."
        )
        ruling(
            "2018-03-16",
            "The above procedure means that if each player in a two-player game controls a Notion Thief and one would draw a card, it really will be that player who draws a card."
        )
    }
}
