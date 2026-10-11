package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Karn, the Great Creator
 * {4}
 * Legendary Planeswalker — Karn
 * Starting Loyalty: 5
 *
 * Activated abilities of artifacts your opponents control can't be activated.
 * +1: Until your next turn, up to one target noncreature artifact becomes an artifact creature with
 *     power and toughness each equal to its mana value.
 * −2: You may reveal an artifact card you own from outside the game or choose a face-up artifact
 *     card you own in exile. Put that card into your hand.
 *
 * - The static is the Drana and Linvala lock: [PreventActivatedAbilities] over
 *   `Artifact.opponentControls()`, mana abilities included. It reads battlefield permanents only,
 *   matching the ruling that abilities working from other zones stay usable.
 * - The +1 is Xenic Poltergeist's animate with Teferi's "until your next turn" duration.
 * - The −2 is one choice across two pools: "outside the game" is the private [Zone.SIDEBOARD]
 *   (the wish model) and the exile half is your own exile filtered to face-up artifact cards
 *   (a face-down card is out even if you know it's an artifact, per ruling). The "may" is the
 *   up-to-one pick; the chosen card goes to hand revealed.
 */
val KarnTheGreatCreator = card("Karn, the Great Creator") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Planeswalker — Karn"
    startingLoyalty = 5
    oracleText = "Activated abilities of artifacts your opponents control can't be activated.\n" +
        "+1: Until your next turn, up to one target noncreature artifact becomes an artifact creature " +
        "with power and toughness each equal to its mana value.\n" +
        "−2: You may reveal an artifact card you own from outside the game or choose a face-up artifact " +
        "card you own in exile. Put that card into your hand."

    staticAbility {
        ability = PreventActivatedAbilities(filter = GameObjectFilter.Artifact.opponentControls())
    }

    loyaltyAbility(+1) {
        val artifact = target(TargetFilter(GameObjectFilter.Artifact.notCreature()), optional = true)
        effect = Effects.BecomeCreatureWithManaValueStats(
            target = artifact,
            addTypes = setOf("ARTIFACT"),
            duration = Duration.UntilYourNextTurn,
        )
    }

    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            val pool = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.SIDEBOARD, Zone.EXILE),
                    player = Player.You,
                    filter = GameObjectFilter.Artifact.faceUp(),
                ),
            )
            val chosen = chooseUpTo(
                1, from = pool,
                prompt = "You may choose an artifact card from outside the game or a face-up one in exile",
            )
            toHand(chosen, revealed = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ec0c0fb-1a4f-45f4-85b7-346a6d3ce2c5.jpg?1783933488"

        ruling("2019-05-03", "Karn's first ability affects only artifacts on the battlefield. Activated abilities that work in other zones can still be activated.")
        ruling("2019-05-03", "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be activated, only if its controller has continuously controlled that permanent since the beginning of their most recent turn. It doesn't matter how long the permanent has been a creature.")
        ruling("2019-05-03", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2019-05-03", "Karn's last ability can't get a face-down card in exile, even if you know it's an artifact card.")
        ruling("2019-05-03", "In a tournament event, a card you choose from outside the game must come from your sideboard. You may look at your sideboard at any time.")
    }
}
