package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Hour of Victory — Aetherdrift #91
 * {2}{B} · Enchantment
 *
 * Start your engines!
 * When this enchantment enters, create a 2/2 black Zombie creature token.
 * Max speed — {1}{B}, Sacrifice this enchantment: Search your library for a card, put it into
 * your hand, then shuffle. Activate only as a sorcery.
 *
 * `startYourEngines()` is keyword-only — raising the controller's speed to 1 is the
 * `StartYourEnginesCheck` state-based action (CR 704.5z), not a trigger. The tutor sits inside a
 * [maxSpeed] block, which turns it into an [com.wingedsheep.sdk.scripting.ActivationRestriction.OnlyIfCondition]
 * on "your speed is 4" so the ability simply isn't a legal action below max speed, and renders the
 * "Max speed — " prefix. "Activate only as a sorcery" is [TimingRule.SorcerySpeed]; the two-part
 * cost is a [Costs.Composite] of the mana and the self-sacrifice.
 */
val HourOfVictory = card("Hour of Victory") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Start your engines!\n" +
        "When this enchantment enters, create a 2/2 black Zombie creature token.\n" +
        "Max speed — {1}{B}, Sacrifice this enchantment: Search your library for a card, put it " +
        "into your hand, then shuffle. Activate only as a sorcery."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.SacrificeSelf)
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                destination = SearchDestination.HAND
            )
            timing = TimingRule.SorcerySpeed
            description = "Search your library for a card, put it into your hand, then shuffle."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9192abc8-05a3-4e72-a634-fc5acbe97b26.jpg?1783907894"
        ruling(
            "2025-02-07",
            "Start your engines! isn't a triggered ability. Increasing your speed to 1 is something " +
                "that happens as a state-based action as soon as you control a permanent with the " +
                "ability. Notably, this includes gaining control of a permanent with the ability that " +
                "another player controls."
        )
        ruling("2025-02-07", "A player \"has max speed\" if their speed is 4.")
    }
}
