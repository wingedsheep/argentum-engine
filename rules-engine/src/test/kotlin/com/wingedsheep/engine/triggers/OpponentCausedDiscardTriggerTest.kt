package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `EventPattern.DiscardEvent.cause` + `Player.ControllerOfCause`: "When a spell or ability an
 * opponent controls causes you to discard this card, that player loses 5 life" (Psychic Purge).
 *
 * The cause is the controller stamped on `CardsDiscardedEvent` by the discard site. An opponent's
 * resolving spell carries one; your own spell carries you; the cleanup-step hand-size discard
 * (a turn-based action, not a spell or ability) carries none. The unqualified `isDiscarded()`
 * stays the every-discard trigger.
 */
class OpponentCausedDiscardTriggerTest : FunSpec({

    val purgeLike = card("Opponent Discard Punisher") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        triggeredAbility {
            trigger = Triggers.self.isDiscarded(cause = ZoneChangeCause.DiscardedByOpponentEffect)
            effect = Effects.LoseLife(5, EffectTarget.PlayerRef(Player.ControllerOfCause))
        }
    }
    val anyDiscard = card("Any Discard Lifegain") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        triggeredAbility {
            trigger = Triggers.self.isDiscarded()
            effect = Effects.GainLife(1)
        }
    }

    val eachOpponentDiscards = card("Each Opponent Discards Test") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Hand.eachOpponentDiscards(1) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(purgeLike)
        driver.registerCard(anyDiscard)
        driver.registerCard(eachOpponentDiscards)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("an opponent's spell: the trigger names the opponent, though the spell has left the stack") {
        val driver = newDriver()
        val me = driver.player1
        val foe = driver.player2
        val punisher = driver.putCardInHand(foe, purgeLike.name)
        val filler = driver.putCardInHand(foe, "Grizzly Bears")

        val mindRot = driver.putCardInHand(me, "Mind Rot")
        driver.giveMana(me, Color.BLACK, 3)
        driver.castSpell(me, mindRot, listOf(foe)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.submitCardSelection(foe, listOf(punisher, filler))

        // Mind Rot is in its owner's graveyard; only the trigger remains, controlled by the discarder.
        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe 15
        driver.getLifeTotal(foe) shouldBe 20
    }

    test("an opponent's \"each opponent discards\" names its caster, not the iterated discarder") {
        val driver = newDriver()
        val me = driver.player1
        val foe = driver.player2
        val punisher = driver.putCardInHand(foe, purgeLike.name)

        val spell = driver.putCardInHand(me, eachOpponentDiscards.name)
        driver.giveMana(me, Color.BLACK, 1)
        driver.castSpell(me, spell).outcome shouldBe Outcome.Done
        driver.bothPass()
        (driver.pendingDecision as? SelectCardsDecision)?.let { driver.submitCardSelection(foe, listOf(punisher)) }

        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe 15
        driver.getLifeTotal(foe) shouldBe 20
    }

    test("your own spell is no opponent's — the qualified trigger stays off, the plain one fires") {
        val driver = newDriver()
        val me = driver.player1
        val punisher = driver.putCardInHand(me, purgeLike.name)
        val lifegain = driver.putCardInHand(me, anyDiscard.name)

        val mindRot = driver.putCardInHand(me, "Mind Rot")
        driver.giveMana(me, Color.BLACK, 3)
        driver.castSpell(me, mindRot, listOf(me)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.submitCardSelection(me, listOf(punisher, lifegain))

        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe 21
        driver.getLifeTotal(driver.player2) shouldBe 20
    }

    test("the cleanup-step hand-size discard has no spell or ability cause") {
        val driver = newDriver()
        val me = driver.player1
        val punisher = driver.putCardInHand(me, purgeLike.name)
        val lifegain = driver.putCardInHand(me, anyDiscard.name)
        repeat(7) { driver.putCardInHand(me, "Island") }

        driver.passPriorityUntil(Step.CLEANUP)
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val others = decision.options.filter { it != punisher && it != lifegain }
        driver.submitCardSelection(me, listOf(punisher, lifegain) + others.take(decision.minSelections - 2))

        // Only the unqualified trigger fires (CR 514.3a gives priority to put it on the stack).
        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe 21
        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
