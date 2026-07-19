package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Frantic Firebolt (WOE #130) — {2}{R} Instant.
 *
 * "Frantic Firebolt deals X damage to target creature, where X is 2 plus the number of cards in
 *  your graveyard that are instant cards, sorcery cards, and/or have an Adventure."
 *
 * Exercises the new `CardPredicate.HasAdventure` (adventurer cards count) and the
 * `Filters.InstantSorceryOrAdventure` graveyard tally that backs the dynamic X.
 */
class FranticFireboltScenarioTest : ScenarioTestBase() {

    init {
        context("Frantic Firebolt") {

            test("X = 2 + instant + sorcery + adventurer, ignoring an unrelated enchantment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Frantic Firebolt")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    // Graveyard tally: 1 instant + 1 sorcery + 1 adventurer card = 3 → X = 5.
                    .withCardInGraveyard(1, "Feed the Cauldron")   // Instant
                    .withCardInGraveyard(1, "Ego Drain")           // Sorcery
                    .withCardInGraveyard(1, "Besotted Knight")     // Creature that has an Adventure
                    .withCardInGraveyard(1, "Hopeless Nightmare")  // Enchantment — must NOT count
                    .withCardOnBattlefield(2, "Wall of Stone")    // 0/8, survives 5 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wall = game.findPermanent("Wall of Stone")!!
                game.castSpell(1, "Frantic Firebolt", wall).error shouldBe null
                game.resolveStack()

                withClue("2 base + (instant + sorcery + adventurer) = 5 damage; the enchantment is excluded") {
                    game.findPermanent("Wall of Stone") shouldNotBe null
                    game.state.getEntity(wall)?.get<DamageComponent>()?.amount shouldBe 5
                }
            }

            test("with an empty graveyard X is just the base 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Frantic Firebolt")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Wall of Stone") // 0/8
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wall = game.findPermanent("Wall of Stone")!!
                game.castSpell(1, "Frantic Firebolt", wall).error shouldBe null
                game.resolveStack()

                withClue("no graveyard cards → X = 2") {
                    game.state.getEntity(wall)?.get<DamageComponent>()?.amount shouldBe 2
                }
            }
        }
    }
}
