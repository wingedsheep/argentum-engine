package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent.HijackState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.SecretOfBloodbending
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.HijackScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Secret of Bloodbending.
 *
 * Secret of Bloodbending ({U}{U}{U}{U}, Sorcery — Lesson, Mythic):
 * "As an additional cost to cast this spell, you may waterbend {10}.
 *  You control target opponent during their next combat phase. If this spell's additional cost was
 *  paid, you control that player during their next turn instead. Exile Secret of Bloodbending."
 *
 * A Mindslaver variant. The base mode schedules a **combat-phase-scoped** hijack
 * ([HijackScope.NextCombatPhase]); paying the optional waterbend upgrades it to a **whole-turn**
 * hijack ([HijackScope.NextTurn]). Both branch off `Conditions.WaterbendWasPaid`. This test pins:
 *  1. Base cast → opponent gets a SCHEDULED NextCombatPhase hijack; the Lesson exiles itself.
 *  2. Waterbended cast → opponent gets a SCHEDULED NextTurn hijack instead.
 *  3. The new combat-phase-scoped lifecycle: it does NOT engage at the affected player's turn
 *     start, engages at their beginning-of-combat (routing input to the hijacker), and reverts
 *     when that one combat phase ends.
 */
class SecretOfBloodbendingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SecretOfBloodbending))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("base mode schedules a combat-phase hijack on the opponent and exiles itself") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val spell = driver.putCardInHand(active, "Secret of Bloodbending")
        driver.giveMana(active, Color.BLUE, 4) // {U}{U}{U}{U}, no waterbend

        driver.castSpell(active, spell, targets = listOf(opponent)).isSuccess shouldBe true
        driver.bothPass() // resolve

        val hijack = driver.state.getEntity(opponent)?.get<PlayerTurnHijackedComponent>()
        hijack?.controllerId shouldBe active
        hijack?.state shouldBe HijackState.SCHEDULED
        hijack?.scope shouldBe HijackScope.NextCombatPhase

        // Lesson: "Exile Secret of Bloodbending" — it goes to exile, not the graveyard.
        driver.getExileCardNames(active) shouldContain "Secret of Bloodbending"
    }

    test("paying the optional waterbend schedules a whole-turn hijack instead") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val spell = driver.putCardInHand(active, "Secret of Bloodbending")
        driver.giveMana(active, Color.BLUE, 14) // {U}{U}{U}{U} + waterbend {10}

        driver.submit(
            CastSpell(
                playerId = active,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(opponent)),
                wasWaterbendPaid = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve

        val hijack = driver.state.getEntity(opponent)?.get<PlayerTurnHijackedComponent>()
        hijack?.controllerId shouldBe active
        hijack?.state shouldBe HijackState.SCHEDULED
        hijack?.scope shouldBe HijackScope.NextTurn
    }

    test("a combat-phase hijack engages at beginning of combat and reverts when it ends") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        // Schedule a combat-phase-scoped hijack of the opponent directly (skip the cast).
        driver.replaceState(
            driver.state.updateEntity(opponent) { container ->
                container.with(
                    PlayerTurnHijackedComponent(
                        controllerId = active,
                        state = HijackState.SCHEDULED,
                        scope = HijackScope.NextCombatPhase
                    )
                )
            }
        )

        // Cross into the opponent's turn.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe opponent

        // A combat-phase hijack must NOT engage at turn start (only whole-turn hijacks do).
        driver.state.getEntity(opponent)?.get<PlayerTurnHijackedComponent>()?.state shouldBe HijackState.SCHEDULED
        driver.state.actorFor(opponent) shouldBe opponent

        // Beginning of the opponent's combat phase → the hijack engages, routing input authority
        // to the controller.
        driver.passPriorityUntil(Step.BEGIN_COMBAT, maxPasses = 200)
        val engaged = driver.state.getEntity(opponent)?.get<PlayerTurnHijackedComponent>()
        engaged?.state shouldBe HijackState.ACTIVE
        engaged?.scope shouldBe HijackScope.NextCombatPhase
        driver.state.actorFor(opponent) shouldBe active

        // Leaving the combat phase (entry to the postcombat main phase) ends control.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, maxPasses = 200)
        driver.state.getEntity(opponent)?.has<PlayerTurnHijackedComponent>() shouldBe false
        driver.state.actorFor(opponent) shouldBe opponent
    }
})
