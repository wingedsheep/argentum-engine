package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rite of Ruin — Avacyn Restored #153
 * {5}{R}{R}
 * Sorcery
 * Choose an order for artifacts, creatures, and lands. Each player sacrifices one permanent of
 * their choice of the first type, sacrifices two of their choice of the second type, then
 * sacrifices three of their choice of the third type.
 *
 * The order is chosen on resolution: a `ChooseAction` over the six orderings of the three types.
 * Each ordering then runs three back-to-back each-player sacrifices (1, 2, 3), each one its own
 * APNAP choose-then-sacrifice-simultaneously event — exactly the ruling's three rounds, so a
 * permanent sacrificed in an earlier round is gone before the later rounds choose.
 */
private val riteTypes = listOf(
    "artifacts" to GameObjectFilter.Artifact,
    "creatures" to GameObjectFilter.Creature,
    "lands" to GameObjectFilter.Land,
)

private val riteOrderings = listOf(
    listOf(0, 1, 2), listOf(0, 2, 1),
    listOf(1, 0, 2), listOf(1, 2, 0),
    listOf(2, 0, 1), listOf(2, 1, 0),
)

val RiteOfRuin = card("Rite of Ruin") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose an order for artifacts, creatures, and lands. Each player sacrifices one " +
        "permanent of their choice of the first type, sacrifices two of their choice of the second " +
        "type, then sacrifices three of their choice of the third type."

    spell {
        effect = Effects.ChooseAction(
            riteOrderings.map { order ->
                val (first, second, third) = order.map { riteTypes[it] }
                EffectChoice(
                    label = "${first.first.replaceFirstChar { it.uppercase() }}, then ${second.first}, " +
                        "then ${third.first}",
                    effect = Effects.Sacrifice(first.second, 1, EffectTarget.PlayerRef(Player.Each)) then
                        Effects.Sacrifice(second.second, 2, EffectTarget.PlayerRef(Player.Each)) then
                        Effects.Sacrifice(third.second, 3, EffectTarget.PlayerRef(Player.Each)),
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Clint Cearley"
        flavorText = "Skirsdag cultists refused to quietly accept Avacyn's ascendancy."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89e7fec3-94b5-411e-aeb0-44a64f517986.jpg?1783940677"
        ruling(
            "2012-05-01",
            "First you choose the order for the card types. Then each player in turn order (starting with the " +
                "active player) chooses one permanent they control of the first card type. Then those permanents " +
                "are sacrificed at the same time. Then each player in turn order chooses two permanents they " +
                "control of the second card type. Then those permanents are sacrificed at the same time. " +
                "Finally, each player in turn order chooses three permanents of the third card type, then those " +
                "permanents are sacrificed at the same time."
        )
        ruling(
            "2012-05-01",
            "Permanents sacrificed for one card type won't be on the battlefield to be chosen for subsequent " +
                "card types."
        )
        ruling(
            "2012-05-01",
            "If any abilities trigger while Rite of Ruin is resolving, those abilities will wait until Rite of " +
                "Ruin finishes resolving before going on the stack. Starting with the active player, each player " +
                "puts their abilities on the stack in any order."
        )
    }
}
