package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Lena, Selfless Champion (M19 #21).
 *
 * Pins the two rulings that depend on how the engine executes the script: the enters trigger
 * counts nontoken creatures (Lena included, tokens excluded) as it resolves, and the sacrifice
 * ability compares against Lena's last-known power — counters included — not her printed 3.
 */
class LenaSelflessChampionScenarioTest : ScenarioTestBase() {

    init {
        test("enters trigger makes a Soldier for each nontoken creature you control, Lena included") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Lena, Selfless Champion")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant", isToken = true)
                .withCardOnBattlefield(2, "Craw Wurm")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lena, Selfless Champion").error shouldBe null
            game.resolveStack()

            // Lena + Grizzly Bears; the token Hill Giant and the opponent's Craw Wurm don't count.
            game.findPermanents("Soldier Token").size shouldBe 2
        }

        test("sacrificing Lena uses her last-known power, counters included") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Lena, Selfless Champion")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lena = game.findPermanent("Lena, Selfless Champion")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            val opposingBears = game.findPermanent("Grizzly Bears")!!
            // Two +1/+1 counters: Lena is a 5/5, so the 3-power Hill Giant qualifies only if her
            // last-known power is read rather than her printed 3.
            game.state = game.state.updateEntity(lena) { c ->
                c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
            }

            val abilityId = cardRegistry.getCard("Lena, Selfless Champion")!!.activatedAbilities.first().id
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = lena, abilityId = abilityId)
            ).error shouldBe null
            game.isOnBattlefield("Lena, Selfless Champion") shouldBe false
            game.resolveStack()

            val projected = game.state.projectedState
            projected.hasKeyword(giant, Keyword.INDESTRUCTIBLE) shouldBe true
            projected.hasKeyword(wurm, Keyword.INDESTRUCTIBLE) shouldBe false
            projected.hasKeyword(opposingBears, Keyword.INDESTRUCTIBLE) shouldBe false
        }
    }
}
