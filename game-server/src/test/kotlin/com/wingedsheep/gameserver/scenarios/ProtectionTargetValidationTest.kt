package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests that protection from a color causes targeted spells/abilities
 * to fizzle when targets gain protection after the spell is cast
 * but before it resolves.
 *
 * Scenario: Player 1 casts Choking Tethers (blue, "Tap up to four target creatures")
 * targeting Player 2's creature. Player 2 responds with Akroma's Blessing
 * ("Choose a color. Creatures you control gain protection from the chosen color
 * until end of turn"), choosing blue. When Choking Tethers tries to resolve,
 * the target has protection from blue, making it an illegal target. The spell fizzles.
 */
class ProtectionTargetValidationTest : ScenarioTestBase() {

    init {
        context("Protection invalidates targets on resolution") {

            test("Choking Tethers fizzles when target gains protection from blue via Akroma's Blessing") {
                val game = scenario()
                    .withPlayers("Caster", "Defender")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Choking Tethers")
                    .withCardInHand(2, "Akroma's Blessing")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Player 1 casts Choking Tethers targeting Grizzly Bears
                val castResult = game.castSpell(1, "Choking Tethers", bearsId)
                withClue("Choking Tethers should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 1 passes priority to Player 2
                game.passPriority()

                // Player 2 responds with Akroma's Blessing (no targets - affects all your creatures)
                val blessingResult = game.castSpell(2, "Akroma's Blessing")
                withClue("Akroma's Blessing should be cast successfully: ${blessingResult.error}") {
                    blessingResult.error shouldBe null
                }

                // Resolve stack until Akroma's Blessing resolves (pauses for color choice)
                game.resolveStack()

                // Handle the color choice decision - choose blue for protection
                val decision = game.getPendingDecision()
                withClue("Should have a color choice decision") {
                    decision.shouldBeInstanceOf<ChooseColorDecision>()
                }
                game.submitDecision(
                    ColorChosenResponse((decision as ChooseColorDecision).id, Color.BLUE)
                )

                // Continue resolving - Choking Tethers should now fizzle
                game.resolveStack()

                // Grizzly Bears should NOT be tapped (protection from blue made it an illegal target)
                withClue("Grizzly Bears should still be on the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
                withClue("Grizzly Bears should NOT be tapped (spell fizzled due to protection)") {
                    game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe false
                }
            }

            test("Choking Tethers still taps creatures without protection") {
                // Control test: without protection, the spell should work normally
                val game = scenario()
                    .withPlayers("Caster", "Defender")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Choking Tethers")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Player 1 casts Choking Tethers targeting Grizzly Bears
                val castResult = game.castSpell(1, "Choking Tethers", bearsId)
                withClue("Choking Tethers should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve stack normally
                game.resolveStack()

                // Grizzly Bears SHOULD be tapped (no protection)
                withClue("Grizzly Bears should be tapped") {
                    game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe true
                }
            }

            test("protection from wrong color does not cause fizzle") {
                // Protection from red should not stop a blue spell
                val game = scenario()
                    .withPlayers("Caster", "Defender")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Choking Tethers")
                    .withCardInHand(2, "Akroma's Blessing")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Player 1 casts Choking Tethers targeting Grizzly Bears
                game.castSpell(1, "Choking Tethers", bearsId)

                // Player 1 passes priority
                game.passPriority()

                // Player 2 responds with Akroma's Blessing
                game.castSpell(2, "Akroma's Blessing")

                // Resolve until color choice
                game.resolveStack()

                // Choose RED (not blue) - should NOT protect against blue spell
                val decision = game.getPendingDecision() as ChooseColorDecision
                game.submitDecision(ColorChosenResponse(decision.id, Color.RED))

                // Continue resolving
                game.resolveStack()

                // Grizzly Bears SHOULD be tapped (protection from red doesn't stop blue spell)
                withClue("Grizzly Bears should be tapped (protection from red doesn't stop blue Choking Tethers)") {
                    game.state.getEntity(bearsId)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
