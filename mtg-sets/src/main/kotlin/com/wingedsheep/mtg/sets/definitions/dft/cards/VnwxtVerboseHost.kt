package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vnwxt, Verbose Host
 * {1}{U}
 * Legendary Creature — Homunculus
 * 0/4
 *
 * Start your engines!
 * You have no maximum hand size.
 * Max speed — If you would draw a card, draw two cards instead.
 *
 * The max-speed clause is a *replacement* effect, declared through `maxSpeed { replacementEffect(…) }`.
 * That path folds the gate into [ReplaceDrawWithEffect]'s own `restrictions` slot — evaluated in the
 * drawing player's context, which is this card's controller for a `Player.You` draw — because a
 * conditional wrapper would be invisible to the interception sites that read
 * `ReplacementEffectSourceComponent` directly.
 *
 * Modelled per card draw rather than as a [ModifyDrawAmount] on the announcement, because the
 * oracle text says "if you would draw a card" — it does not refer to the number of cards drawn,
 * which is what CR 121.2a scopes the announcement-level modification to. The distinction is
 * observable: CR 616.1g requires an effect applying to a *contained* event (one card draw) to be
 * chosen only after one applying to the *containing* event (the announced quantity) has been.
 * Modelling both here and on Quantum Riddler at the announcement would put them in one CR 616.1e
 * pool and let the player pick an order that yields 3 where the rules give 4.
 *
 * The rulings still hold: CR 614.5 stops the replacement applying to its own two draws, so
 * Harmonize's three draws each become two — six cards.
 */
val VnwxtVerboseHost = card("Vnwxt, Verbose Host") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Homunculus"
    power = 0
    toughness = 4
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "You have no maximum hand size.\n" +
        "Max speed — If you would draw a card, draw two cards instead."

    startYourEngines()

    staticAbility {
        ability = NoMaximumHandSize
    }

    maxSpeed {
        replacementEffect(
            ReplaceDrawWithEffect(
                replacementEffect = DrawCardsEffect(2),
                appliesTo = EventPattern.DrawEvent(player = Player.You),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Izzy"
        flavorText = "\"Racers! Start! Your! Engiiiiiines!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/893254c7-64cc-4cb9-b79f-2c41a8935ea0.jpg?1783907900"

        ruling(
            "2025-02-07",
            "If a spell or ability causes you to draw multiple cards, this creature's last ability " +
                "doubles each card draw. For example, if you cast Harmonize (\"Draw three cards\"), " +
                "you'll draw six cards."
        )
        ruling(
            "2025-02-07",
            "The effects of multiple such effects are cumulative. For example, if you have max speed " +
                "and control both Vnwxt and Thought Reflection (an enchantment with the same ability), " +
                "you'll draw four times the original number of cards."
        )
        ruling(
            "2025-02-07",
            "If two or more replacement effects would apply to a card-drawing event, the player who's " +
                "drawing the card chooses what order to apply them."
        )
    }
}
