package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana, Dreadhorde General
 * {4}{B}{B}
 * Legendary Planeswalker — Liliana
 * Starting Loyalty: 6
 *
 * Whenever a creature you control dies, draw a card.
 * +1: Create a 2/2 black Zombie creature token.
 * −4: Each player sacrifices two creatures of their choice.
 * −9: Each opponent chooses a permanent they control of each permanent type and sacrifices
 *     the rest.
 *
 * The two sacrifice abilities are both "every affected player chooses, then the sacrifices happen"
 * (CR 101.4), so both resolve in APNAP order — via [Player.ActivePlayerFirst] for the −4 and via
 * `chooseOnePerCategory`'s own APNAP walk for the −9 — the active player picks first and each later
 * player chooses knowing what came before.
 *
 * The −9 uses [Filters.PermanentTypes] rather than a hand-written type list: one pick per permanent
 * type the opponent controls, where a permanent with several types can be the pick for each of them
 * (an artifact creature can be both the surviving artifact and the surviving creature).
 */
val LilianaDreadhordeGeneral = card("Liliana, Dreadhorde General") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 6
    oracleText = "Whenever a creature you control dies, draw a card.\n" +
        "+1: Create a 2/2 black Zombie creature token.\n" +
        "−4: Each player sacrifices two creatures of their choice.\n" +
        "−9: Each opponent chooses a permanent they control of each permanent type and " +
        "sacrifices the rest."

    // Whenever a creature you control dies, draw a card.
    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        effect = Effects.DrawCards(1)
    }

    // +1: Create a 2/2 black Zombie creature token.
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/9/3/935d1421-0d26-468f-aa86-488b0ba25e77.jpg?1783933352"
        )
    }

    // −4: Each player sacrifices two creatures of their choice.
    // A player who controls only one creature sacrifices just that one (the executor auto-takes
    // everything when the board is at or under the required count).
    loyaltyAbility(-4) {
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.Creature,
            count = 2,
            target = EffectTarget.PlayerRef(Player.ActivePlayerFirst)
        )
    }

    // −9: Each opponent chooses a permanent they control of each permanent type and sacrifices
    // the rest.
    //
    // Gather → choose-one-per-type → sacrifice the difference. The pool is scoped by the filter
    // rather than the gather's `player` (which resolves to a single player), so "each opponent" is
    // one control predicate; `chooseOnePerCategory` then asks each of those opponents in APNAP
    // order, and `exclude` is the "the rest" half.
    loyaltyAbility(-9) {
        effect = Effects.Pipeline {
            val atRisk = gather(GameObjectFilter.Permanent.opponentControls())
            val kept = chooseOnePerCategory(atRisk, Filters.PermanentTypes)
            sacrifice(exclude(atRisk, kept))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "97"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d75ebba8-34ca-47a0-bf13-8318ad73b343.jpg?1783933442"

        ruling("2024-01-12", "If Liliana dies at the same time as one or more creatures you control, her first ability triggers for each of those creatures.")
        ruling("2024-01-12", "If Liliana somehow becomes a creature and dies, her first ability will trigger.")
        ruling("2024-01-12", "As Liliana's second loyalty ability resolves, first the player whose turn it is chooses two creatures they control, then each other player in turn order does the same, knowing the choices made before them. Then all the chosen creatures are sacrificed at the same time. If any player can choose only one creature, that player does so.")
        ruling("2024-01-12", "As Liliana's last ability resolves, the next opponent in turn order (or, if it's somehow an opponent's turn, that opponent) makes all of their choices for it, then each other opponent in turn order does the same, knowing the choices made before them. Then all the unchosen permanents are sacrificed at the same time.")
        ruling("2024-01-12", "The permanent types are artifact, creature, enchantment, land, and planeswalker. Supertypes, like legendary, aren't permanent types.")
        ruling("2024-01-12", "While making choices for Liliana's last ability, if a permanent has more than one permanent type, it can count for any of them. For example, you could choose an artifact creature as the artifact you're sparing, another creature as the creature, and an enchantment creature as the enchantment. Similarly, you could choose an enchantment creature as both the creature and the enchantment that you're sparing, even if you control another creature and/or another enchantment.")
    }
}
