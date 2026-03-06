package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Suspension Field.
 *
 * Suspension Field: {1}{W}
 * Enchantment
 * When this enchantment enters, you may exile target creature with toughness 3 or
 * greater until this enchantment leaves the battlefield.
 */
class SuspensionFieldScenarioTest : ScenarioTestBase() {

    init {
        context("Suspension Field") {
            test("ETB exiles creature with toughness 3 or greater") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Suspension Field")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Tusked Colossodon") // 6/5
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Suspension Field
                game.castSpell(1, "Suspension Field")
                game.resolveStack() // resolve enchantment → ETB triggers

                // ETB trigger fires — select target
                game.hasPendingDecision() shouldBe true
                val colossodonId = game.findPermanent("Tusked Colossodon")!!
                game.selectTargets(listOf(colossodonId))
                game.resolveStack() // resolve ETB

                // Colossodon should be exiled
                game.isOnBattlefield("Tusked Colossodon") shouldBe false

                // Suspension Field should have LinkedExileComponent
                val sfId = game.findPermanent("Suspension Field")!!
                val linked = game.state.getEntity(sfId)?.get<LinkedExileComponent>()
                linked shouldNotBe null
                linked!!.exiledIds shouldHaveSize 1
            }

            test("LTB returns exiled creature when Suspension Field is destroyed") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Suspension Field")
                    .withCardInHand(1, "Naturalize")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Tusked Colossodon") // 6/5
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Suspension Field + ETB
                game.castSpell(1, "Suspension Field")
                game.resolveStack()

                val colossodonId = game.findPermanent("Tusked Colossodon")!!
                game.selectTargets(listOf(colossodonId))
                game.resolveStack()

                // Creature is exiled
                game.isOnBattlefield("Tusked Colossodon") shouldBe false

                // Now destroy Suspension Field with Naturalize
                val sfId = game.findPermanent("Suspension Field")!!
                game.castSpell(1, "Naturalize", sfId)
                game.resolveStack() // resolve Naturalize → SF destroyed → LTB triggers
                game.resolveStack() // resolve LTB trigger → returns creature

                // Suspension Field should be gone
                game.isOnBattlefield("Suspension Field") shouldBe false

                // Creature should be back on the battlefield
                game.isOnBattlefield("Tusked Colossodon") shouldBe true
            }

            test("optional: can decline to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Suspension Field")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Tusked Colossodon")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Suspension Field")
                game.resolveStack()

                // ETB trigger fires — decline by skipping targets
                game.hasPendingDecision() shouldBe true
                game.skipTargets()
                game.resolveStack()

                // Creature should still be on battlefield
                game.isOnBattlefield("Tusked Colossodon") shouldBe true
            }

            test("does not target creatures with toughness less than 3") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Suspension Field")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Valley Dasher") // 2/2
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Suspension Field")
                game.resolveStack() // resolve enchantment

                // ETB trigger should auto-skip since no valid targets (Valley Dasher is 2/2)
                // No pending decision expected
                game.isOnBattlefield("Valley Dasher") shouldBe true
            }
        }
    }
}
