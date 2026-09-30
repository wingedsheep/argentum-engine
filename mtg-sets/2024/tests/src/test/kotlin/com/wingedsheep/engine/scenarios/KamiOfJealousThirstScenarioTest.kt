package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.KamiOfJealousThirst
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kami of Jealous Thirst (MH3) — "{4}{B}: Each opponent loses 2 life and you gain 2 life. This
 * ability costs {4}{B} less to activate if you've drawn three or more cards this turn. Activate
 * only once each turn."
 *
 * The reduction is the whole cost, colored pip included — the generic-only rail would leave {B}
 * behind — so after three draws the ability is free. The threshold counts *your* draws.
 */
class KamiOfJealousThirstScenarioTest : ScenarioTestBase() {

    private val abilityId = KamiOfJealousThirst.activatedAbilities.first().id

    private fun board(swamps: Int, yourDraws: Int = 0, opponentDraws: Int = 0): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Kami of Jealous Thirst")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (swamps > 0) builder.withLandsOnBattlefield(1, "Swamp", swamps)
        if (yourDraws > 0) builder.withCardsDrawnThisTurn(1, yourDraws)
        if (opponentDraws > 0) builder.withCardsDrawnThisTurn(2, opponentDraws)
        return builder.build()
    }

    private fun TestGame.activate() = execute(
        ActivateAbility(player1Id, findPermanent("Kami of Jealous Thirst")!!, abilityId)
    )

    init {
        context("Kami of Jealous Thirst — {4}{B} drain, free after three draws") {

            test("costs {4}{B} with no draws: five Swamps pay it and the drain resolves") {
                val game = board(swamps = 5)
                game.activate().error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 22
                game.getLifeTotal(2) shouldBe 18
                withClue("all five Swamps were tapped for the full cost") {
                    game.findPermanents("Swamp").count { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 5
                }
            }

            test("two draws are not enough: with no mana the ability can't be activated") {
                val game = board(swamps = 0, yourDraws = 2)
                game.activate().error shouldNotBe null
                game.getLifeTotal(2) shouldBe 20
            }

            test("after three draws the ability costs {0} — the {B} is reduced too") {
                val game = board(swamps = 0, yourDraws = 3)
                game.activate().error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 22
                game.getLifeTotal(2) shouldBe 18
            }

            test("the free activation is still limited to once each turn") {
                val game = board(swamps = 0, yourDraws = 4)
                game.activate().error shouldBe null
                game.resolveStack()

                game.activate().error shouldNotBe null
                game.getLifeTotal(2) shouldBe 18
            }

            test("the opponent's draws don't count toward the threshold") {
                val game = board(swamps = 0, opponentDraws = 3)
                game.activate().error shouldNotBe null
                game.getLifeTotal(2) shouldBe 20
            }

            test("the legal-action menu offers the reduced ability as affordable with no lands") {
                val game = board(swamps = 0, yourDraws = 3)
                val offered = game.getLegalActions(1).single {
                    (it.action as? ActivateAbility)?.abilityId == abilityId
                }
                offered.isAffordable shouldBe true
            }
        }
    }
}
