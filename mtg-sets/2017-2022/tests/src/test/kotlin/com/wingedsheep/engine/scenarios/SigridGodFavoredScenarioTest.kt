package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.SigridGodFavored
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sigrid, God-Favored: flashed in during the opponent's combat, her ETB exiles an attacking
 * creature until she leaves the battlefield. Her "protection from God creatures" is covered by
 * the engine's TypeQualityProtectionTest.
 */
class SigridGodFavoredScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SigridGodFavored)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("flashed in during combat, exiles an attacker until Sigrid leaves") {
        val driver = createDriver()
        val attackingPlayer = driver.activePlayer!!
        val you = driver.getOpponent(attackingPlayer)

        val attacker = driver.putCreatureOnBattlefield(attackingPlayer, "Glory Seeker")
        driver.removeSummoningSickness(attacker)
        val sigridCard = driver.putCardInHand(you, "Sigrid, God-Favored")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attackingPlayer, listOf(attacker), you)
        driver.passPriority(attackingPlayer)

        driver.giveMana(you, Color.WHITE, 3)
        driver.castSpell(you, sigridCard)
        driver.bothPass() // Sigrid resolves -> ETB trigger
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(you, listOf(attacker))
        driver.bothPass() // ETB resolves

        driver.getExileCardNames(attackingPlayer) shouldContain "Glory Seeker"
        val sigrid = driver.findPermanent(you, "Sigrid, God-Favored")
        sigrid shouldNotBe null

        val doomBlade = driver.putCardInHand(attackingPlayer, "Doom Blade")
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.giveMana(attackingPlayer, Color.BLACK, 2)
        driver.castSpellWithTargets(attackingPlayer, doomBlade, listOf(ChosenTarget.Permanent(sigrid!!)))
            .outcome shouldBe Outcome.Done
        driver.bothPass() // Doom Blade resolves; the exiled creature returns immediately

        driver.findPermanent(attackingPlayer, "Glory Seeker") shouldNotBe null
        driver.getExileCardNames(attackingPlayer) shouldNotContain "Glory Seeker"
    }

    test("the ETB may target nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val sigridCard = driver.putCardInHand(you, "Sigrid, God-Favored")

        driver.giveMana(you, Color.WHITE, 3)
        driver.castSpell(you, sigridCard)
        driver.bothPass()

        driver.findPermanent(you, "Sigrid, God-Favored") shouldNotBe null
        driver.state.stack.isEmpty() shouldBe true
    }
})
