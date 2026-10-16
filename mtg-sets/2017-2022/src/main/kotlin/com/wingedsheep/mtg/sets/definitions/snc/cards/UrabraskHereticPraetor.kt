package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urabrask, Heretic Praetor — Streets of New Capenna #129 (canonical printing)
 * {3}{R}{R} · Legendary Creature — Phyrexian Praetor · 4/4
 *
 * Haste
 * At the beginning of your upkeep, exile the top card of your library. You may play it this turn.
 * At the beginning of each opponent's upkeep, the next time they would draw a card this turn,
 * instead they exile the top card of their library. They may play it this turn.
 *
 * The opponent half is a next-draw shield aimed at the upkeep's player
 * (`Effects.ReplaceNextDraw(…, player = PlayerRef(TriggeringPlayer))`); when it replaces their
 * draw, the impulse reads that player as `Player.TriggeringPlayer` — their library, their permission.
 */
val UrabraskHereticPraetor = card("Urabrask, Heretic Praetor") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    oracleText = "Haste\nAt the beginning of your upkeep, exile the top card of your library. You may play it this turn.\n" +
        "At the beginning of each opponent's upkeep, the next time they would draw a card this turn, instead they " +
        "exile the top card of their library. They may play it this turn."
    power = 4
    toughness = 4

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Exile.impulse(1)
        description = "At the beginning of your upkeep, exile the top card of your library. You may play it this turn."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        effect = Effects.ReplaceNextDraw(
            Patterns.Exile.impulse(1, player = Player.TriggeringPlayer),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
        description = "At the beginning of each opponent's upkeep, the next time they would draw a card this " +
            "turn, instead they exile the top card of their library. They may play it this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "129"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9a4ec18-1da4-43c6-a79a-03fbd4aef3db.jpg?1783923111"
        ruling(
            "2022-04-29",
            "Players must pay all costs and follow all normal timing rules for cards played this way. For " +
                "example, players may play a land card this way only during a main phase of their own turn when " +
                "the stack is empty, and only if they haven't yet played a land this turn."
        )
    }
}
