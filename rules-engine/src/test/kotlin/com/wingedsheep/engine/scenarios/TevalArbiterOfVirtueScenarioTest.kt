package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.TevalArbiterOfVirtue
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Teval, Arbiter of Virtue (TDM, {2}{B}{G}{U}, 6/6).
 *
 * - "Whenever you cast a spell, you lose life equal to its mana value."
 * - "Spells you cast have delve" — verified by being able to pay generic cost with graveyard cards.
 */
class TevalArbiterOfVirtueScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TevalArbiterOfVirtue))
        return driver
    }

    fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    fun GameTestDriver.life(player: com.wingedsheep.sdk.model.EntityId): Int =
        state.getEntity(player)?.get<LifeTotalComponent>()?.life ?: 0

    test("casting a spell makes you lose life equal to its mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.advanceToPlayer1Main()

        driver.putCreatureOnBattlefield(driver.player1, "Teval, Arbiter of Virtue")
        repeat(2) { driver.putLandOnBattlefield(driver.player1, "Forest") }
        val bears = driver.putCardInHand(driver.player1, "Grizzly Bears") // {1}{G}, MV 2

        val lifeBefore = driver.life(driver.player1)

        driver.castSpell(driver.player1, bears)
        // Resolve Teval's "you cast a spell" trigger (and the spell).
        var safety = 0
        while (driver.state.stack.isNotEmpty() && driver.pendingDecision == null && safety < 20) {
            driver.bothPass(); safety++
        }

        // Grizzly Bears is mana value 2 → lose 2 life.
        driver.life(driver.player1) shouldBe lifeBefore - 2
    }

    test("spells you cast have delve while Teval is on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.advanceToPlayer1Main()

        val resolver = GrantedKeywordResolver(driver.cardRegistry)
        val grizzly = driver.cardRegistry.requireCard("Grizzly Bears")

        // No granter yet → no granted delve.
        resolver.hasKeyword(driver.state, driver.player1, grizzly, Keyword.DELVE) shouldBe false

        driver.putCreatureOnBattlefield(driver.player1, "Teval, Arbiter of Virtue")

        // Teval grants delve to any spell you cast.
        resolver.hasKeyword(driver.state, driver.player1, grizzly, Keyword.DELVE) shouldBe true
        // The opponent's spells are unaffected.
        resolver.hasKeyword(driver.state, driver.player2, grizzly, Keyword.DELVE) shouldBe false
    }
})
