package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Ixidor, Reality Sculptor:
 * {3}{U}{U}
 * Legendary Creature — Human Wizard
 * 3/4
 * Face-down creatures get +1/+1.
 * {2}{U}: Turn target face-down creature face up.
 *
 * Cards used:
 * - Ixidor, Reality Sculptor ({3}{U}{U}, 3/4 Legendary Creature — Human Wizard)
 * - Battering Craghorn ({3}{R}, 3/1 First Strike, Morph {1}{R})
 * - Whipcorder ({W}{W}, 2/2 Creature — Human Soldier Rebel, Morph {W})
 */
class IxidorRealitySculptorScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Ixidor's static ability — face-down creatures get +1/+1") {

            test("face-down creature gets +1/+1 from Ixidor") {
                val game = scenario()
                    .withPlayers("Ixidor Player", "Opponent")
                    .withCardOnBattlefield(1, "Ixidor, Reality Sculptor")
                    .withCardInHand(1, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Battering Craghorn face-down for {3}
                val craghornCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, craghornCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
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

                // Verify projected stats are 3/3 (base 2/2 + Ixidor's +1/+1)
                val projected = stateProjector.project(game.state)
                withClue("Face-down creature should be 3/3 with Ixidor's buff") {
                    projected.getPower(faceDownId!!) shouldBe 3
                    projected.getToughness(faceDownId) shouldBe 3
                }
            }

            test("buff goes away when creature is turned face up") {
                val game = scenario()
                    .withPlayers("Ixidor Player", "Opponent")
                    .withCardOnBattlefield(1, "Ixidor, Reality Sculptor")
                    .withCardInHand(1, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Island", 3) // for morph {3}
                    .withLandsOnBattlefield(1, "Mountain", 3) // for unmorph {1}{R}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Battering Craghorn face-down
                val craghornCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                game.execute(CastSpell(game.player1Id, craghornCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Verify it's 3/3 while face-down
                val projectedBefore = stateProjector.project(game.state)
                withClue("Face-down creature should be 3/3 with Ixidor's buff") {
                    projectedBefore.getPower(faceDownId) shouldBe 3
                    projectedBefore.getToughness(faceDownId) shouldBe 3
                }

                // Turn face-up via morph cost ({1}{R})
                val turnUpResult = game.execute(
                    com.wingedsheep.engine.core.TurnFaceUp(game.player1Id, faceDownId)
                )
                withClue("Turn face up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Should now be a 3/1 Battering Craghorn (no more +1/+1)
                val projectedAfter = stateProjector.project(game.state)
                withClue("Battering Craghorn should be 3/1 after turning face up") {
                    projectedAfter.getPower(faceDownId) shouldBe 3
                    projectedAfter.getToughness(faceDownId) shouldBe 1
                }

                withClue("Should be face-up Battering Craghorn") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
                    game.state.getEntity(faceDownId)?.get<CardComponent>()?.name shouldBe "Battering Craghorn"
                }
            }

            test("affects opponent's face-down creatures too") {
                val game = scenario()
                    .withPlayers("Ixidor Player", "Opponent")
                    .withCardOnBattlefield(1, "Ixidor, Reality Sculptor")
                    .withCardInHand(2, "Whipcorder")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Whipcorder face-down
                val whipcorderCardId = game.state.getHand(game.player2Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                game.execute(CastSpell(game.player2Id, whipcorderCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Opponent's face-down creature should also be 3/3
                val projected = stateProjector.project(game.state)
                withClue("Opponent's face-down creature should also get +1/+1 from Ixidor") {
                    projected.getPower(faceDownId) shouldBe 3
                    projected.getToughness(faceDownId) shouldBe 3
                }
            }

            test("does not affect face-up creatures") {
                val game = scenario()
                    .withPlayers("Ixidor Player", "Opponent")
                    .withCardOnBattlefield(1, "Ixidor, Reality Sculptor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Grizzly Bears should still be 2/2 — Ixidor only buffs face-down creatures
                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should not get +1/+1 from Ixidor") {
                    projected.getPower(bearsId) shouldBe 2
                    projected.getToughness(bearsId) shouldBe 2
                }
            }
        }

        context("Ixidor's activated ability — turn face-down creature face up") {

            test("turns target face-down creature face up") {
                val game = scenario()
                    .withPlayers("Ixidor Player", "Opponent")
                    .withCardOnBattlefield(1, "Ixidor, Reality Sculptor")
                    .withCardInHand(1, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Island", 6) // 3 for morph + 3 for ability
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Battering Craghorn face-down
                val craghornCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                game.execute(CastSpell(game.player1Id, craghornCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Activate Ixidor's ability: {2}{U}: Turn target face-down creature face up
                val ixidorId = game.findPermanent("Ixidor, Reality Sculptor")!!
                val cardDef = cardRegistry.getCard("Ixidor, Reality Sculptor")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ixidorId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(faceDownId))
                    )
                )
                withClue("Activate ability should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                // Creature should now be face-up Battering Craghorn
                withClue("Creature should be face-up") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
                }
                withClue("Creature should be Battering Craghorn") {
                    game.state.getEntity(faceDownId)?.get<CardComponent>()?.name shouldBe "Battering Craghorn"
                }
            }

            test("can turn opponent's face-down creature face up") {
                val game = scenario()
                    .withPlayers("Ixidor Player", "Opponent")
                    .withCardOnBattlefield(1, "Ixidor, Reality Sculptor")
                    .withCardInHand(2, "Whipcorder")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Whipcorder face-down
                val whipcorderCardId = game.state.getHand(game.player2Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                game.execute(CastSpell(game.player2Id, whipcorderCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Switch to Ixidor player's turn
                game.state = game.state.copy(
                    activePlayerId = game.player1Id,
                    priorityPlayerId = game.player1Id
                )

                // Activate Ixidor's ability targeting opponent's face-down creature
                val ixidorId = game.findPermanent("Ixidor, Reality Sculptor")!!
                val cardDef = cardRegistry.getCard("Ixidor, Reality Sculptor")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ixidorId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(faceDownId))
                    )
                )
                withClue("Activate ability should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }
                game.resolveStack()

                // Creature should now be face-up Whipcorder
                withClue("Creature should be face-up") {
                    game.state.getEntity(faceDownId)?.has<FaceDownComponent>() shouldBe false
                }
                withClue("Creature should be Whipcorder") {
                    game.state.getEntity(faceDownId)?.get<CardComponent>()?.name shouldBe "Whipcorder"
                }
            }

            test("cannot target a face-up creature") {
                val game = scenario()
                    .withPlayers("Ixidor Player", "Opponent")
                    .withCardOnBattlefield(1, "Ixidor, Reality Sculptor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ixidorId = game.findPermanent("Ixidor, Reality Sculptor")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val cardDef = cardRegistry.getCard("Ixidor, Reality Sculptor")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to activate targeting a face-up creature — should fail
                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ixidorId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )
                withClue("Should not be able to target a face-up creature") {
                    activateResult.error shouldNotBe null
                }
            }
        }
    }
}
