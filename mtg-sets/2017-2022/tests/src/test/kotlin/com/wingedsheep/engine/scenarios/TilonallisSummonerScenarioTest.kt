package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tilonalli's Summoner (RIX #121) — "Whenever this creature attacks, you may pay {X}{R}. If you
 * do, create X 1/1 red Elemental creature tokens that are tapped and attacking. At the beginning
 * of the next end step, exile those tokens unless you have the city's blessing."
 *
 * Exercises `Gate.MayPayX(additionalCost = {R})`: the {R} is paid with X in one payment (so the
 * X ceiling is what's left after it, and X = 0 still costs {R}), and an unaffordable {R} skips the
 * prompt. The end-step exile checks the city's blessing only as it resolves.
 */
class TilonallisSummonerScenarioTest : ScenarioTestBase() {

    private fun TestGame.elementals() = findAllPermanents("Elemental Token")

    private fun TestGame.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    private fun TestGame.untappedMountains() =
        findAllPermanents("Mountain").count { !isTapped(it) }

    private fun TestGame.attackAndResolveTrigger() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Tilonalli's Summoner" to 2)).error shouldBe null
        resolveStack()
    }

    init {
        test("paying {X}{R} with X = 2 makes two attacking Elementals, exiled at the end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tilonalli's Summoner")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.attackAndResolveTrigger()
            val ask = game.getPendingDecision()
            ask.shouldBeInstanceOf<YesNoDecision>()
            ask.yesText shouldBe "Pay {X}{R}"
            game.answerYesNo(true).error shouldBe null

            val number = game.getPendingDecision()
            number.shouldBeInstanceOf<ChooseNumberDecision>()
            withClue("four Mountains, one reserved for {R}, leave X up to 3") {
                number.maxValue shouldBe 3
            }
            game.chooseNumber(2).error shouldBe null

            val tokens = game.elementals()
            tokens.size shouldBe 2
            tokens.forEach { token ->
                game.isTapped(token) shouldBe true
                game.state.getEntity(token)?.has<AttackingComponent>() shouldBe true
            }
            withClue("X = 2 plus {R} taps three Mountains") { game.untappedMountains() shouldBe 1 }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.elementals().size shouldBe 0
        }

        test("X = 0 still pays {R} and makes no tokens") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tilonalli's Summoner")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.attackAndResolveTrigger()
            game.answerYesNo(true).error shouldBe null
            game.chooseNumber(0).error shouldBe null

            game.elementals().size shouldBe 0
            game.untappedMountains() shouldBe 1
        }

        test("with only {R} available, yes pays it as X = 0 without asking for a number") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tilonalli's Summoner")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.attackAndResolveTrigger()
            game.answerYesNo(true).error shouldBe null

            game.hasPendingDecision() shouldBe false
            game.untappedMountains() shouldBe 0
        }

        test("without red mana the {X}{R} can't be paid, so nothing is asked") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tilonalli's Summoner")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.attackAndResolveTrigger()

            game.hasPendingDecision() shouldBe false
            game.elementals().size shouldBe 0
        }

        test("declining pays nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tilonalli's Summoner")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.attackAndResolveTrigger()
            game.answerYesNo(false).error shouldBe null

            game.elementals().size shouldBe 0
            game.untappedMountains() shouldBe 3
        }

        test("the tokens stay if you have the city's blessing as the end-step trigger resolves") {
            // Summoner + 7 Mountains = 8 permanents; the two tokens make ten, so the Summoner's
            // ascend grants the blessing after they're created — checked only at resolution.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tilonalli's Summoner")
                .withLandsOnBattlefield(1, "Mountain", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.attackAndResolveTrigger()
            game.answerYesNo(true).error shouldBe null
            game.chooseNumber(2).error shouldBe null
            game.elementals().size shouldBe 2

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.elementals().size shouldBe 2
        }
    }
}
