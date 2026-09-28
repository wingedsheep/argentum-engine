package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.ExpeditionLookout
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Expedition Lookout (MOM) — Defender; as long as an opponent has eight or more cards in
 * their graveyard, it can attack as though it didn't have defender and can't be blocked.
 */
class ExpeditionLookoutScenarioTest : FunSpec({

    fun setup(opponentGraveyard: Int, ownGraveyard: Int = 0): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(ExpeditionLookout))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        repeat(opponentGraveyard) { driver.putCardInGraveyard(opponent, "Island") }
        repeat(ownGraveyard) { driver.putCardInGraveyard(you, "Island") }
        val lookout = driver.putCreatureOnBattlefield(you, "Expedition Lookout")
        driver.removeSummoningSickness(lookout)
        // Decoy so the declare-attackers step isn't skipped.
        val decoy = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.removeSummoningSickness(decoy)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        return Triple(driver, lookout, opponent)
    }

    test("cannot attack with seven cards in the opponent's graveyard") {
        val (driver, lookout, opponent) = setup(opponentGraveyard = 7, ownGraveyard = 10)
        val result = driver.declareAttackers(driver.activePlayer!!, listOf(lookout), opponent)
        (result.error != null) shouldBe true
        driver.state.getEntity(lookout)?.get<AttackingComponent>().shouldBeNull()
    }

    test("can attack with eight cards in the opponent's graveyard") {
        val (driver, lookout, opponent) = setup(opponentGraveyard = 8)
        val result = driver.declareAttackers(driver.activePlayer!!, listOf(lookout), opponent)
        result.error shouldBe null
        driver.state.getEntity(lookout)?.get<AttackingComponent>().shouldNotBeNull()
    }
})
