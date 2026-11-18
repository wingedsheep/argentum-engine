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
 * Twinflame (JOU #115) — {1}{R} Sorcery.
 *
 * "Strive — This spell costs {2}{R} more to cast for each target beyond the first.
 *  Choose any number of target creatures you control. For each of them, create a token that's a
 *  copy of that creature, except it has haste. Exile those tokens at the beginning of the next end
 *  step."
 */
class TwinflameScenarioTest : ScenarioTestBase() {

    private fun TestGame.castTwinflame(targets: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Twinflame"
            },
            targets = targets.map { ChosenTarget.Permanent(it) },
        )
    )

    init {
        context("Twinflame") {

            test("two targets on {3}{R}{R} make two hasty copies, exiled at the next end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twinflame")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val cast = game.castTwinflame(listOf(bears, lions))
                withClue("{1}{R} + {2}{R} is payable with five Mountains: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val bearsToken = game.findPermanents("Grizzly Bears").single { it != bears }
                val lionsToken = game.findPermanents("Savannah Lions").single { it != lions }
                for (token in listOf(bearsToken, lionsToken)) {
                    withClue("each copy is controlled by the caster and has haste") {
                        game.state.projectedState.getController(token) shouldBe game.player1Id
                        game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
                    }
                }
                withClue("the originals don't gain haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("both tokens are exiled at the end step; only the originals remain") {
                    game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
                    game.findPermanents("Savannah Lions") shouldBe listOf(lions)
                }
            }

            test("a second target is rejected when the strive tax can't be paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twinflame")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castTwinflame(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets cost {3}{R}{R}; four lands can't pay it") { cast.error.shouldNotBeNull() }
                withClue("a rejected cast leaves the spell in hand") { game.handSize(1) shouldBe 1 }
            }

            test("an opponent's creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twinflame")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castTwinflame(listOf(game.findPermanent("Grizzly Bears")!!))
                withClue("\"target creatures you control\"") { cast.error.shouldNotBeNull() }
            }
        }
    }
}
