package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.BoldImpaler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bold Impaler (Eldritch Moon #120) — {R} Creature — Vampire Knight, 1/2.
 *
 * "{2}{R}: This creature gets +2/+0 until end of turn."
 */
class BoldImpalerScenarioTest : FunSpec({

    val abilityId = BoldImpaler.activatedAbilities.first().id

    test("{2}{R}: gets +2/+0 until end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.player1

        val impaler = driver.putCreatureOnBattlefield(player, "Bold Impaler")
        driver.giveMana(player, Color.RED, 3) // {2}{R}

        driver.submitSuccess(ActivateAbility(playerId = player, sourceId = impaler, abilityId = abilityId))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.state.projectedState.getPower(impaler) shouldBe 3
        driver.state.projectedState.getToughness(impaler) shouldBe 2
    }
})
