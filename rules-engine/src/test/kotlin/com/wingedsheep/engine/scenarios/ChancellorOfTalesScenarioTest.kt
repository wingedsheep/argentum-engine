package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.ChancellorOfTales
import com.wingedsheep.mtg.sets.definitions.woe.cards.CruelSomnophage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chancellor of Tales (WOE #45): {3}{U} 2/3 Faerie Advisor with flying
 *
 * "Whenever you cast an Adventure spell, you may copy it. You may choose new targets for the copy."
 *
 * Covers the new `SpellCastPredicate.CastAsAdventure`: the trigger keys off *how* the card was
 * cast, so casting the same adventurer card as its creature half must not fire it.
 *
 * The Adventure used here is Cruel Somnophage's "Can't Wake Up" ({1}{U} Sorcery — Adventure,
 * "Target player mills four cards"), whose effect is directly observable in the graveyard count.
 */
class ChancellorOfTalesScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ChancellorOfTales, CruelSomnophage))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    /** Cast Cruel Somnophage's Adventure face ("Can't Wake Up") at [target]. */
    fun GameTestDriver.castCantWakeUp(player: EntityId, card: EntityId, target: EntityId) {
        submitSuccess(
            CastSpell(
                playerId = player,
                cardId = card,
                faceIndex = 0,
                targets = listOf(ChosenTarget.Player(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    /** Drain the stack, answering the copy's optional "choose new targets" prompt if it appears. */
    fun GameTestDriver.finishResolving(player: EntityId, retarget: EntityId) {
        repeat(8) {
            if (pendingDecision is ChooseTargetsDecision) {
                submitTargetSelection(player, listOf(retarget))
            } else if (!isPaused && stackSize > 0) {
                bothPass()
            }
        }
    }

    test("casting an Adventure spell offers a copy, and accepting mills twice") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Chancellor of Tales")
        val somnophage = driver.putCardInHand(player, "Cruel Somnophage")
        driver.giveMana(player, Color.BLUE, 2) // {1}{U}

        val graveyardBefore = driver.getGraveyard(opponent).size

        driver.castCantWakeUp(player, somnophage, opponent)

        // The cast trigger sits above the spell; resolving it asks "you may copy it".
        driver.bothPass()
        driver.isPaused shouldBe true
        driver.submitYesNo(player, true)

        driver.finishResolving(player, opponent)

        // Original + copy = eight cards milled.
        driver.getGraveyard(opponent).size shouldBe graveyardBefore + 8
    }

    test("declining the copy mills only once") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Chancellor of Tales")
        val somnophage = driver.putCardInHand(player, "Cruel Somnophage")
        driver.giveMana(player, Color.BLUE, 2)

        val graveyardBefore = driver.getGraveyard(opponent).size

        driver.castCantWakeUp(player, somnophage, opponent)
        driver.bothPass()
        driver.isPaused shouldBe true
        driver.submitYesNo(player, false)

        driver.finishResolving(player, opponent)

        driver.getGraveyard(opponent).size shouldBe graveyardBefore + 4
    }

    test("casting the creature half is not an Adventure spell and does not trigger") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Chancellor of Tales")
        // One creature card in a graveyard, so the */* Somnophage resolves as a 1/1 rather than
        // dying to state-based actions as a 0/0 before we can look at it.
        driver.putCardInGraveyard(player, "Grizzly Bears")
        val somnophage = driver.putCardInHand(player, "Cruel Somnophage")
        driver.giveMana(player, Color.BLACK, 2) // {1}{B} creature half

        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = somnophage,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // Only the creature spell is on the stack — no copy trigger was added above it.
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.isPaused shouldBe false

        val resolved = driver.getCreatures(player).single { driver.getCardName(it) == "Cruel Somnophage" }
        driver.state.projectedState.getPower(resolved) shouldBe 1
        driver.state.projectedState.getToughness(resolved) shouldBe 1
    }
})
