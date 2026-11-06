package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Defiant Greatmaw (AKH #163).
 *
 *   When this creature enters, put two -1/-1 counters on target creature you control.
 *   Whenever you put one or more -1/-1 counters on this creature, remove a -1/-1 counter from
 *   another target creature you control.
 *
 * Proves the SELF-bound "you put -1/-1 counters on this creature" trigger fires off its own enters
 * trigger (the classic line: put the counters on the Greatmaw itself) and strips a counter from
 * another creature, and that counters put on a different creature don't trigger it.
 */
class DefiantGreatmawScenarioTest : ScenarioTestBase() {

    private fun TestGame.minusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    init {
        test("putting the enters counters on itself removes a -1/-1 counter from another creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Defiant Greatmaw")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) {
                it.with(CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 1))
            }

            game.castSpell(1, "Defiant Greatmaw").error shouldBe null
            game.resolveStack()

            val greatmaw = game.findPermanent("Defiant Greatmaw")!!
            withClue("the enters trigger asks for its target") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(greatmaw)).error shouldBe null
            game.resolveStack()

            withClue("the counters landing on the Greatmaw trigger its second ability") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.minusCounters(greatmaw) shouldBe 2
            withClue("one -1/-1 counter came off the other creature") {
                game.minusCounters(bears) shouldBe 0
            }
        }

        test("putting the enters counters on another creature doesn't trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Defiant Greatmaw")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) {
                it.with(CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, 1))
            }

            game.castSpell(1, "Defiant Greatmaw").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("no second trigger: the counters went on the Hill Giant") {
                game.getPendingDecision() shouldBe null
                game.state.stack.isEmpty() shouldBe true
            }
            game.minusCounters(giant) shouldBe 2
            game.minusCounters(bears) shouldBe 1
        }
    }
}
