package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for simultaneous loss scenarios where the game ends in a draw.
 *
 * Rule 104.4a: If all the players remaining in a game lose simultaneously, the game is a draw.
 */
class SimultaneousLossDrawTest : FunSpec({

    test("game ends in draw when spell kills both players simultaneously (Rule 104.4a)") {
        // Test card that deals X damage to all players (simplified Earthquake)
        val MassiveFire = CardDefinition.sorcery(
            name = "Massive Fire",
            manaCost = ManaCost.parse("{X}{R}"),
            oracleText = "Massive Fire deals X damage to each player.",
            script = CardScript.spell(
                effect = DealDamageToPlayersEffect(DynamicAmount.XValue)
            )
        )

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MassiveFire))

        // Both players start with only 5 life
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20),
            skipMulligans = true,
            startingLife = 5
        )

        val player1 = driver.player1!!
        val player2 = driver.player2!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put MassiveFire in hand and give mana for X=5
        val massiveFire = driver.putCardInHand(player1, "Massive Fire")
        driver.giveMana(player1, Color.RED, 6) // 1R + X=5

        // Cast MassiveFire with X=5 (deals 5 damage to each player)
        driver.castXSpell(player1, massiveFire, 5)
        driver.bothPass()

        // Game should be over
        driver.state.gameOver.shouldBeTrue()

        // Winner should be null (draw) because both players took lethal damage
        driver.state.winnerId shouldBe null

        // Both players should have PlayerLostComponent with LIFE_ZERO reason
        val player1Lost = driver.state.getEntity(player1)?.get<PlayerLostComponent>()
        val player2Lost = driver.state.getEntity(player2)?.get<PlayerLostComponent>()

        player1Lost shouldNotBe null
        player1Lost!!.reason shouldBe LossReason.LIFE_ZERO

        player2Lost shouldNotBe null
        player2Lost!!.reason shouldBe LossReason.LIFE_ZERO
    }
})
