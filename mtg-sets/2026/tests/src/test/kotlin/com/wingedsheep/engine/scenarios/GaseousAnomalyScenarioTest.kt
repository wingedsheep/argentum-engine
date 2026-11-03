package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gaseous Anomaly: "{5}{U}, {T}, Sacrifice this land: Draw two cards. Create a tapped Planet land
 * token named New Planet with '{T}: Add one mana of any color.'"
 */
class GaseousAnomalyScenarioTest : ScenarioTestBase() {

    private fun abilityId() = cardRegistry.getCard("Gaseous Anomaly")!!.script.activatedAbilities[1].id

    init {
        context("Gaseous Anomaly") {
            test("sacrificing it draws two cards and leaves a tapped New Planet behind") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gaseous Anomaly")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val anomaly = game.findPermanent("Gaseous Anomaly")!!
                val handBefore = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, anomaly, abilityId())).error shouldBe null
                withClue("the land is sacrificed as a cost") {
                    game.findPermanents("Gaseous Anomaly").size shouldBe 0
                }
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 2
                val planets = game.findPermanents("New Planet")
                planets.size shouldBe 1
                game.state.getEntity(planets.single())!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
