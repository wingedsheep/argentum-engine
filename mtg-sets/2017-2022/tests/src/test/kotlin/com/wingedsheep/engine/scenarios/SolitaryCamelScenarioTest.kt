package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Solitary Camel (HOU #23) — {2}{W} Creature — Camel 3/2.
 *
 *   This creature has lifelink as long as you control a Desert or there is a Desert card in your
 *   graveyard.
 */
class SolitaryCamelScenarioTest : ScenarioTestBase() {

    private val name = "Solitary Camel"
    private val desert = "Desert of the Fervent"

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(1, name)

        test("a Desert you control gives it lifelink, so its combat damage gains life") {
            val game = board().withCardOnBattlefield(1, desert).build()
            val camel = game.findPermanent(name)!!
            game.state.projectedState.hasKeyword(camel, Keyword.LIFELINK) shouldBe true
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(name to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
        }

        test("a Desert card in your graveyard gives it lifelink") {
            val game = board().withCardInGraveyard(1, desert).build()
            val camel = game.findPermanent(name)!!
            game.state.projectedState.hasKeyword(camel, Keyword.LIFELINK) shouldBe true
        }

        test("without a Desert of your own it has no lifelink") {
            val game = board()
                .withCardOnBattlefield(1, "Plains")
                .withCardInGraveyard(1, "Plains")
                .withCardOnBattlefield(2, desert)
                .withCardInGraveyard(2, desert)
                .build()
            val camel = game.findPermanent(name)!!
            withClue("an opponent's Desert, on the battlefield or in their graveyard, doesn't count") {
                game.state.projectedState.hasKeyword(camel, Keyword.LIFELINK) shouldBe false
            }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(name to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
