package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.BlightHerder
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Blight Herder (BFZ #2): "When you cast this spell, you may put two cards your opponents own from
 * exile into their owners' graveyards. If you do, create three 1/1 colorless Eldrazi Scion
 * creature tokens. They have 'Sacrifice this token: Add {C}.'"
 *
 * "Two" is all-or-nothing: with only one card an opponent owns in exile the option isn't offered
 * and nothing moves; with two or more, "yes" picks exactly two. The cast trigger resolves while
 * Blight Herder is still on the stack.
 */
class BlightHerderScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BlightHerder))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(5) { driver.putLandOnBattlefield(you, "Swamp") }
        return driver to you
    }

    class TriggerLog(var askedYesNo: Boolean = false, var minPick: Int? = null, var maxPick: Int? = null)

    /**
     * Casts Blight Herder and resolves only its cast trigger, answering the yes/no with [accept]
     * and the pick with [pick]. The creature spell is left on the stack.
     */
    fun GameTestDriver.castAndResolveTrigger(you: EntityId, accept: Boolean, pick: List<EntityId>): TriggerLog {
        castSpell(you, putCardInHand(you, "Blight Herder")).error shouldBe null
        state.stack.size shouldBe 2
        val log = TriggerLog()
        var safety = 0
        while ((state.stack.size > 1 || pendingDecision != null) && safety++ < 20) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is YesNoDecision -> {
                    log.askedYesNo = true
                    submitYesNo(you, accept)
                }
                is SelectCardsDecision -> {
                    log.minPick = decision.minSelections
                    log.maxPick = decision.maxSelections
                    submitCardSelection(you, pick)
                }
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
        return log
    }

    fun GameTestDriver.scions(you: EntityId): List<EntityId> =
        getCreatures(you).filter { state.projectedState.hasSubtype(it, "Scion") }

    test("putting two of an opponent's exiled cards into their graveyard creates three Scions") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val first = driver.putCardInExile(opponent, "Swamp")
        val second = driver.putCardInExile(opponent, "Grizzly Bears")
        val third = driver.putCardInExile(opponent, "Swamp")

        val log = driver.castAndResolveTrigger(you, accept = true, pick = listOf(first, second))

        log.askedYesNo shouldBe true
        log.minPick shouldBe 2
        log.maxPick shouldBe 2
        driver.getGraveyard(opponent) shouldContainAll listOf(first, second)
        driver.state.getZone(opponent, Zone.EXILE) shouldContain third
        val scions = driver.scions(you)
        scions shouldHaveSize 3
        scions.forEach {
            driver.state.projectedState.hasSubtype(it, "Eldrazi") shouldBe true
            driver.state.projectedState.getPower(it) shouldBe 1
            driver.state.projectedState.getToughness(it) shouldBe 1
        }
        // Blight Herder itself is still on the stack.
        driver.state.stack.size shouldBe 1
    }

    test("with only one card an opponent owns in exile, nothing can be put and no Scions are made") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val only = driver.putCardInExile(opponent, "Swamp")
        driver.putCardInExile(you, "Swamp")

        val log = driver.castAndResolveTrigger(you, accept = true, pick = emptyList())

        log.askedYesNo shouldBe false
        log.minPick shouldBe null
        driver.state.getZone(opponent, Zone.EXILE) shouldContain only
        driver.getGraveyard(opponent) shouldNotContain only
        driver.scions(you) shouldHaveSize 0
    }

    test("declining leaves the exiled cards and makes no Scions") {
        val (driver, you) = setup()
        val opponent = driver.getOpponent(you)
        val first = driver.putCardInExile(opponent, "Swamp")
        val second = driver.putCardInExile(opponent, "Swamp")

        val log = driver.castAndResolveTrigger(you, accept = false, pick = emptyList())

        log.askedYesNo shouldBe true
        driver.state.getZone(opponent, Zone.EXILE) shouldContainAll listOf(first, second)
        driver.scions(you) shouldHaveSize 0
    }
})
