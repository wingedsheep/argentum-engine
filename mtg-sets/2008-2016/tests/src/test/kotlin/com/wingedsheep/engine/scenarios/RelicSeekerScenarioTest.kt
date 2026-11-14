package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Relic Seeker (ORI #29) — {1}{W} Creature — Human Soldier 2/2.
 *
 *   Renown 1
 *   When this creature becomes renowned, you may search your library for an Equipment card,
 *   reveal it, put it into your hand, then shuffle.
 *
 * The first card to use `Triggers.self.becomesRenowned()`: the renown trigger resolves, emits
 * `BecameRenownedEvent`, and that event fires the tutor.
 */
class RelicSeekerScenarioTest : ScenarioTestBase() {

    private fun connect(alreadyRenowned: Boolean = false): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Relic Seeker")
            .withCardInLibrary(1, "Veteran's Sidearm")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        if (alreadyRenowned) {
            val seeker = game.findPermanent("Relic Seeker")!!
            game.state = game.state.updateEntity(seeker) { it.with(RenownedComponent) }
        }

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Relic Seeker" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers()
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        if (!alreadyRenowned) {
            var guard = 0
            while (game.state.pendingDecision !is YesNoDecision && guard++ < 20) game.resolveStack()
        }
        return game
    }

    init {
        test("becoming renowned lets you tutor an Equipment card to hand") {
            val game = connect()
            val seeker = game.findPermanent("Relic Seeker")!!

            withClue("renown resolved first: one +1/+1 counter and the designation") {
                game.state.getEntity(seeker)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.getEntity(seeker)?.has<RenownedComponent>() shouldBe true
            }

            game.answerYesNo(true).error shouldBe null
            val sidearm = game.findCardsInLibrary(1, "Veteran's Sidearm").single()
            game.selectCards(listOf(sidearm)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Veteran's Sidearm") shouldBe true
            withClue("the non-Equipment card stays in the library") {
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }
        }

        test("declining the search leaves the Equipment in the library") {
            val game = connect()

            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Veteran's Sidearm") shouldBe false
            game.findCardsInLibrary(1, "Veteran's Sidearm").size shouldBe 1
        }

        test("an already-renowned Seeker connecting again does not trigger the tutor") {
            val game = connect(alreadyRenowned = true)
            game.resolveStack()

            withClue("no renown trigger, so no counter and no search offer") {
                (game.state.pendingDecision is YesNoDecision) shouldBe false
                val counters = game.state.getEntity(game.findPermanent("Relic Seeker")!!)
                    ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                counters shouldBe 0
            }
            game.isInHand(1, "Veteran's Sidearm") shouldBe false
        }
    }
}
