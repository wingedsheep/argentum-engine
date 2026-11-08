package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.DelightfulDiscovery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Delightful Discovery (J25 #6) — {4}{U} Instant.
 *
 * "This spell costs {1} less to cast for each spell your opponents have cast this turn.
 *  Scry 2, then draw two cards."
 *
 * Covers the self cost reduction: it counts only the *opponents'* casts this turn (resolved
 * against the caster while the card is still in hand), and the caster's own spells don't count.
 */
class DelightfulDiscoveryScenarioTest : FunSpec({

    fun newDriver(startingPlayer: Int): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(DelightfulDiscovery))
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = startingPlayer)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, targets = listOf(target)).error shouldBe null
    }

    test("three opponent spells this turn reduce the cost to {1}{U}, and no further") {
        val d = newDriver(startingPlayer = 1)
        val you = d.player1
        val opponent = d.player2

        d.bolt(opponent, you); d.bothPass()
        d.bolt(opponent, you); d.bothPass()
        // Third bolt stays on the stack; the opponent passes so you can respond.
        d.bolt(opponent, you)
        d.passPriority(opponent)

        val discovery = d.putCardInHand(you, "Delightful Discovery")
        d.giveMana(you, Color.BLUE, 1)
        d.castSpell(you, discovery).error shouldNotBe null

        d.giveMana(you, Color.BLUE, 1)
        d.castSpell(you, discovery).error shouldBe null
    }

    test("your own spells this turn don't reduce the cost") {
        val d = newDriver(startingPlayer = 0)
        val you = d.player1
        d.bolt(you, d.player2); d.bothPass()
        d.bolt(you, d.player2); d.bothPass()

        val discovery = d.putCardInHand(you, "Delightful Discovery")
        d.giveMana(you, Color.BLUE, 3)
        d.castSpell(you, discovery).error shouldNotBe null
    }
})
