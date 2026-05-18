package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Builder's Bane (Mirage).
 *
 * Card:
 *   {X}{X}{R}  Sorcery
 *   Destroy X target artifacts. Builder's Bane deals damage to each player equal to the
 *   number of artifacts they controlled that were put into a graveyard this way.
 *
 * Exercises [com.wingedsheep.sdk.scripting.effects.DestroyTargetsAndDamageControllersEffect]:
 *  - per-controller damage tally
 *  - indestructible targets drop out of the damage count ("put into a graveyard this way")
 *  - X=0 cast destroys nothing and deals no damage
 */
class BuildersBaneScenarioTest : ScenarioTestBase() {

    private val testArtifact = CardDefinition.artifact(
        name = "Test Artifact",
        manaCost = ManaCost.parse("{2}")
    )

    private val indestructibleArtifact = CardDefinition(
        name = "Test Indestructible Artifact",
        manaCost = ManaCost.parse("{2}"),
        typeLine = TypeLine(cardTypes = setOf(CardType.ARTIFACT)),
        keywords = setOf(Keyword.INDESTRUCTIBLE)
    )

    init {
        cardRegistry.register(testArtifact)
        cardRegistry.register(indestructibleArtifact)

        context("Builder's Bane") {

            test("destroys two artifacts split across controllers and deals 1 damage to each") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Builder's Bane")
                    .withLandsOnBattlefield(1, "Mountain", 5) // X=2 → {2}{2}{R} = 5 mana
                    .withCardOnBattlefield(1, "Test Artifact")
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownArtifact = game.state.getBattlefield(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Artifact" }
                val oppArtifact = game.state.getBattlefield(game.player2Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Artifact" }

                val baneId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Builder's Bane"
                }

                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = baneId,
                        targets = listOf(
                            ChosenTarget.Permanent(ownArtifact),
                            ChosenTarget.Permanent(oppArtifact)
                        ),
                        xValue = 2
                    )
                )
                withClue("Cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Test Artifact") shouldBe false
                withClue("Each controller lost exactly one artifact, so each takes 1 damage") {
                    game.getLifeTotal(1) shouldBe 19
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("controller with two killed artifacts takes 2 damage") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Builder's Bane")
                    .withLandsOnBattlefield(1, "Mountain", 5) // X=2
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oppArtifacts = game.state.getBattlefield(game.player2Id)
                    .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Artifact" }
                oppArtifacts.size shouldBe 2

                val baneId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Builder's Bane"
                }
                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = baneId,
                        targets = oppArtifacts.map { ChosenTarget.Permanent(it) },
                        xValue = 2
                    )
                )
                withClue("Cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Both of opponent's artifacts destroyed → 2 damage to opponent, 0 to caster") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("indestructible target is not counted toward damage") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Builder's Bane")
                    .withLandsOnBattlefield(1, "Mountain", 5) // X=2
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withCardOnBattlefield(2, "Test Indestructible Artifact")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val destructible = game.findPermanent("Test Artifact")!!
                val indestructible = game.findPermanent("Test Indestructible Artifact")!!

                val baneId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Builder's Bane"
                }
                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = baneId,
                        targets = listOf(
                            ChosenTarget.Permanent(destructible),
                            ChosenTarget.Permanent(indestructible)
                        ),
                        xValue = 2
                    )
                )
                withClue("Cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Destructible artifact dies; indestructible one stays") {
                    game.isInGraveyard(2, "Test Artifact") shouldBe true
                    game.isOnBattlefield("Test Indestructible Artifact") shouldBe true
                }
                withClue("Only the destructible artifact contributes to damage → opponent takes 1") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("rejects a cast that picks more targets than X") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Builder's Bane")
                    .withLandsOnBattlefield(1, "Mountain", 5) // X=2
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artifacts = game.state.getBattlefield(game.player2Id)
                    .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Artifact" }
                artifacts.size shouldBe 3

                val baneId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Builder's Bane"
                }
                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = baneId,
                        targets = artifacts.map { ChosenTarget.Permanent(it) }, // 3 targets, X=2
                        xValue = 2
                    )
                )
                withClue("Engine should reject 3 targets when X=2") {
                    result.error shouldNotBe null
                }
                withClue("All artifacts untouched after rejected cast") {
                    game.state.getBattlefield(game.player2Id)
                        .count { game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Artifact" } shouldBe 3
                }
            }

            test("X=0 resolves with no targets, no damage, and no destruction") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Builder's Bane")
                    .withLandsOnBattlefield(1, "Mountain", 1) // just {R}
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val baneId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Builder's Bane"
                }
                val result = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = baneId,
                        targets = emptyList(),
                        xValue = 0
                    )
                )
                withClue("X=0 cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("No targets → artifact survives and nobody takes damage") {
                    game.isOnBattlefield("Test Artifact") shouldBe true
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
