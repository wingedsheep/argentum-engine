package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.CulvertAmbusher
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Culvert Ambusher — "When this creature enters or is turned face up, target creature blocks this
 * turn if able."
 *
 * Covers the new `Effects.MarkMustBlockThisTurn` vocabulary end to end: the floating
 * `SetMustBlock` it applies has to reach `ProjectedState.mustBlock`, and `BlockPhaseManager` has to
 * reject a declaration that ignores it. Both halves matter — a modification that projects but is
 * never validated is indistinguishable from doing nothing.
 *
 * The second test is the CR 509.1c half: "if able" is a requirement, not a guarantee, so a tapped
 * creature is excused and its controller may declare no blockers at all.
 */
class CulvertAmbusherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CulvertAmbusher)
        return driver
    }

    /**
     * Cast the Ambusher and point its enters trigger at [victim]. Leaves the game with the trigger
     * fully resolved.
     */
    fun castAmbusherTargeting(driver: GameTestDriver, caster: EntityId, victim: EntityId) {
        val card = driver.putCardInHand(caster, "Culvert Ambusher")
        driver.giveMana(caster, Color.GREEN, 5)
        driver.castSpellWithTargets(caster, card, emptyList()).error shouldBe null
        driver.bothPass() // resolve the creature spell; the enters trigger goes on the stack

        // The trigger's target is chosen as it goes on the stack.
        driver.pendingDecision.shouldNotBeNull()
        withClue("the enters trigger must ask for its target") {
            driver.submitTargetSelection(caster, listOf(victim)).error shouldBe null
        }
        driver.bothPass() // resolve the trigger
    }

    test("the marked creature must block if it can") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castAmbusherTargeting(driver, active, blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(attacker), opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("declaring no blockers ignores the requirement and must be rejected") {
            driver.declareNoBlockers(opponent).error shouldNotBe null
        }
        withClue("blocking with the marked creature satisfies it") {
            driver.declareBlockers(opponent, mapOf(blocker to listOf(attacker))).error shouldBe null
        }
    }

    test("a tapped creature is excused — 'if able' is a requirement, not a guarantee") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(active, "Grizzly Bears")
        driver.removeSummoningSickness(attacker)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castAmbusherTargeting(driver, active, blocker)
        driver.tapPermanent(blocker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(attacker), opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("a tapped creature can't block, so declaring no blockers is legal (CR 509.1c)") {
            driver.declareNoBlockers(opponent).error shouldBe null
        }
    }
})
