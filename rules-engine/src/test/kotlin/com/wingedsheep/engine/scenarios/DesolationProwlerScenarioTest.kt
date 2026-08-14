package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Desolation Prowler (HOB #64) — {1}{B} Creature — Wolf 2/2.
 * "Pay 2 life: This creature gets +2/+2 until end of turn. Activate only once each turn."
 *
 * Three things have to hold: the cost is life (not mana), the pump lands on itself, and the
 * once-each-turn restriction actually blocks the second activation.
 */
class DesolationProwlerScenarioTest : ScenarioTestBase() {

    init {
        context("Desolation Prowler") {

            test("paying 2 life pumps it to 4/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desolation Prowler")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val prowler = game.findPermanent("Desolation Prowler")!!
                val pump = cardRegistry.requireCard("Desolation Prowler").activatedAbilities.single().id

                game.state.projectedState.getPower(prowler) shouldBe 2
                game.getLifeTotal(1) shouldBe 20

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = prowler, abilityId = pump)
                ).error shouldBe null
                game.resolveStack()

                withClue("the cost is 2 life — no mana involved") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("+2/+2 on itself") {
                    game.state.projectedState.getPower(prowler) shouldBe 4
                    game.state.projectedState.getToughness(prowler) shouldBe 4
                }
            }

            test("it cannot be activated a second time in the same turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desolation Prowler")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val prowler = game.findPermanent("Desolation Prowler")!!
                val pump = cardRegistry.requireCard("Desolation Prowler").activatedAbilities.single().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = prowler, abilityId = pump)
                ).error shouldBe null
                game.resolveStack()

                val second = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = prowler, abilityId = pump)
                )
                withClue("'Activate only once each turn' rejects the second activation") {
                    second.error shouldNotBe null
                }
                withClue("no second life payment and no second pump") {
                    game.getLifeTotal(1) shouldBe 18
                    game.state.projectedState.getPower(prowler) shouldBe 4
                }
            }
        }
    }
}
