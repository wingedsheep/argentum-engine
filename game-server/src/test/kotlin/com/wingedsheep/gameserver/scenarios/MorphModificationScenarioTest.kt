package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for modification effects on face-down morph creatures.
 *
 * Per MTG rules, face-down creatures are 2/2 colorless creatures that can be
 * targeted and affected by spells and effects like any other creature.
 * Stat modifications (like +2/+4 from Inspirit) should apply on top of the
 * base 2/2 stats.
 *
 * Cards used:
 * - Whipcorder ({W}{W}, 2/2 Creature — Human Soldier Rebel, Morph {W})
 * - Inspirit ({2}{W}, Instant, "Untap target creature. It gets +2/+4 until end of turn.")
 * - Steadfastness ({1}{W}, Sorcery, "Creatures you control get +0/+3 until end of turn.")
 * - Grizzly Bears (2/2, green creature — used as a baseline)
 */
class MorphModificationScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Targeted modification effects on face-down morph creatures") {

            test("Inspirit gives +2/+4 to face-down morph creature") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withCardInHand(1, "Inspirit")
                    .withLandsOnBattlefield(1, "Plains", 7) // 3 for morph + 3 for Inspirit + 1 extra
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Whipcorder face-down for {3}
                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val castMorphResult = game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castMorphResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Verify base projected stats are 2/2
                val projectedBefore = stateProjector.project(game.state)
                withClue("Face-down creature should be 2/2 before modification") {
                    projectedBefore.getPower(faceDownId!!) shouldBe 2
                    projectedBefore.getToughness(faceDownId) shouldBe 2
                }

                // Cast Inspirit targeting the face-down creature
                val castInspiritResult = game.castSpell(1, "Inspirit", faceDownId!!)
                withClue("Inspirit should target face-down creature successfully: ${castInspiritResult.error}") {
                    castInspiritResult.error shouldBe null
                }
                game.resolveStack()

                // Verify projected stats are now 4/6 (2+2/2+4)
                val projectedAfter = stateProjector.project(game.state)
                withClue("Face-down creature should be 4/6 after Inspirit (+2/+4)") {
                    projectedAfter.getPower(faceDownId) shouldBe 4
                    projectedAfter.getToughness(faceDownId) shouldBe 6
                }

                // Verify client state also shows correct stats
                val clientState = game.getClientState(1)
                val faceDownCard = clientState.cards[faceDownId]
                withClue("Client state should show correct modified stats for face-down creature") {
                    faceDownCard shouldNotBe null
                    faceDownCard!!.power shouldBe 4
                    faceDownCard.toughness shouldBe 6
                }
            }
        }

        context("Group modification effects on face-down morph creatures") {

            test("Steadfastness gives +0/+3 to face-down morph creature") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withCardInHand(1, "Steadfastness")
                    .withLandsOnBattlefield(1, "Plains", 6) // 3 for morph + 2 for Steadfastness + 1 extra
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Whipcorder face-down for {3}
                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val castMorphResult = game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castMorphResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Cast Steadfastness (group pump: creatures you control get +0/+3)
                val castSteadfastnessResult = game.castSpell(1, "Steadfastness")
                withClue("Steadfastness should be cast successfully: ${castSteadfastnessResult.error}") {
                    castSteadfastnessResult.error shouldBe null
                }
                game.resolveStack()

                // Verify projected stats are now 2/5 (2+0/2+3)
                val projectedAfter = stateProjector.project(game.state)
                withClue("Face-down creature should be 2/5 after Steadfastness (+0/+3)") {
                    projectedAfter.getPower(faceDownId!!) shouldBe 2
                    projectedAfter.getToughness(faceDownId) shouldBe 5
                }

                // Verify client state shows correct stats
                val clientState = game.getClientState(1)
                val faceDownCard = clientState.cards[faceDownId]
                withClue("Client state should show correct modified stats") {
                    faceDownCard shouldNotBe null
                    faceDownCard!!.power shouldBe 2
                    faceDownCard.toughness shouldBe 5
                }
            }

            test("Steadfastness affects both face-down and face-up creatures") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withCardInHand(1, "Steadfastness")
                    .withCardOnBattlefield(1, "Grizzly Bears") // Normal 2/2 creature
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Whipcorder face-down
                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Cast Steadfastness
                game.castSpell(1, "Steadfastness")
                game.resolveStack()

                val projected = stateProjector.project(game.state)

                // Face-down creature: 2/5 (2+0/2+3)
                withClue("Face-down creature should be 2/5") {
                    projected.getPower(faceDownId) shouldBe 2
                    projected.getToughness(faceDownId) shouldBe 5
                }

                // Grizzly Bears: 2/5 (2+0/2+3)
                withClue("Grizzly Bears should be 2/5") {
                    projected.getPower(bearsId) shouldBe 2
                    projected.getToughness(bearsId) shouldBe 5
                }
            }
        }

        context("Opponent view of modified face-down creatures") {

            test("opponent sees modified stats on face-down creature") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withCardInHand(1, "Inspirit")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down and buff it
                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                game.castSpell(1, "Inspirit", faceDownId)
                game.resolveStack()

                // Opponent should see modified stats (4/6), not base 2/2
                val opponentState = game.getClientState(2)
                val faceDownCard = opponentState.cards[faceDownId]
                withClue("Opponent should see modified face-down creature stats") {
                    faceDownCard shouldNotBe null
                    faceDownCard!!.power shouldBe 4
                    faceDownCard.toughness shouldBe 6
                }

                // But opponent should NOT see the card's real name
                withClue("Opponent should not see real card info") {
                    faceDownCard!!.name shouldBe "Face-down creature"
                }
            }
        }
    }
}
