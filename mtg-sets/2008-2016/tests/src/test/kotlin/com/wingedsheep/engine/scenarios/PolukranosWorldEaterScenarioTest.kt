package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Polukranos, World Eater: "{X}{X}{G}: Monstrosity X. When Polukranos becomes monstrous, it deals X
 * damage divided as you choose among any number of target creatures your opponents control. Each of
 * those creatures deals damage equal to its power to Polukranos."
 *
 * The trigger's X is the monstrosity X, the division is announced as the trigger goes on the stack,
 * and every target hits back — including one Polukranos just dealt lethal damage, since state-based
 * actions wait until the ability has finished resolving. A creature that wasn't targeted deals nothing.
 */
class PolukranosWorldEaterScenarioTest : ScenarioTestBase() {

    private fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.activateMonstrosity(x: Int) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Polukranos, World Eater") ?: error("Polukranos is not on the battlefield"),
            abilityId = cardRegistry.getCard("Polukranos, World Eater")!!.activatedAbilities[0].id,
            xValue = x,
            paymentStrategy = PaymentStrategy.AutoPay,
        )
    )

    init {
        test("monstrosity 3 divides 3 damage among the targets, and each target hits back for its power") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Polukranos, World Eater")
                .withLandsOnBattlefield(1, "Forest", 7)
                .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                .withCardOnBattlefield(2, "Hill Giant")    // 3/3
                .withCardOnBattlefield(2, "Craw Wurm")     // 6/4, not targeted
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val polukranos = game.findPermanent("Polukranos, World Eater")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.activateMonstrosity(3).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldNotBeNull().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears, giant)).error shouldBe null

            val division = game.getPendingDecision().shouldNotBeNull().shouldBeInstanceOf<DistributeDecision>()
            withClue("the trigger divides the monstrosity X") {
                division.totalAmount shouldBe 3
            }
            game.submitDistribution(mapOf(bears to 2, giant to 1)).error shouldBe null
            game.resolveStack()

            withClue("Polukranos is an 8/8 after three +1/+1 counters") {
                game.state.projectedState.getPower(polukranos) shouldBe 8
            }
            withClue("the Bears (lethally damaged) and the Giant both dealt damage equal to their power; the Wurm dealt none") {
                game.damageOn(polukranos) shouldBe 5
            }
            game.findPermanent("Grizzly Bears") shouldBe null
            game.damageOn(giant) shouldBe 1
            game.findPermanent("Craw Wurm").shouldNotBeNull()
        }

        test("a single target takes all X damage and still hits back") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Polukranos, World Eater")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val polukranos = game.findPermanent("Polukranos, World Eater")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.activateMonstrosity(2).error shouldBe null
            game.resolveStack()

            game.selectTargets(listOf(wurm)).error shouldBe null
            game.resolveStack()

            game.damageOn(wurm) shouldBe 2
            game.damageOn(polukranos) shouldBe 6
            withClue("a 7/7 with 6 damage survives") {
                game.findPermanent("Polukranos, World Eater").shouldNotBeNull()
            }
        }
    }
}
