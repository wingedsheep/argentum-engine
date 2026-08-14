package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aether Syphon
 * {1}{U}{U}
 * Artifact
 *
 * Start your engines!
 * {2}, {T}: Draw a card.
 * Max speed — Whenever you draw a card, each opponent mills two cards.
 *
 * The max-speed clause is an ordinary "whenever you draw a card" triggered ability declared inside
 * the [maxSpeed] block, which folds `Conditions.YouHaveMaxSpeed` into its `triggerCondition` — so
 * the ability exists only while your speed is 4 (CR 702.180a) rather than firing and checking
 * later. `Triggers.YouDraw` fires once per card drawn (CR 121.2), so a multi-card draw mills two
 * per card, matching the printed wording.
 */
val AetherSyphon = card("Aether Syphon") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{2}, {T}: Draw a card.\n" +
        "Max speed — Whenever you draw a card, each opponent mills two cards. (Each opponent puts " +
        "the top two cards of their library into their graveyard.)"

    startYourEngines()

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.DrawCards(1)
        description = "{2}, {T}: Draw a card."
    }

    maxSpeed {
        triggeredAbility {
            trigger = Triggers.YouDraw
            effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.EachOpponent))
            description = "Whenever you draw a card, each opponent mills two cards."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Martin de Diego Sádaba"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7033739-4cd8-4727-b9b5-099fb597006b.jpg?1783907911"
        ruling(
            "2025-02-07",
            "Start your engines! isn't a triggered ability. Increasing your speed to 1 is something " +
                "that happens as a state-based action as soon as you control a permanent with the " +
                "ability. Notably, this includes gaining control of a permanent with the ability " +
                "that another player controls."
        )
        ruling(
            "2025-02-07",
            "“Max speed — [ability]” means “As long as you have max speed, this " +
                "object has [ability].” If the granted ability functions in a zone other than " +
                "the battlefield, the max speed ability does too."
        )
    }
}
