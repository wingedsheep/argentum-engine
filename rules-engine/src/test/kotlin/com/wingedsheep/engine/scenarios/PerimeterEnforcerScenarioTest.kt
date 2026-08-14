package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.MuseumNightwatch
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NoviceInspector
import com.wingedsheep.mtg.sets.definitions.mkm.cards.PerimeterEnforcer
import com.wingedsheep.mtg.sets.definitions.mkm.cards.UndercoverCrocodelf
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Perimeter Enforcer (MKM #31) — "Whenever another Detective you control enters and whenever a
 * Detective you control is turned face up, this creature gets +1/+1 until end of turn."
 *
 * The card is the reason `Triggers.CreatureTurnedFaceUp` grew a `filter` parameter: before this
 * there was no way to say "a **Detective** you control is turned face up", only "a creature". These
 * tests pin both halves of the ability and, more importantly, the two things a filter can get wrong
 * — matching creatures it shouldn't, and (because a face-down permanent is a nameless, subtypeless
 * 2/2) matching nothing at all because it read the pre-flip characteristics.
 */
class PerimeterEnforcerScenarioTest : FunSpec({

    val allCards = TestCards.all + listOf(
        PerimeterEnforcer, NoviceInspector, UndercoverCrocodelf, MuseumNightwatch
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    /** Put [cardName] onto the battlefield face down, deriving turn-up data as a real cast would. */
    fun GameTestDriver.putFaceDown(playerId: EntityId, cardName: String): EntityId {
        val id = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent).with(FaceDownModeComponent(FaceDownMode.DISGUISE))
                FaceDownTurnUp.dataFor(cardDef, cardName, FaceDownMode.DISGUISE)?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    /** Settle the stack without letting the turn advance past the end-of-turn pump expiry. */
    fun GameTestDriver.settle() {
        repeat(4) { if (state.priorityPlayerId != null && stackSize > 0) bothPass() }
    }

    context("whenever another Detective you control enters") {

        test("a Detective entering under your control pumps the Enforcer to 2/2") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val enforcer = driver.putCreatureOnBattlefield(player, "Perimeter Enforcer")
            driver.state.projectedState.getPower(enforcer) shouldBe 1

            val inspector = driver.putCardInHand(player, "Novice Inspector")
            driver.giveMana(player, Color.WHITE, 1)
            driver.castSpell(player, inspector).error shouldBe null
            driver.settle()

            driver.state.projectedState.getPower(enforcer) shouldBe 2
            driver.state.projectedState.getToughness(enforcer) shouldBe 2
        }

        test("two Detectives entering stack the pump to 3/3") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val enforcer = driver.putCreatureOnBattlefield(player, "Perimeter Enforcer")

            repeat(2) {
                val inspector = driver.putCardInHand(player, "Novice Inspector")
                driver.giveMana(player, Color.WHITE, 1)
                driver.castSpell(player, inspector).error shouldBe null
                driver.settle()
            }

            driver.state.projectedState.getPower(enforcer) shouldBe 3
            driver.state.projectedState.getToughness(enforcer) shouldBe 3
        }

        test("a non-Detective creature entering does not pump it") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val enforcer = driver.putCreatureOnBattlefield(player, "Perimeter Enforcer")

            val bears = driver.putCardInHand(player, "Grizzly Bears")
            driver.giveMana(player, Color.GREEN, 2)
            driver.castSpell(player, bears).error shouldBe null
            driver.settle()

            driver.state.projectedState.getPower(enforcer) shouldBe 1
        }

        test("an opponent's Detective entering does not pump it — 'you control'") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // The Enforcer belongs to the non-active player; the active player casts the Detective.
            val enforcer = driver.putCreatureOnBattlefield(opponent, "Perimeter Enforcer")

            val inspector = driver.putCardInHand(player, "Novice Inspector")
            driver.giveMana(player, Color.WHITE, 1)
            driver.castSpell(player, inspector).error shouldBe null
            driver.settle()

            driver.state.projectedState.getPower(enforcer) shouldBe 1
        }

        test("its own arrival does not pump it — 'another'") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val card = driver.putCardInHand(player, "Perimeter Enforcer")
            driver.giveMana(player, Color.WHITE, 2)
            driver.castSpell(player, card).error shouldBe null
            driver.settle()

            val enforcer = driver.findPermanent(player, "Perimeter Enforcer")!!
            driver.state.projectedState.getPower(enforcer) shouldBe 1
        }
    }

    context("whenever a Detective you control is turned face up") {

        test("flipping a face-down Detective pumps the Enforcer") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val enforcer = driver.putCreatureOnBattlefield(player, "Perimeter Enforcer")
            // Undercover Crocodelf — Elf Crocodile Detective, disguise {3}{G/U}{G/U}.
            val croc = driver.putFaceDown(player, "Undercover Crocodelf")
            driver.giveMana(player, Color.GREEN, 5)

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = croc,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            driver.settle()

            driver.state.getEntity(croc)?.get<FaceDownComponent>() shouldBe null
            driver.state.projectedState.getPower(enforcer) shouldBe 2
            driver.state.projectedState.getToughness(enforcer) shouldBe 2
        }

        test("flipping a face-down non-Detective does not pump it") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val enforcer = driver.putCreatureOnBattlefield(player, "Perimeter Enforcer")
            // Museum Nightwatch — Centaur Soldier, disguise {1}{W}. Same face-down 2/2 as the
            // Crocodelf, so only the post-flip type line can tell the two cases apart.
            val nightwatch = driver.putFaceDown(player, "Museum Nightwatch")
            driver.giveMana(player, Color.WHITE, 2)

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = nightwatch,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            driver.settle()

            driver.state.getEntity(nightwatch)?.get<FaceDownComponent>() shouldBe null
            driver.state.projectedState.getPower(enforcer) shouldBe 1
        }

        test("an opponent flipping their Detective does not pump it — 'you control'") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // The Enforcer sits with the non-active player; the active player — who has priority,
            // and so can take the turn-up special action — flips their own Detective.
            val enforcer = driver.putCreatureOnBattlefield(opponent, "Perimeter Enforcer")
            val croc = driver.putFaceDown(player, "Undercover Crocodelf")
            driver.giveMana(player, Color.GREEN, 5)

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = croc,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            driver.settle()

            driver.state.getEntity(croc)?.get<FaceDownComponent>() shouldBe null
            driver.state.projectedState.getPower(enforcer) shouldBe 1
        }
    }

    context("duration") {

        test("the pump wears off at end of turn") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val enforcer = driver.putCreatureOnBattlefield(player, "Perimeter Enforcer")
            val inspector = driver.putCardInHand(player, "Novice Inspector")
            driver.giveMana(player, Color.WHITE, 1)
            driver.castSpell(player, inspector).error shouldBe null
            driver.settle()
            driver.state.projectedState.getPower(enforcer) shouldBe 2

            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.state.projectedState.getPower(enforcer) shouldBe 1
            driver.state.projectedState.getToughness(enforcer) shouldBe 1
        }
    }
})
