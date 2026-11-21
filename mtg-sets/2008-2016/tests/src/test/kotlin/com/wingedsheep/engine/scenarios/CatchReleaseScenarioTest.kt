package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Catch // Release (DGM #125) — fuse split sorcery.
 *
 *   Catch {1}{U}{R}: Gain control of target permanent until end of turn. Untap it. It gains haste.
 *   Release {4}{R}{W}: Each player sacrifices an artifact, a creature, an enchantment, a land, and a
 *   planeswalker of their choice.
 *
 * Release's ruling is the point: one permanent answers one type only, and each player must give up
 * as many permanents as their board allows — an artifact creature can't be "spent" as the artifact
 * when it is the only creature.
 */
class CatchReleaseScenarioTest : ScenarioTestBase() {

    init {
        val p1 = EntityId.of("player-1")
        val p2 = EntityId.of("player-2")

        fun TestGame.splitCard(): EntityId = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Catch // Release"
        }

        fun TestGame.permanentOf(name: String, controller: EntityId): EntityId =
            state.getBattlefield().first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == name &&
                    state.projectedState.getController(id) == controller
            }

        context("Release") {
            test("each player loses one distinct permanent per type, as many as possible, all at once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Catch // Release")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Sol Ring")
                    .withCardOnBattlefield(1, "Mind Stone")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myOrnithopter = game.permanentOf("Ornithopter", p1)
                val solRing = game.permanentOf("Sol Ring", p1)
                val mindStone = game.permanentOf("Mind Stone", p1)

                game.execute(CastSpell(game.player1Id, game.splitCard(), faceIndex = 1)).error shouldBe null
                game.resolveStack()

                withClue("the active player picks first, and the only creature isn't offered as the artifact") {
                    val decision = game.getPendingDecision() as SelectCardsDecision
                    decision.playerId shouldBe p1
                    decision.prompt shouldContain "artifact to sacrifice"
                    decision.options shouldContainExactlyInAnyOrder listOf(solRing, mindStone)
                }
                game.selectCards(listOf(solRing)).error shouldBe null
                game.resolveStack()

                withClue("the creature resolves itself; the land is a real choice") {
                    val decision = game.getPendingDecision() as SelectCardsDecision
                    decision.playerId shouldBe p1
                    decision.prompt shouldContain "land to sacrifice"
                    decision.options.size shouldBe 6
                }
                withClue("nothing is sacrificed until every pick is in") {
                    game.state.getBattlefield().contains(solRing) shouldBe true
                    game.state.getBattlefield().contains(myOrnithopter) shouldBe true
                }
                val land = (game.getPendingDecision() as SelectCardsDecision).options.first()
                game.selectCards(listOf(land)).error shouldBe null
                game.resolveStack()

                withClue("the opponent's lone artifact creature answers only one type, so nothing else is asked") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("the caster lost Sol Ring, Ornithopter and a land; Mind Stone stays") {
                    game.isInGraveyard(1, "Sol Ring") shouldBe true
                    game.isInGraveyard(1, "Ornithopter") shouldBe true
                    game.state.getBattlefield().contains(land) shouldBe false
                    game.state.getBattlefield().contains(mindStone) shouldBe true
                }
                withClue("the opponent lost their Ornithopter and their Island") {
                    game.isInGraveyard(2, "Ornithopter") shouldBe true
                    game.isInGraveyard(2, "Island") shouldBe true
                }
            }
        }

        context("Catch // Release fused") {
            test("Catch resolves first, so the stolen creature can be sacrificed as the caster's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Catch // Release")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.permanentOf("Hill Giant", p2)
                game.execute(
                    CastSpell(
                        game.player1Id, game.splitCard(),
                        targets = listOf(ChosenTarget.Permanent(giant)), castFused = true
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the caster now controls the giant, so their only question is the land") {
                    game.state.projectedState.getController(giant) shouldBe p1
                    val decision = game.getPendingDecision() as SelectCardsDecision
                    decision.playerId shouldBe p1
                    decision.prompt shouldContain "land to sacrifice"
                }
                val land = (game.getPendingDecision() as SelectCardsDecision).options.first()
                game.selectCards(listOf(land)).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Forest") shouldBe true
            }
        }

        context("Catch") {
            test("steals any permanent until end of turn, untapped and hasty") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Catch // Release")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.permanentOf("Hill Giant", p2)
                game.execute(
                    CastSpell(game.player1Id, game.splitCard(), faceIndex = 0, targets = listOf(ChosenTarget.Permanent(giant)))
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getController(giant) shouldBe p1
                game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe true
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
