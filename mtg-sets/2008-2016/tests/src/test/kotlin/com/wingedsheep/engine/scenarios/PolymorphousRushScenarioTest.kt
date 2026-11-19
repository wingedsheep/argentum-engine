package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Polymorphous Rush (JOU #46) — {2}{U} Instant.
 *
 * "Strive — This spell costs {1}{U} more to cast for each target beyond the first.
 *  Choose a creature on the battlefield. Any number of target creatures you control each become a
 *  copy of that creature until end of turn."
 *
 * Pins the strive tax ({3}{U}{U} for two targets), that the copied creature is chosen on resolution
 * (an opponent's creature, not a target), that every target copies the same chosen creature, and
 * that the copies revert at end of turn.
 */
class PolymorphousRushScenarioTest : ScenarioTestBase() {

    private fun TestGame.castRush(targets: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Polymorphous Rush"
            },
            targets = targets.map { ChosenTarget.Permanent(it) },
        )
    )

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    init {
        context("Polymorphous Rush") {

            test("two targets each become a copy of a chosen opposing creature until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Polymorphous Rush")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val angel = game.findPermanent("Serra Angel")!!

                val cast = game.castRush(listOf(bears, lions))
                withClue("two targets cost {3}{U}{U}, payable with five Islands: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("the creature to copy is chosen as the spell resolves") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(angel)).error shouldBe null

                val projected = game.state.projectedState
                for (id in listOf(bears, lions)) {
                    withClue("each target is now a 4/4 flying Serra Angel") {
                        game.nameOf(id) shouldBe "Serra Angel"
                        projected.getPower(id) shouldBe 4
                        projected.getToughness(id) shouldBe 4
                        projected.hasKeyword(id, Keyword.FLYING) shouldBe true
                        projected.getController(id) shouldBe game.player1Id
                    }
                }
                withClue("the chosen creature itself is untouched") {
                    game.nameOf(angel) shouldBe "Serra Angel"
                    game.state.projectedState.getController(angel) shouldBe game.player2Id
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the copies end at end of turn") {
                    game.nameOf(bears) shouldBe "Grizzly Bears"
                    game.nameOf(lions) shouldBe "Savannah Lions"
                }
            }

            test("a second target is rejected when only four mana is available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Polymorphous Rush")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castRush(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets cost five mana; four Islands can't pay it") { cast.error.shouldNotBeNull() }
                withClue("a rejected cast leaves the spell in hand") { game.handSize(1) shouldBe 1 }
            }
        }
    }
}
