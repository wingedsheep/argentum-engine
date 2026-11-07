package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.PsemillaMeletianPoet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Psemilla, Meletian Poet (J25 #31) — "Whenever you cast your first enchantment spell each turn,
 * create a 2/2 white Nymph enchantment creature token. At the beginning of each combat, if you
 * control five or more enchantments, Psemilla gets +4/+4 and gains lifelink until end of turn."
 */
class PsemillaMeletianPoetScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PsemillaMeletianPoet)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.nymphs() = getPermanents(player1).filter { getCardName(it) == "Nymph Token" }

    test("only the first enchantment spell each turn makes a Nymph") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Psemilla, Meletian Poet")

        val first = d.putCardInHand(d.player1, "Test Enchantment")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.castSpell(d.player1, first).outcome shouldBe Outcome.Done
        d.assertStackSize(2, "the cast trigger sits above the enchantment spell")
        d.bothPass() // trigger resolves
        d.bothPass() // enchantment resolves

        val nymph = d.nymphs().single()
        withClue("the Nymph is a 2/2 enchantment creature") {
            d.state.projectedState.hasType(nymph, "ENCHANTMENT") shouldBe true
            d.state.projectedState.getPower(nymph) shouldBe 2
        }

        val second = d.putCardInHand(d.player1, "Test Enchantment")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.castSpell(d.player1, second).outcome shouldBe Outcome.Done
        d.assertStackSize(1, "the second enchantment spell this turn doesn't trigger")
        d.bothPass()
        d.nymphs().size shouldBe 1
    }

    test("with five enchantments, beginning of combat makes Psemilla a 5/5 lifelinker") {
        val d = driver()
        val psemilla = d.putCreatureOnBattlefield(d.player1, "Psemilla, Meletian Poet")
        repeat(5) { d.putPermanentOnBattlefield(d.player1, "Test Enchantment") }

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.assertStackSize(1, "the intervening-if is met, so the trigger goes on the stack")
        d.bothPass()

        val projected = d.state.projectedState
        projected.getPower(psemilla) shouldBe 5
        projected.getToughness(psemilla) shouldBe 5
        projected.hasKeyword(psemilla, Keyword.LIFELINK) shouldBe true
    }

    test("with only four enchantments, nothing happens at combat") {
        val d = driver()
        val psemilla = d.putCreatureOnBattlefield(d.player1, "Psemilla, Meletian Poet")
        repeat(4) { d.putPermanentOnBattlefield(d.player1, "Test Enchantment") }

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.state.projectedState.getPower(psemilla) shouldBe 1
        d.state.projectedState.hasKeyword(psemilla, Keyword.LIFELINK) shouldBe false
    }
})
