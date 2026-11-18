package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Brain Maggot
 * {1}{B}
 * Enchantment Creature — Insect
 * 1/1
 *
 * When this creature enters, target opponent reveals their hand and you choose a nonland card
 * from it. Exile that card until this creature leaves the battlefield.
 *
 * Kitesail Freebooter's pipeline with a plain nonland filter. `MoveUntilSourceLeaves` returns the
 * card to its owner's hand the moment the Maggot leaves, and moves nothing if the Maggot has
 * already left by the time the trigger resolves.
 */
val BrainMaggot = card("Brain Maggot") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Insect"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, target opponent reveals their hand and you choose a nonland " +
        "card from it. Exile that card until this creature leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosen = chooseExactly(
                1,
                from = opponentHand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to exile until Brain Maggot leaves the battlefield",
                selectedLabel = "Exile",
                alwaysPrompt = true,
                showAllCards = true,
            )
            run(Effects.MoveUntilSourceLeaves(chosen.asTarget, Zone.EXILE))
        }
        description = "When this creature enters, target opponent reveals their hand and you choose a nonland " +
            "card from it. Exile that card until this creature leaves the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acdc31d5-6726-453e-94e0-69d2fe9447af.jpg?1783939439"
        ruling("2014-04-26", "Brain Maggot's ability causes a zone change with a duration, a new style of ability that's somewhat reminiscent of older cards like Tidehollow Sculler. However, unlike Tidehollow Sculler, Brain Maggot has a single ability that creates two one-shot effects: one that exiles the nonland card when the ability resolves, and another that returns the exiled card to its owner's hand immediately after Brain Maggot leaves the battlefield.")
        ruling("2014-04-26", "If Brain Maggot leaves the battlefield before its enters-the-battlefield ability resolves, the opponent will reveal their hand, but no card will be exiled.")
        ruling("2014-04-26", "The exiled card returns to its owner's hand immediately after Brain Maggot leaves the battlefield. Nothing happens between the two events, including state-based actions.")
        ruling("2014-04-26", "In a multiplayer game, if Brain Maggot's owner leaves the game, the exiled card will return to its owner's hand. Because the one-shot effect that returns the card isn't an ability that goes on the stack, it won't cease to exist along with the leaving player's spells and abilities on the stack.")
    }
}
