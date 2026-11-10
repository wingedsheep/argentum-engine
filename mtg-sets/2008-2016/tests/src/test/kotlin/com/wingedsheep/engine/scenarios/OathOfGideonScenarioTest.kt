package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Oath of Gideon (OGW #30) — {2}{W} Legendary Enchantment.
 *
 *   When Oath of Gideon enters, create two 1/1 white Kor Ally creature tokens.
 *   Each planeswalker you control enters with an additional loyalty counter on it.
 *
 * Pins the first global (non-self) "enters with an additional loyalty counter" replacement: the
 * counter stacks on top of the planeswalker's printed starting loyalty, and only planeswalkers
 * entering under the Oath controller's control get it.
 */
class OathOfGideonScenarioTest : ScenarioTestBase() {

    private val testWalker = card("Oath of Gideon Test Gideon") {
        manaCost = "{W}"
        typeLine = "Legendary Planeswalker — Gideon"
        startingLoyalty = 3
        oracleText = ""
    }

    init {
        cardRegistry.register(testWalker)

        fun TestGame.loyalty(name: String): Int =
            state.getEntity(findPermanent(name)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

        test("enters: creates two 1/1 white Kor Ally creature tokens") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Oath of Gideon")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Oath of Gideon").error shouldBe null
            game.resolveStack()

            val tokens = game.findPermanents("Kor Ally Token")
            tokens shouldHaveSize 2
            val projected = game.state.projectedState
            tokens.forEach { token ->
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1
                projected.getColors(token) shouldBe setOf("WHITE")
                projected.hasSubtype(token, "Kor") shouldBe true
                projected.hasSubtype(token, "Ally") shouldBe true
                projected.getController(token) shouldBe game.player1Id
            }
        }

        test("a planeswalker you control enters with one more loyalty counter than printed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Oath of Gideon")
                .withCardInHand(1, "Oath of Gideon Test Gideon")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Oath of Gideon Test Gideon").error shouldBe null
            game.resolveStack()

            withClue("printed loyalty 3 plus Oath of Gideon's additional counter") {
                game.loyalty("Oath of Gideon Test Gideon") shouldBe 4
            }
        }

        test("an opponent's Oath of Gideon doesn't add loyalty to your planeswalker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Oath of Gideon")
                .withCardInHand(1, "Oath of Gideon Test Gideon")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Oath of Gideon Test Gideon").error shouldBe null
            game.resolveStack()

            game.loyalty("Oath of Gideon Test Gideon") shouldBe 3
        }
    }
}
