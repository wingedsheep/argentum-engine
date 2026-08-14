package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.MistwaySpy
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mistway Spy — "When this creature is turned face up, until end of turn, whenever a creature you
 * control deals combat damage to a player, investigate."
 *
 * The payoff is a *floating* triggered ability, and the three things that distinguish it from the
 * naive reading are exactly what's tested:
 *
 *  1. it fires for **any** creature you control, not just the Spy — a SELF-bound trigger would pass
 *     an "attack with the Spy" test and fail every other board, so the assertion here is that a
 *     creature that isn't the Spy generates the Clue;
 *  2. it repeats — two creatures connecting in one combat investigate twice, which a one-shot
 *     delayed trigger would not;
 *  3. it exists only after the Spy is turned face up, so a face-down Spy on the battlefield during
 *     combat damage must produce nothing.
 *
 * Turning face up is a special action (CR 701.34c), not an enters-the-battlefield event, so the
 * ability hangs off `Triggers.TurnedFaceUp`.
 */
class MistwaySpyScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        // GameTestDriver starts with an empty registry, so the Clue token has to be registered
        // explicitly — an unregistered predefined token makes Investigate resolve to nothing.
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens)
        driver.registerCard(MistwaySpy)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Put [cardName] onto the battlefield face down under [mode], deriving its turn-up data exactly
     * the way a real face-down entry does (same helper as DisguiseKeywordScenarioTest).
     */
    fun GameTestDriver.putFaceDown(
        playerId: EntityId,
        cardName: String,
        mode: FaceDownMode
    ): EntityId {
        val id = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent).with(FaceDownModeComponent(mode))
                FaceDownTurnUp.dataFor(cardDef, cardName, mode)?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    fun clueCount(driver: GameTestDriver): Int =
        driver.getPermanents(driver.player1).count { driver.getCardName(it) == "Clue" }

    /**
     * Attack with [attackers] into an empty board and let combat damage resolve, draining whatever
     * it puts on the stack.
     *
     * Every step boundary is explicit and every action's `error` is asserted: declaring blockers
     * only becomes legal *after* passing out of declare-attackers, and an ignored
     * "You can only declare blockers during the declare blockers step" is exactly the kind of
     * rejection that turns a trigger test vacuous. An unblocked attacker needs no
     * damage-assignment decision, so [confirmCombatDamage] only applies when one is pending.
     */
    fun GameTestDriver.attackAndResolveDamage(attackers: List<EntityId>) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, attackers, player2).error shouldBe null
        bothPass()
        declareNoBlockers(player2).error shouldBe null
        passPriorityUntil(Step.COMBAT_DAMAGE)
        if (state.pendingDecision != null) confirmCombatDamage()
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    /** Put a face-down Spy on the battlefield and turn it face up for its disguise cost. */
    fun unmaskedSpy(driver: GameTestDriver): EntityId {
        val spy = driver.putFaceDown(driver.player1, "Mistway Spy", FaceDownMode.DISGUISE)
        driver.giveMana(driver.player1, Color.BLUE, 1)
        driver.giveColorlessMana(driver.player1, 1) // Disguise {1}{U}
        driver.submit(
            TurnFaceUp(
                playerId = driver.player1,
                sourceId = spy,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.state.getEntity(spy)?.has<FaceDownComponent>() shouldBe false
        driver.bothPass() // resolve the turned-face-up trigger, creating the floating ability
        return spy
    }

    test("a creature other than the Spy connecting investigates") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        unmaskedSpy(driver)
        clueCount(driver) shouldBe 0

        driver.attackAndResolveDamage(listOf(bears))

        withClue("the floating trigger watches every creature you control, not just the Spy") {
            clueCount(driver) shouldBe 1
        }
    }

    test("two creatures connecting investigate twice — the ability repeats") {
        val driver = newDriver()
        val first = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        listOf(first, second).forEach { driver.removeSummoningSickness(it) }

        unmaskedSpy(driver)

        driver.attackAndResolveDamage(listOf(first, second))

        withClue("this is a repeating trigger, not a one-shot delayed trigger") {
            clueCount(driver) shouldBe 2
        }
    }

    test("a Spy left face down grants nothing") {
        val driver = newDriver()
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        // The Spy is on the battlefield but never turned face up.
        driver.putFaceDown(driver.player1, "Mistway Spy", FaceDownMode.DISGUISE)

        driver.attackAndResolveDamage(listOf(bears))

        withClue("face down it is a vanilla 2/2 with ward {2} — no trigger at all") {
            clueCount(driver) shouldBe 0
        }
    }
})
