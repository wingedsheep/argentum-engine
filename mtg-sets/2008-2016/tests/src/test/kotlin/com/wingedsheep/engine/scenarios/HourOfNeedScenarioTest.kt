package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Hour of Need (JOU #40) — {2}{U} Instant.
 *
 * "Strive — This spell costs {1}{U} more to cast for each target beyond the first.
 *  Exile any number of target creatures. For each creature exiled this way, its controller creates
 *  a 4/4 blue Sphinx creature token with flying."
 *
 * Pins the strive tax ({3}{U}{U} for two targets) and that each exiled creature's *controller* —
 * not the caster — gets its own 4/4 flying Sphinx.
 */
class HourOfNeedScenarioTest : ScenarioTestBase() {

    private fun TestGame.castHourOfNeed(targets: List<EntityId>) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Hour of Need"
            },
            targets = targets.map { ChosenTarget.Permanent(it) },
        )
    )

    private fun TestGame.tokensControlledBy(playerId: EntityId): List<EntityId> =
        state.getBattlefield().filter { id ->
            state.getEntity(id)?.has<TokenComponent>() == true &&
                state.projectedState.getController(id) == playerId
        }

    init {
        context("Hour of Need") {

            test("two targets with different controllers: each controller gets a 4/4 flying Sphinx") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hour of Need")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castHourOfNeed(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets cost {3}{U}{U}, payable with five Islands: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("both targeted creatures are exiled") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Savannah Lions") shouldBe true
                }

                val mine = game.tokensControlledBy(game.player1Id)
                val theirs = game.tokensControlledBy(game.player2Id)
                withClue("each exiled creature's controller creates exactly one token") {
                    mine shouldHaveSize 1
                    theirs shouldHaveSize 1
                }
                for (token in mine + theirs) {
                    val projected = game.state.projectedState
                    withClue("the token is a 4/4 blue Sphinx with flying") {
                        projected.getPower(token) shouldBe 4
                        projected.getToughness(token) shouldBe 4
                        projected.hasKeyword(token, Keyword.FLYING) shouldBe true
                        projected.getColors(token) shouldBe setOf(Color.BLUE.name)
                        projected.getSubtypes(token).contains("Sphinx") shouldBe true
                    }
                }
            }

            test("a stolen creature's Sphinx goes to its controller, not its owner") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Act of Treason")
                    .withCardInHand(1, "Hour of Need")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Act of Treason", targetId = bears).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getController(bears) shouldBe game.player1Id

                game.castHourOfNeed(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the Bears are exiled to their owner's exile") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("the player who controlled the Bears gets the Sphinx; the owner gets none") {
                    game.tokensControlledBy(game.player1Id) shouldHaveSize 1
                    game.tokensControlledBy(game.player2Id) shouldHaveSize 0
                }
            }

            test("a second target is rejected when only four mana is available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hour of Need")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castHourOfNeed(
                    listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Savannah Lions")!!)
                )
                withClue("two targets cost five mana; four Islands can't pay it") { cast.error.shouldNotBeNull() }
                withClue("a rejected cast leaves the spell in hand") { game.handSize(1) shouldBe 1 }
            }
        }
    }
}
