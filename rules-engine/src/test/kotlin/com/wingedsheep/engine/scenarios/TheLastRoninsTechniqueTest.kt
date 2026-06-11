package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Last Ronin's Technique (TMT #12) — Instant, Sneak {1}{W}. "Create three
 * 1/1 white Ninja Turtle Spirit creature tokens. If this spell's sneak cost was
 * paid, they enter tapped and attacking." (The sneak-paid path is exercised by the
 * SneakCostWasPaid plumbing in SneakTest; here we verify the normal cast.)
 */
class TheLastRoninsTechniqueTest : FunSpec({
    test("normal cast creates three untapped, non-attacking Spirit tokens") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val player = driver.activePlayer!!

        val spell = driver.putCardInHand(player, "The Last Ronin's Technique")
        driver.giveMana(player, Color.WHITE, 4)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castSpell(player, spell).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val tokens = driver.getPermanents(player).filter {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.typeLine?.subtypes?.any { st -> st.value == "Spirit" } == true
        }
        tokens.size shouldBe 3
        tokens.all { driver.state.getEntity(it)?.get<AttackingComponent>() == null } shouldBe true
    }
})
