package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Solemnity (HOU #22) — {2}{W} Enchantment.
 *
 *   Players can't get counters.
 *   Counters can't be put on artifacts, creatures, enchantments, or lands.
 *
 * The rules matrix of the player half lives in the engine's `PlayersCantGetCountersTest`; this pins
 * the card: players and creatures get nothing, a noncreature planeswalker still enters with its
 * loyalty, and an undying creature keeps coming back because it never gets its +1/+1 counter.
 */
class SolemnityScenarioTest : ScenarioTestBase() {

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        test("no player gets poison counters and no creature gets a +1/+1 counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Solemnity")
                .withCardInHand(1, "Ichor Rats")
                .withCardInHand(1, "Pollenbright Druid")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Ichor Rats").error shouldBe null
            game.resolveStack()
            game.resolveStack()
            withClue("Ichor Rats' \"each player gets a poison counter\" gives nobody one") {
                count(game, game.player1Id, CounterType.POISON) shouldBe 0
                count(game, game.player2Id, CounterType.POISON) shouldBe 0
            }

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Pollenbright Druid").error shouldBe null
            game.resolveStack()
            game.chooseMode(0)
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()
            count(game, giant, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        }

        test("a noncreature planeswalker still enters with its loyalty counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Solemnity")
                .withCardInHand(1, "Garruk Wildspeaker")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Garruk Wildspeaker").error shouldBe null
            game.resolveStack()
            val garruk = game.findPermanent("Garruk Wildspeaker")!!
            count(game, garruk, CounterType.LOYALTY) shouldBe 3
        }

        test("an undying creature returns without a +1/+1 counter, so it returns again") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Solemnity")
                .withCardOnBattlefield(2, "Evernight Shade")
                .withCardsInHand(1, "Lightning Bolt", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            repeat(2) { round ->
                val shade = game.findPermanent("Evernight Shade")!!
                game.castSpell(1, "Lightning Bolt", shade).error shouldBe null
                game.resolveStack()
                game.resolveStack()
                val returned = game.findPermanent("Evernight Shade")
                withClue("undying brings it back after death ${round + 1}") { returned shouldNotBe null }
                count(game, returned!!, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            }
        }
    }

    private fun TestGame.chooseMode(index: Int) {
        val decision = getPendingDecision()
        submitDecision(
            com.wingedsheep.engine.core.OptionChosenResponse(decision!!.id, index)
        ).error shouldBe null
    }
}
