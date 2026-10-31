package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.LuckyClover
import com.wingedsheep.mtg.sets.definitions.eld.cards.MerfolkSecretkeeper
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lucky Clover (ELD #226): {2} Artifact
 *
 * "Whenever you cast an Adventure instant or sorcery spell, copy it. You may choose new targets
 * for the copy."
 *
 * The Adventure is Merfolk Secretkeeper's "Venture Deeper" ({U} Sorcery — Adventure, "Target
 * player mills four cards"). Proves the copy is mandatory and retargetable, that the copy isn't a
 * card (it leaves nothing in exile), that the original still goes on an adventure and can be cast
 * as the creature from exile, and that casting the creature half doesn't trigger the Clover.
 */
class LuckyCloverScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LuckyClover, MerfolkSecretkeeper))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Grizzly Bears" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("casting an Adventure copies it; the copy can be retargeted and the original goes on an adventure") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Lucky Clover")
        val secretkeeper = driver.putCardInHand(player, "Merfolk Secretkeeper")
        driver.giveMana(player, Color.BLUE, 1)

        val myGraveyardBefore = driver.getGraveyard(player).size
        val oppGraveyardBefore = driver.getGraveyard(opponent).size

        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = secretkeeper,
                faceIndex = 0,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        // The Clover's trigger sits above Venture Deeper — no "may" prompt.
        driver.stackSize shouldBe 2
        driver.bothPass()

        // The copy offers new targets: point it at ourselves.
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(player, listOf(player))

        repeat(6) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
        driver.stackSize shouldBe 0

        // Copy milled us, the original milled the opponent.
        driver.getGraveyard(player).size shouldBe myGraveyardBefore + 4
        driver.getGraveyard(opponent).size shouldBe oppGraveyardBefore + 4

        // Only the real card is in exile — the copy ceased to exist — and it's on an adventure.
        driver.getExile(player) shouldBe listOf(secretkeeper)
        driver.state.mayPlayPermissions.any { secretkeeper in it.cardIds } shouldBe true

        // The original can now be cast as the creature from exile.
        driver.giveMana(player, Color.BLUE, 1)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = secretkeeper,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()
        driver.getCreatures(player).any { driver.getCardName(it) == "Merfolk Secretkeeper" } shouldBe true
    }

    test("casting the creature half is not an Adventure spell and is not copied") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Lucky Clover")
        val secretkeeper = driver.putCardInHand(player, "Merfolk Secretkeeper")
        driver.giveMana(player, Color.BLUE, 1)

        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = secretkeeper,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getCreatures(player).any { driver.getCardName(it) == "Merfolk Secretkeeper" } shouldBe true
    }
})
