package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck

/**
 * Crypt Lurker
 * {3}{B}
 * Creature — Horror
 * 3/4
 *
 * When this creature enters, you may sacrifice a creature or discard a creature card. If you do,
 * draw a card.
 *
 * Reckless Detective's shape: a "may" over a two-branch choice, each branch paying its own cost
 * and then drawing, so the draw only follows a payment that happened. "A creature" includes the
 * Lurker itself (ruling); the discard branch is offered only with a creature card in hand. Only
 * one payment, one card (ruling).
 */
val CryptLurker = card("Crypt Lurker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you may sacrifice a creature or discard a creature card. " +
        "If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            effect = Effects.ChooseAction(
                choices = listOf(
                    EffectChoice(
                        label = "Sacrifice a creature",
                        effect = Effects.SacrificeOwn(GameObjectFilter.Creature) then Effects.DrawCards(1),
                        feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                            filter = GameObjectFilter.Creature
                        ),
                    ),
                    EffectChoice(
                        label = "Discard a creature card",
                        effect = Patterns.Hand.discardCards(1, filter = GameObjectFilter.Creature) then
                            Effects.DrawCards(1),
                        feasibilityCheck = FeasibilityCheck.HasCardsInZone(
                            zone = Zone.HAND,
                            filter = GameObjectFilter.Creature
                        ),
                    ),
                )
            ),
            descriptionOverride = "You may sacrifice a creature or discard a creature card. If you do, draw a card.",
        )
        description = "When this creature enters, you may sacrifice a creature or discard a creature card. " +
            "If you do, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Svetlin Velinov"
        flavorText = "\"Even the rats refuse to set foot inside. I fear to think what might be waiting in there.\"\n" +
            "—Gravedigger's journal"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0bba170-5176-4fab-a10d-e23d70128875.jpg?1783930711"
        ruling("2020-06-23", "While resolving Crypt Lurker's ability, you can't sacrifice more than one creature, discard more than one card, or do both to draw more than one card.")
        ruling("2020-06-23", "Crypt Lurker can be sacrificed to pay the cost of its own triggered ability.")
    }
}
