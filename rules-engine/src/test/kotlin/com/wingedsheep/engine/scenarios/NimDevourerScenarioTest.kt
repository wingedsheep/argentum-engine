package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.NimDevourer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Nim Devourer (MRD #70) — {3}{B}{B} Creature — Zombie, 4/1.
 *
 * "This creature gets +1/+0 for each artifact you control.
 *  {B}{B}: Return this card from your graveyard to the battlefield, then sacrifice a creature.
 *  Activate only during your upkeep."
 *
 * Three claims worth pinning: the artifact-count boost is live and never counts the Devourer
 * itself, the timing rider really is "your upkeep" (both halves — right step *and* right turn),
 * and the return-then-sacrifice ordering means the Devourer is back on the battlefield in time to
 * be a legal sacrifice.
 */
class NimDevourerScenarioTest : FunSpec({

    val abilityId = NimDevourer.activatedAbilities.first().id

    fun newDriver(startingPlayer: Int = 0): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NimDevourer))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20, startingPlayer = startingPlayer)
        return driver
    }

    fun canActivate(driver: GameTestDriver, player: EntityId, devourer: EntityId): Boolean {
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        return enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
            .any { (it.action as? ActivateAbility)?.sourceId == devourer }
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 50) bothPass()
    }

    test("gets +1/+0 for each artifact you control, and is not itself an artifact") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val devourer = driver.putPermanentOnBattlefield(me, "Nim Devourer")

        // No artifacts yet — printed 4/1.
        driver.state.projectedState.getPower(devourer) shouldBe 4
        driver.state.projectedState.getToughness(devourer) shouldBe 1

        driver.putPermanentOnBattlefield(me, "Frogmite")
        driver.putPermanentOnBattlefield(me, "Frogmite")

        driver.state.projectedState.getPower(devourer) shouldBe 6
        driver.state.projectedState.getToughness(devourer) shouldBe 1
    }

    test("the boost only counts artifacts you control") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val devourer = driver.putPermanentOnBattlefield(me, "Nim Devourer")
        driver.putPermanentOnBattlefield(opponent, "Frogmite")

        driver.state.projectedState.getPower(devourer) shouldBe 4
    }

    test("can't be activated outside the upkeep step") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val devourer = driver.putCardInGraveyard(me, "Nim Devourer")
        driver.giveMana(me, Color.BLACK, 2)

        canActivate(driver, me, devourer) shouldBe false
    }

    test("can't be activated during an opponent's upkeep") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.UPKEEP)
        val activePlayer = driver.activePlayer!!
        val nonActive = driver.getOpponent(activePlayer)

        val devourer = driver.putCardInGraveyard(nonActive, "Nim Devourer")
        driver.giveMana(nonActive, Color.BLACK, 2)

        canActivate(driver, nonActive, devourer) shouldBe false
    }

    test("during your upkeep, returns itself and then sacrifices a creature") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.UPKEEP)
        val me = driver.activePlayer!!

        val devourer = driver.putCardInGraveyard(me, "Nim Devourer")
        // A second creature so the edict has a choice other than the Devourer itself.
        driver.putPermanentOnBattlefield(me, "Savannah Lions")
        driver.giveMana(me, Color.BLACK, 2)

        canActivate(driver, me, devourer) shouldBe true

        driver.submit(ActivateAbility(playerId = me, sourceId = devourer, abilityId = abilityId))
            .isSuccess shouldBe true
        driver.bothPass()

        // The return happens before the sacrifice, so the Devourer is on the battlefield and is
        // itself a legal choice for the edict.
        var guard = 0
        while (driver.isPaused && guard++ < 20) driver.autoResolveDecision()
        driver.drainStack()

        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(devourer) shouldBe false
        // Exactly one creature was sacrificed: two creatures were on the battlefield, one remains.
        driver.getCreatures(me).size shouldBe 1
    }
})
