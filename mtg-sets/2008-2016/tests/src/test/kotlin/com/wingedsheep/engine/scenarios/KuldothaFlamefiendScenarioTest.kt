package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mbs.cards.KuldothaFlamefiend
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Kuldotha Flamefiend (MBS): "When this creature enters, you may sacrifice an artifact. If you do,
 * this creature deals 4 damage divided as you choose among any number of targets."
 *
 * The targets and their division are announced as the trigger goes on the stack (CR 603.3d), even
 * though the damage sits behind the "if you do" gate that opens only at resolution.
 */
class KuldothaFlamefiendScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KuldothaFlamefiend))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(6) { driver.putLandOnBattlefield(you, "Mountain") }
        val artifact = driver.putPermanentOnBattlefield(you, "Artifact Creature")
        return Triple(driver, you, artifact)
    }

    /** Casts the Flamefiend and resolves it, stopping at the trigger's target prompt. */
    fun GameTestDriver.castFlamefiend(you: EntityId) {
        castSpell(you, putCardInHand(you, "Kuldotha Flamefiend"))
        var safety = 0
        while (pendingDecision !is ChooseTargetsDecision && safety++ < 20) bothPass()
    }

    /** Answers the "may sacrifice" pick (the artifact, or none to decline) and resolves the trigger. */
    fun GameTestDriver.resolveGate(you: EntityId, artifact: EntityId, sacrifice: Boolean) {
        var safety = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && safety++ < 20) {
            when (val decision = pendingDecision) {
                null -> bothPass()
                is SelectCardsDecision -> submitCardSelection(you, if (sacrifice) listOf(artifact) else emptyList())
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
        }
    }

    test("sacrificing an artifact deals the division announced on the stack") {
        val (driver, you, artifact) = setup()
        val opponent = driver.getOpponent(you)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.castFlamefiend(you)
        driver.submitMultiTargetSelection(you, mapOf(0 to listOf(bear, opponent)))

        // The division is asked for now, before the gate.
        val divide = driver.pendingDecision as DistributeDecision
        divide.totalAmount shouldBe 4
        driver.submitDecision(you, DistributionResponse(divide.id, mapOf(bear to 2, opponent to 2)))

        driver.resolveGate(you, artifact, sacrifice = true)

        driver.getGraveyard(you) shouldContain artifact
        driver.state.getBattlefield() shouldNotContain bear
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("declining the sacrifice deals no damage") {
        val (driver, you, artifact) = setup()
        val opponent = driver.getOpponent(you)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.castFlamefiend(you)
        driver.submitMultiTargetSelection(you, mapOf(0 to listOf(bear, opponent)))
        val divide = driver.pendingDecision as DistributeDecision
        driver.submitDecision(you, DistributionResponse(divide.id, mapOf(bear to 1, opponent to 3)))

        driver.resolveGate(you, artifact, sacrifice = false)

        driver.state.getBattlefield() shouldContain artifact
        driver.state.getBattlefield() shouldContain bear
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("a single target takes all 4 damage") {
        val (driver, you, artifact) = setup()
        val opponent = driver.getOpponent(you)

        driver.castFlamefiend(you)
        driver.submitMultiTargetSelection(you, mapOf(0 to listOf(opponent)))
        driver.resolveGate(you, artifact, sacrifice = true)

        driver.getLifeTotal(opponent) shouldBe 16
    }

    test("choosing no targets still lets you sacrifice, and nothing is dealt") {
        val (driver, you, artifact) = setup()
        val opponent = driver.getOpponent(you)

        driver.castFlamefiend(you)
        driver.submitMultiTargetSelection(you, mapOf(0 to emptyList()))
        driver.resolveGate(you, artifact, sacrifice = true)

        driver.getGraveyard(you) shouldContain artifact
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
