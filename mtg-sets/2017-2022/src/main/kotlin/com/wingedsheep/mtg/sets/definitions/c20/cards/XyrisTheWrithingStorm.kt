package com.wingedsheep.mtg.sets.definitions.c20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawStepFirstCard
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Xyris, the Writhing Storm
 * {2}{G}{U}{R}
 * Legendary Creature — Snake Leviathan
 * 3/5
 * Flying
 * Whenever an opponent draws a card except the first one they draw in each of their draw steps,
 * create a 1/1 green Snake creature token.
 * Whenever Xyris deals combat damage to a player, you and that player each draw that many cards.
 *
 * The draw trigger is Orcish Bowmasters' `Triggers.anOpponent.draws(DrawStepFirstCard.EXCLUDED)`
 * — one trigger per card drawn, the turn-based draw-step draw exempt. The combat trigger draws
 * the damage dealt (`triggerDamageAmount`) for you, then for the damaged player
 * (`Player.TriggeringPlayer`); state-based actions only check after the whole ability resolves,
 * so two players who both overdraw lose simultaneously, as the rulings require.
 */
val XyrisTheWrithingStorm = card("Xyris, the Writhing Storm") {
    manaCost = "{2}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Snake Leviathan"
    power = 3
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever an opponent draws a card except the first one they draw in each of their draw steps, create a 1/1 green Snake creature token.\n" +
        "Whenever Xyris deals combat damage to a player, you and that player each draw that many cards."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anOpponent.draws(DrawStepFirstCard.EXCLUDED)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Snake"),
            imageUri = "https://cards.scryfall.io/normal/front/0/e/0e0f26b2-eaeb-4ea3-8305-1d65d4402d1a.jpg?1783931098",
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(DynamicAmounts.triggerDamageAmount()) then
            Effects.DrawCards(
                DynamicAmounts.triggerDamageAmount(),
                EffectTarget.PlayerRef(Player.TriggeringPlayer),
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "18"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/447db519-7825-4330-99e7-a2429e1f6b2e.jpg?1783931227"
        ruling(
            "2020-04-17",
            "If a spell or ability causes an opponent to put cards into their hand without specifically using the word \"draw,\" it's not a card drawn.",
        )
        ruling(
            "2020-04-17",
            "If the amount of damage Xyris deals is greater than the number of cards in your library and the number of cards in that player's library, you both lose the game at the same time. It doesn't matter if one of you had more cards in library than the other. If there are no other players left in the game, the game is a draw.",
        )
    }
}
