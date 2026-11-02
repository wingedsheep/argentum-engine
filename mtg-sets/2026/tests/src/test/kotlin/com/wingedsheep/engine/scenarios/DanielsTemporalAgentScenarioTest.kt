package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Daniels, Temporal Agent: the enters trigger returns a creature card from your graveyard, and
 * from the graveyard "{2}, Put this card from your graveyard on the bottom of your library: You
 * gain 2 life" — the card moves to the bottom of the library as a cost, before the ability resolves.
 */
class DanielsTemporalAgentScenarioTest : ScenarioTestBase() {

    private fun abilityId() = cardRegistry.getCard("Daniels, Temporal Agent")!!.script.activatedAbilities[0].id

    init {
        context("Daniels, Temporal Agent") {
            test("enters trigger returns a creature card from your graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Daniels, Temporal Agent")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Daniels, Temporal Agent").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(game.findCardsInGraveyard(1, "Grizzly Bears")).error shouldBe null
                }
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
            }

            test("from the graveyard: pay {2} and put it on the bottom of the library to gain 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Daniels, Temporal Agent")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val daniels = game.findCardsInGraveyard(1, "Daniels, Temporal Agent").single()
                withClue("the ability is offered from the graveyard") {
                    game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == daniels } shouldBe true
                }

                game.execute(ActivateAbility(game.player1Id, daniels, abilityId())).error shouldBe null

                withClue("the cost moved Daniels to the bottom of the library before resolution") {
                    game.isInGraveyard(1, "Daniels, Temporal Agent") shouldBe false
                    game.state.getLibrary(game.player1Id).last() shouldBe daniels
                    game.librarySize(1) shouldBe 3
                    game.getLifeTotal(1) shouldBe 20
                }

                game.resolveStack()
                game.getLifeTotal(1) shouldBe 22
            }

            test("the ability can't be activated while Daniels is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Daniels, Temporal Agent")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val daniels = game.findPermanent("Daniels, Temporal Agent")
                daniels shouldNotBe null
                game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == daniels } shouldBe false
                game.execute(ActivateAbility(game.player1Id, daniels!!, abilityId())).error shouldNotBe null
            }
        }
    }
}
