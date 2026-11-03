package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Temple Altisaur (RIX #28):
 *   If a source would deal damage to another Dinosaur you control, prevent all but 1 of that damage.
 *
 * Pins the `PreventDamage.allBut` semantics: another Dinosaur keeps exactly 1, the Altisaur itself and
 * non-Dinosaurs take everything, and alongside "prevent half, rounded up" (Gisela) the recipient's
 * player gets the least-damage order — all-but-1 first (3 -> 1), then half (1 -> 0).
 */
class TempleAltisaurScenarioTest : ScenarioTestBase() {

    init {
        fun damage(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        fun boltScenario(vararg player1Permanents: String): TestGame {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            player1Permanents.forEach { builder.withCardOnBattlefield(1, it) }
            return builder.build()
        }

        context("Temple Altisaur") {

            test("another Dinosaur you control takes only 1 of a 3-damage bolt") {
                val game = boltScenario("Temple Altisaur", "Colossal Dreadmaw")
                val dreadmaw = game.findPermanent("Colossal Dreadmaw")!!

                game.castSpell(2, "Lightning Bolt", dreadmaw).error shouldBe null
                game.resolveStack()

                withClue("all but 1 of the 3 damage is prevented") { damage(game, dreadmaw) shouldBe 1 }
            }

            test("does not protect itself") {
                val game = boltScenario("Temple Altisaur")
                val altisaur = game.findPermanent("Temple Altisaur")!!

                game.castSpell(2, "Lightning Bolt", altisaur).error shouldBe null
                game.resolveStack()

                withClue("'another' Dinosaur — the Altisaur takes the full 3") { damage(game, altisaur) shouldBe 3 }
            }

            test("two Altisaurs protect each other") {
                val game = boltScenario("Temple Altisaur", "Temple Altisaur")
                val (first, _) = game.findPermanents("Temple Altisaur")

                game.castSpell(2, "Lightning Bolt", first).error shouldBe null
                game.resolveStack()

                withClue("the other Altisaur's prevention leaves 1") { damage(game, first) shouldBe 1 }
                game.isOnBattlefield("Temple Altisaur") shouldBe true
            }

            test("non-Dinosaurs take full damage") {
                val game = boltScenario("Temple Altisaur", "Grizzly Bears")
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is not a Dinosaur, so it dies") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("all-but-1 applies before Gisela's half, leaving nothing") {
                val game = boltScenario("Temple Altisaur", "Colossal Dreadmaw", "Gisela, Blade of Goldnight")
                val dreadmaw = game.findPermanent("Colossal Dreadmaw")!!

                game.castSpell(2, "Lightning Bolt", dreadmaw).error shouldBe null
                game.resolveStack()

                withClue("3 -> 1 (all but 1) -> 0 (half, rounded up); the other order would leave 1") {
                    damage(game, dreadmaw) shouldBe 0
                }
            }
        }
    }
}
