package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.WastelandStrangler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Wasteland Strangler (BFZ #102): "When this creature enters, you may put a card an opponent owns
 * from exile into that player's graveyard. If you do, target creature gets -3/-3 until end of turn."
 *
 * The -3/-3 target is announced as the trigger goes on the stack; the processed card is picked on
 * resolution, and only a card an opponent owns can be picked. No pick, no shrink.
 */
class WastelandStranglerScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WastelandStrangler))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(you, "Swamp") }
        return driver to you
    }

    /** Casts the Strangler, resolves it, and answers the trigger's target prompt with [target]. */
    fun GameTestDriver.castStranglerTargeting(you: EntityId, target: EntityId) {
        castSpell(you, putCardInHand(you, "Wasteland Strangler"))
        var safety = 0
        while (pendingDecision !is ChooseTargetsDecision && safety++ < 20) bothPass()
        submitTargetSelection(you, listOf(target))
    }

    /**
     * Resolves the trigger, answering the processor pick with [pick] (empty to decline). Returns
     * the options the pick offered, or null when no pick was asked.
     */
    fun GameTestDriver.resolveTrigger(you: EntityId, pick: List<EntityId>): List<EntityId>? {
        var offered: List<EntityId>? = null
        var safety = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && safety++ < 20) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is SelectCardsDecision -> {
                    offered = decision.options
                    submitCardSelection(you, pick)
                }
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
        return offered
    }

    test("processing an opponent's exiled card gives the target -3/-3") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val exiled = driver.putCardInExile(opponent, "Swamp")

        driver.castStranglerTargeting(you, bears)
        val offered = driver.resolveTrigger(you, listOf(exiled))

        offered!! shouldContainExactly listOf(exiled)
        driver.getGraveyard(opponent) shouldContain exiled
        driver.state.getZone(opponent, Zone.EXILE) shouldNotContain exiled
        driver.state.getBattlefield() shouldNotContain bears
        driver.getGraveyard(opponent) shouldContain bears
    }

    test("declining the pick leaves the exiled card and the target untouched") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val exiled = driver.putCardInExile(opponent, "Swamp")

        driver.castStranglerTargeting(you, bears)
        driver.resolveTrigger(you, emptyList())

        driver.state.getZone(opponent, Zone.EXILE) shouldContain exiled
        driver.state.getBattlefield() shouldContain bears
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("a card you own in exile can't be processed, so nothing happens") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val mine = driver.putCardInExile(you, "Swamp")

        driver.castStranglerTargeting(you, bears)
        val offered = driver.resolveTrigger(you, emptyList())

        offered shouldBe null
        driver.state.getZone(you, Zone.EXILE) shouldContain mine
        driver.state.getBattlefield() shouldContain bears
    }
})
