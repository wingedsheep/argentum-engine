package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sadistic Shell Game
 * {4}{B}
 * Sorcery
 *
 * Starting with the next opponent in turn order, each player chooses a creature you don't control.
 * Destroy the chosen creatures.
 *
 * One choice per player in [Player.EachStartingWithNextOpponent] order, so the caster chooses last
 * and every player sees the earlier picks (ruling). Inside the per-player loop `Player.You` is the
 * chooser, so "you don't control" is anchored to the caster through `Player.ControllerOfSource`.
 * Nothing stops two players choosing the same creature; it is destroyed once.
 */
val SadisticShellGame = card("Sadistic Shell Game") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Starting with the next opponent in turn order, each player chooses a creature you don't control. Destroy the chosen creatures."

    spell {
        effect = Effects.Pipeline {
            val (chosen) = forEachPlayerCollecting(Player.EachStartingWithNextOpponent) {
                val candidates = gather(
                    CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature.copy(
                            controllerPredicate = ControllerPredicate.Not(
                                ControllerPredicate.ControlledByReferencedPlayer(
                                    EffectTarget.PlayerRef(Player.ControllerOfSource)
                                )
                            )
                        )
                    )
                )
                listOf(
                    chooseExactly(
                        1,
                        from = candidates,
                        chooser = Chooser.Controller,
                        useTargetingUI = true,
                        prompt = "Choose a creature the caster doesn't control"
                    )
                )
            }
            destroy(chosen)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Karl Kopinski"
        flavorText = "\"Shame about your friends. Wanna see what's inside yours?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c5c8eb3-c866-4974-90eb-be99c07af1a1.jpg?1783909657"
    }
}
