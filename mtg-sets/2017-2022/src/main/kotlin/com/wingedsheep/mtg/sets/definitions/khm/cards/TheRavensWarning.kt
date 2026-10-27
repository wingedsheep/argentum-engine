package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Raven's Warning — Kaldheim #227
 * {1}{W}{U} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 1/1 blue Bird creature token with flying. You gain 2 life.
 * II — Whenever one or more creatures you control with flying deal combat damage to a player this
 *      turn, look at that player's hand and draw a card.
 * III — You may put a card you own from outside the game on top of your library.
 *
 * Chapter II creates a floating "this turn" triggered ability owned by no permanent
 * ([Effects.CreateGlobalTriggeredAbility] with [Duration.EndOfTurn]), so it keeps working if the
 * Saga leaves. Its trigger is the batched `oneOrMore(...).dealCombatDamageToAPlayer()`: one trigger
 * per player hit however many fliers connected, with "that player" bound as the triggering player.
 * It repeats for each combat-damage step (first strike, extra combats), as the printed "whenever"
 * says. "Outside the game" is the sideboard pseudo-zone; the card is not revealed (2021-02-05 ruling).
 */
val TheRavensWarning = card("The Raven's Warning") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 1/1 blue Bird creature token with flying. You gain 2 life.\n" +
        "II — Whenever one or more creatures you control with flying deal combat damage to a player this turn, " +
        "look at that player's hand and draw a card.\n" +
        "III — You may put a card you own from outside the game on top of your library."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/1/a/1a0cb299-057d-4c39-9d73-3a3512526e4a.jpg?1783928081"
        ) then Effects.GainLife(2)
    }

    sagaChapter(2) {
        effect = Effects.CreateGlobalTriggeredAbility(
            duration = Duration.EndOfTurn,
            ability = TriggeredAbility.create(
                trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
                    .dealCombatDamageToAPlayer(),
                effect = Effects.LookAtHand(EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
                    Effects.DrawCards(1)
            ),
            descriptionOverride = "Whenever one or more creatures you control with flying deal combat damage " +
                "to a player this turn, look at that player's hand and draw a card."
        )
    }

    sagaChapter(3) {
        effect = Patterns.Sideboard.wish(destination = SearchDestination.TOP_OF_LIBRARY, revealed = false)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d00bd457-e1cc-45e1-8b2e-58f886bd6df8.jpg?1783928191"
        ruling("2021-02-05", "You don't have to reveal the card you bring into the game via the chapter III ability.")
        ruling(
            "2021-02-05",
            "In a casual game, a card from outside the game comes from your personal collection. In a game " +
                "played under tournament rules, a card from outside the game comes from your sideboard. You may " +
                "look at your sideboard at any time."
        )
    }
}
