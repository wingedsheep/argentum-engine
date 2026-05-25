package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for declaring attacking bands (CR 702.22). A band groups one or more attacking
 * creatures with banding plus up to one without; all members attack the same defender and
 * are stamped with a shared [AttackingComponent.bandId].
 */
class BandDeclarationTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("a banding creature and a non-banding creature can be declared as one band") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(active, "Banding Scout")
        val courser = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        driver.removeSummoningSickness(scout)
        driver.removeSummoningSickness(courser)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackingBand(active, listOf(scout, courser), opponent).isSuccess shouldBe true

        // Both attackers carry the same, non-null band id (CR 702.22).
        val scoutBand = driver.state.getEntity(scout)?.get<AttackingComponent>()?.bandId
        val courserBand = driver.state.getEntity(courser)?.get<AttackingComponent>()?.bandId
        scoutBand.shouldNotBeNull()
        courserBand shouldBe scoutBand
    }

    test("a lone attacker has no band id") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(active, "Banding Scout")
        driver.removeSummoningSickness(scout)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(scout), opponent).isSuccess shouldBe true

        driver.state.getEntity(scout)?.get<AttackingComponent>()?.bandId shouldBe null
    }

    test("a band must contain at least two creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scout = driver.putCreatureOnBattlefield(active, "Banding Scout")
        driver.removeSummoningSickness(scout)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val result = driver.submit(
            DeclareAttackers(active, mapOf(scout to opponent), bands = listOf(setOf(scout)))
        )
        result.isSuccess shouldBe false
        result.error shouldNotBe null
    }

    test("a band may contain at most one creature without banding") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val courserA = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        val courserB = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        driver.removeSummoningSickness(courserA)
        driver.removeSummoningSickness(courserB)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val result = driver.declareAttackingBand(active, listOf(courserA, courserB), opponent)
        result.isSuccess shouldBe false
        result.error shouldNotBe null
    }
})
