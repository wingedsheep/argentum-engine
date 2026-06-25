package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.CursedRecording
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cursed Recording (DSK #131) — {2}{R}{R} Artifact.
 *
 * "Whenever you cast an instant or sorcery spell, put a time counter on this artifact. Then if there
 * are seven or more time counters on it, remove those counters and it deals 20 damage to you.
 * {T}: When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new
 * targets for the copy."
 *
 * Exercises (1) the cast-an-instant-or-sorcery trigger adding a time counter, (2) the resolution-time
 * "Then if there are seven or more time counters" check removing the counters and dealing 20 damage
 * to the controller, and (3) the {T} arming of a one-shot delayed spell copy.
 */
class CursedRecordingScenarioTest : FunSpec({

    val copyAbilityId = CursedRecording.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + CursedRecording)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun timeCounters(driver: GameTestDriver, recording: EntityId): Int =
        driver.state.getEntity(recording)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    test("casting an instant puts a time counter on Cursed Recording") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2

        val recording = driver.putPermanentOnBattlefield(player, "Cursed Recording")

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(opponent)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()

        timeCounters(driver, recording) shouldBe 1
    }

    test("seventh time counter removes the counters and deals 20 damage to you") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2

        val recording = driver.putPermanentOnBattlefield(player, "Cursed Recording")
        // Pre-load six time counters so the next cast pushes it to seven.
        driver.addComponent(recording, CountersComponent(mapOf(CounterType.TIME to 6)))
        // Raise life so the 20 damage doesn't end the game.
        driver.setLifeTotal(player, 40)

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(opponent)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()

        // The counters were removed and the controller took 20.
        timeCounters(driver, recording) shouldBe 0
        driver.getLifeTotal(player) shouldBe 20
    }

    test("{T} arms a copy of the next instant or sorcery cast this turn") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2

        val recording = driver.putPermanentOnBattlefield(player, "Cursed Recording")

        // Arm the delayed copy.
        driver.submit(
            ActivateAbility(playerId = player, sourceId = recording, abilityId = copyAbilityId)
        ).error shouldBe null
        driver.bothPass() // resolve the {T} ability — registers the pending spell copy
        driver.state.pendingSpellCopies.size shouldBe 1

        // Cast an instant — it gets copied (a storm-copy trigger lands on the stack) and the pending
        // copy is consumed.
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(opponent)).isSuccess shouldBe true

        val stormCopies = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.count { it.effect is StormCopyEffect }
        stormCopies shouldBe 1
        driver.state.pendingSpellCopies.size shouldBe 0
    }
})
