package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Thrashing Mudspawn.
 *
 * Card reference:
 * - Thrashing Mudspawn (3BB): 4/4 Creature — Beast
 *   Whenever Thrashing Mudspawn is dealt damage, you lose that much life.
 *   Morph {1}{B}{B}
 */
class ThrashingMudspawnScenarioTest : ScenarioTestBase() {

    init {
        context("Thrashing Mudspawn damage trigger") {
            test("controller loses 2 life when dealt 2 damage by Shock") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thrashing Mudspawn")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mudspawn = game.findPermanent("Thrashing Mudspawn")!!
                val initialLife = game.getLifeTotal(1)

                // Opponent casts Shock targeting Thrashing Mudspawn
                val castResult = game.castSpell(2, "Shock", mudspawn)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve Shock (deals 2 damage, triggering the ability)
                game.resolveStack()
                // Resolve the triggered ability (lose 2 life)
                game.resolveStack()

                withClue("Controller should lose 2 life from trigger") {
                    game.getLifeTotal(1) shouldBe initialLife - 2
                }

                // Thrashing Mudspawn is 4/4 so it survives Shock
                withClue("Thrashing Mudspawn should survive 2 damage") {
                    game.findPermanent("Thrashing Mudspawn") shouldBe mudspawn
                }
            }

            test("controller loses life equal to combat damage received") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Thrashing Mudspawn")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)

                // Advance to declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Thrashing Mudspawn" to listOf("Glory Seeker")))

                // Pass priority through combat damage step - trigger should fire
                var iterations = 0
                while (game.state.step != Step.END_COMBAT && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Thrashing Mudspawn (4/4) survives Glory Seeker (2/2) damage
                withClue("Thrashing Mudspawn should survive combat") {
                    game.findPermanent("Thrashing Mudspawn") shouldBe game.findPermanent("Thrashing Mudspawn")
                }

                // Controller should lose 2 life (Glory Seeker deals 2 damage)
                withClue("Controller should lose 2 life from combat damage trigger") {
                    game.getLifeTotal(1) shouldBe initialLife - 2
                }
            }
        }

        context("Thrashing Mudspawn morph - face-down should not trigger") {
            test("face-down Thrashing Mudspawn does not trigger life loss when dealt damage (Rule 708.2)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Thrashing Mudspawn")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)

                // Cast Thrashing Mudspawn face-down for {3}
                val mudspawnCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Thrashing Mudspawn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, mudspawnCardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Pass priority to opponent so they can cast Shock
                game.passPriority()

                // Opponent casts Shock targeting the face-down creature
                val shockResult = game.castSpell(2, "Shock", faceDownId!!)
                withClue("Cast Shock should succeed: ${shockResult.error}") {
                    shockResult.error shouldBe null
                }

                // Resolve Shock (deals 2 damage to face-down 2/2)
                game.resolveStack()

                // Face-down creature is 2/2, Shock deals 2 — it dies
                // But the trigger should NOT have fired (Rule 708.2)
                withClue("Controller should NOT lose life from face-down trigger") {
                    game.getLifeTotal(1) shouldBe initialLife
                }
            }

            test("face-down Thrashing Mudspawn survives 1 damage and does not trigger (Rule 708.2)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Thrashing Mudspawn")
                    .withCardInHand(2, "Spark Spray") // 1 damage
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)

                // Cast Thrashing Mudspawn face-down for {3}
                val mudspawnCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Thrashing Mudspawn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, mudspawnCardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Pass priority to opponent so they can cast Spark Spray
                game.passPriority()

                // Opponent casts Spark Spray targeting the face-down creature (1 damage)
                val sprayResult = game.castSpell(2, "Spark Spray", faceDownId!!)
                withClue("Cast Spark Spray should succeed: ${sprayResult.error}") {
                    sprayResult.error shouldBe null
                }

                // Resolve Spark Spray (deals 1 damage to face-down 2/2 — it survives)
                game.resolveStack()

                // Face-down creature survives
                withClue("Face-down creature should still be on battlefield") {
                    game.state.getBattlefield().any { entityId ->
                        game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                    } shouldBe true
                }

                // Trigger should NOT have fired (Rule 708.2)
                withClue("Controller should NOT lose life from face-down trigger") {
                    game.getLifeTotal(1) shouldBe initialLife
                }
            }

            test("face-down Thrashing Mudspawn blocking does not trigger life loss from combat damage (Rule 708.2)") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardInHand(1, "Thrashing Mudspawn")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Thrashing Mudspawn face-down for {3}
                val mudspawnCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Thrashing Mudspawn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, mudspawnCardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                val initialLife = game.getLifeTotal(1)

                // Pass to opponent's turn — advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Block Glory Seeker (2/2) with face-down (2/2) — both die
                game.declareBlockers(mapOf("Thrashing Mudspawn" to listOf("Glory Seeker")))

                // Pass through combat damage
                var iterations = 0
                while (game.state.step != Step.END_COMBAT && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Trigger should NOT have fired (Rule 708.2)
                withClue("Controller should NOT lose life from face-down combat damage trigger") {
                    game.getLifeTotal(1) shouldBe initialLife
                }
            }
        }
    }
}
