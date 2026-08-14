package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.AngryRabble
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Angry Rabble (SPM #75) — "Whenever you cast a spell with mana value 4 or greater, this creature
 * deals 1 damage to each opponent."
 *
 * Pins the caster gate: the trigger fires on the controller's MV>=4 casts (`Player.You`), NOT on an
 * opponent's. The pre-fix bug used `Player.Each` (which `matchesPlayer` treats as every player), so
 * an opponent's big spell wrongly pinged them for the Rabble's controller.
 */
class AngryRabbleScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AngryRabble)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        return driver
    }

    fun resolve(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("your MV>=4 cast deals 1 damage to each opponent") {
        val driver = createDriver()
        val you = driver.player1
        val opponent = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Angry Rabble")
        val fon = driver.putCardInHand(you, "Force of Nature") // {3}{G}{G} = mana value 5
        driver.giveMana(you, Color.GREEN, 5)

        driver.getLifeTotal(opponent) shouldBe 20
        driver.castSpell(you, fon)
        resolve(driver)

        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("an opponent's MV>=4 cast does NOT trigger it") {
        val driver = createDriver()
        val you = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(you, "Angry Rabble")

        // Advance to the opponent's main phase so they can cast a sorcery-speed creature.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe opponent

        val fon = driver.putCardInHand(opponent, "Force of Nature")
        driver.giveMana(opponent, Color.GREEN, 5)
        driver.castSpell(opponent, fon)
        resolve(driver)

        // Angry Rabble belongs to you; the opponent's cast must not trigger it.
        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(you) shouldBe 20
    }
})
