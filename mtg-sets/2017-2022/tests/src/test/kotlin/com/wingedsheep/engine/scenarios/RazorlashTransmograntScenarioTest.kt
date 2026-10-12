package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Razorlash Transmogrant (BRO #122).
 *
 *   {4}{B}{B}: Return this card from your graveyard to the battlefield with a +1/+1 counter on it.
 *   This ability costs {4} less to activate if an opponent controls four or more nonbasic lands.
 *
 * Exercises the conditional reduction on an ability activated from the graveyard: with only
 * {B}{B} available the return is offered exactly when an opponent has four nonbasic lands.
 */
class RazorlashTransmograntScenarioTest : ScenarioTestBase() {

    private fun activate(opponentNonbasics: Int): Pair<TestGame, String?> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInGraveyard(1, "Razorlash Transmogrant")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(2, "Evolving Wilds", opponentNonbasics)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val razorlash = game.findCardsInGraveyard(1, "Razorlash Transmogrant").single()
        val abilityId = cardRegistry.getCard("Razorlash Transmogrant")!!.activatedAbilities.first().id
        val result = game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = razorlash, abilityId = abilityId)
        )
        return game to result.error
    }

    init {
        context("Razorlash Transmogrant") {

            test("an opponent with four nonbasic lands discounts the return to {B}{B}") {
                val (game, error) = activate(opponentNonbasics = 4)
                withClue("activation should succeed for {B}{B}: $error") { error shouldBe null }
                game.resolveStack()

                val razorlash = game.findPermanent("Razorlash Transmogrant")
                withClue("Razorlash returned to the battlefield") { razorlash shouldNotBe null }
                withClue("it returned with a +1/+1 counter") {
                    game.state.getEntity(razorlash!!)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("three nonbasic lands leave the full {4}{B}{B} cost") {
                val (game, error) = activate(opponentNonbasics = 3)
                withClue("{B}{B} can't pay the unreduced cost") { error shouldNotBe null }
                game.isInGraveyard(1, "Razorlash Transmogrant") shouldBe true
            }
        }
    }
}
