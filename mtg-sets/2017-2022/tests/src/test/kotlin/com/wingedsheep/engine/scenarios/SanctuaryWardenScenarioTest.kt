package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sanctuary Warden (SNC #30).
 *
 * "Whenever this creature enters or attacks, you may remove a counter from a creature or
 * planeswalker you control. If you do, draw a card and create a 1/1 green and white Citizen
 * creature token." The payoff is gated on a counter actually coming off: accepting removes one of
 * the Warden's own entry shield counters and pays out; declining leaves both shields and pays
 * nothing; with no counters anywhere the may-question is never asked.
 */
class SanctuaryWardenScenarioTest : ScenarioTestBase() {

    private fun TestGame.shields(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) ?: 0

    private fun TestGame.citizens(): Int =
        findPermanents("Citizen Token").size + findPermanents("Citizen").size

    private fun castWarden(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Sanctuary Warden")
            .withLandsOnBattlefield(1, "Plains", 6)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Sanctuary Warden").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("enters with two shield counters; removing one draws a card and makes a Citizen") {
            val game = castWarden()
            val warden = game.findPermanent("Sanctuary Warden")!!
            withClue("the enters trigger asks before anything is removed") {
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.shields(warden) shouldBe 2
            }

            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.shields(warden) shouldBe 1
            game.handSize(1) shouldBe 1
            game.citizens() shouldBe 1
        }

        test("declining keeps both shield counters and pays nothing") {
            val game = castWarden()
            val warden = game.findPermanent("Sanctuary Warden")!!

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.shields(warden) shouldBe 2
            game.handSize(1) shouldBe 0
            game.citizens() shouldBe 0
        }

        test("attacking with no counters anywhere asks nothing and pays nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sanctuary Warden")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val warden = game.findPermanent("Sanctuary Warden")!!
            withClue("placed directly, so no entry shields") { game.shields(warden) shouldBe 0 }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Sanctuary Warden" to 2)).error shouldBe null
            game.resolveStack()

            withClue("no permanent you control carries a counter, so the may-question is skipped") {
                game.getPendingDecision() shouldBe null
            }
            game.handSize(1) shouldBe 0
            game.citizens() shouldBe 0
        }
    }
}
