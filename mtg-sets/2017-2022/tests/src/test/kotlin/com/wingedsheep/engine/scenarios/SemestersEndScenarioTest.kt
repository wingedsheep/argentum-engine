package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Semester's End (STX #27) — exile any number of target creatures and/or planeswalkers you
 * control; at the beginning of the next end step each returns with an additional +1/+1 counter if
 * it's a creature and an additional loyalty counter if it's a planeswalker.
 */
class SemestersEndScenarioTest : ScenarioTestBase() {
    init {
        test("a creature returns with a +1/+1 counter and a planeswalker with one extra loyalty") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Kasmina, Enigmatic Mentor")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Semester's End")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val kasmina = game.findPermanent("Kasmina, Enigmatic Mentor")!!
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.findCardsInHand(1, "Semester's End").single()

            withClue("an opponent's creature isn't a legal target") {
                game.execute(CastSpell(game.player1Id, spell,
                    targets = listOf(giant).map { entityIdToChosenTarget(game.state, it) })).error shouldNotBe null
            }
            val cast = game.execute(CastSpell(game.player1Id, spell,
                targets = listOf(bears, kasmina).map { entityIdToChosenTarget(game.state, it) }))
            withClue("cast with both targets: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("both are exiled on resolution") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Kasmina, Enigmatic Mentor") shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            fun counters(name: String) =
                game.state.getEntity(game.findPermanent(name)!!)!!.get<CountersComponent>()!!
            withClue("the creature returns with one +1/+1 counter and no loyalty") {
                counters("Grizzly Bears").getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                counters("Grizzly Bears").getCount(CounterType.LOYALTY) shouldBe 0
            }
            withClue("the planeswalker returns with its printed 5 loyalty plus one, and no +1/+1 counter") {
                counters("Kasmina, Enigmatic Mentor").getCount(CounterType.LOYALTY) shouldBe 6
                counters("Kasmina, Enigmatic Mentor").getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            }
        }

        test("casting with no targets does nothing") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Semester's End")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val spell = game.findCardsInHand(1, "Semester's End").single()
            game.execute(CastSpell(game.player1Id, spell, targets = emptyList())).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
