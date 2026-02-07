package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Avarax.
 *
 * Card reference:
 * - Avarax ({3}{R}{R}): Creature — Beast, 3/3
 *   Haste
 *   "When Avarax enters the battlefield, you may search your library
 *   for a card named Avarax, reveal it, put it into your hand, then shuffle."
 *   "{1}{R}: Avarax gets +1/+0 until end of turn."
 */
class AvaraxScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Avarax ETB search") {

            test("ETB triggers search for card named Avarax") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Avarax")
                    .withCardInLibrary(1, "Avarax")      // Second copy in library
                    .withCardInLibrary(1, "Mountain")     // Filler
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Avarax
                val castResult = game.castSpell(1, "Avarax")
                withClue("Casting Avarax should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell (creature enters battlefield)
                game.resolveStack()

                // ETB trigger goes on stack — MayEffect asks yes/no
                withClue("Should have pending may decision for search") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose yes to search
                game.answerYesNo(true)

                // Should now have a search decision
                withClue("Should have pending search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Find the Avarax in library to select
                val libraryCards = game.state.getLibrary(game.player1Id)
                val avaraxInLibrary = libraryCards.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Avarax"
                }

                withClue("Should find Avarax in library") {
                    (avaraxInLibrary != null) shouldBe true
                }

                // Select the card
                game.selectCards(listOf(avaraxInLibrary!!))

                // Verify the card went to hand
                withClue("Should have Avarax in hand after search") {
                    game.isInHand(1, "Avarax") shouldBe true
                }

                // Verify on battlefield
                withClue("First Avarax should be on battlefield") {
                    game.isOnBattlefield("Avarax") shouldBe true
                }
            }

            test("may decline to search") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Avarax")
                    .withCardInLibrary(1, "Avarax")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Avarax")
                game.resolveStack()

                // Decline the may effect
                game.answerYesNo(false)

                // Avarax should be on battlefield
                withClue("Avarax should be on battlefield") {
                    game.isOnBattlefield("Avarax") shouldBe true
                }
            }
        }

        context("Avarax haste") {

            test("can attack the turn it enters the battlefield") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardInHand(1, "Avarax")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Avarax
                game.castSpell(1, "Avarax")
                game.resolveStack()

                // Decline search
                game.answerYesNo(false)

                // Move to declare attackers step
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Declare Avarax as attacker — should succeed due to haste
                val attackResult = game.declareAttackers(mapOf("Avarax" to 2))
                withClue("Avarax should be able to attack due to haste: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Verify damage goes through
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("Defender should have taken 3 damage from Avarax") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }

        context("Avarax firebreathing") {

            test("activated ability gives +1/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Avarax")
                    .withLandsOnBattlefield(1, "Mountain", 3) // Need {1}{R} per activation
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val avaraxId = game.findPermanent("Avarax")!!
                val cardDef = cardRegistry.getCard("Avarax")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the pump ability
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = avaraxId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )
                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Avarax should be 4/3
                val projected = stateProjector.project(game.state)
                withClue("Avarax should be 4/3 after one activation") {
                    projected.getPower(avaraxId) shouldBe 4
                    projected.getToughness(avaraxId) shouldBe 3
                }
            }

            test("activated ability stacks multiple times") {
                val game = scenario()
                    .withPlayers("Controller", "Opponent")
                    .withCardOnBattlefield(1, "Avarax")
                    .withLandsOnBattlefield(1, "Mountain", 6) // Enough for 3 activations
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val avaraxId = game.findPermanent("Avarax")!!
                val cardDef = cardRegistry.getCard("Avarax")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate three times
                repeat(3) {
                    val result = game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = avaraxId,
                            abilityId = ability.id,
                            targets = emptyList()
                        )
                    )
                    withClue("Activation ${it + 1} should succeed: ${result.error}") {
                        result.error shouldBe null
                    }
                    game.resolveStack()
                }

                // Avarax should be 6/3 after three activations
                val projected = stateProjector.project(game.state)
                withClue("Avarax should be 6/3 after three activations") {
                    projected.getPower(avaraxId) shouldBe 6
                    projected.getToughness(avaraxId) shouldBe 3
                }
            }
        }
    }
}
