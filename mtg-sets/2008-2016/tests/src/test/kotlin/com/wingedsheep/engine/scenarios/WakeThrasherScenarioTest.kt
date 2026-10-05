package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eve.cards.WakeThrasher
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Wake Thrasher (EVE) — "Whenever a permanent you control becomes untapped, this creature gets
 * +1/+1 until end of turn."
 */
class WakeThrasherScenarioTest : ScenarioTestBase() {

    private val unwind = card("Unwind Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Untap target permanent."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.Untap(t)
        }
    }

    private fun game(bearsOwner: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Wake Thrasher")
            .withCardOnBattlefield(bearsOwner, "Grizzly Bears", tapped = true)
            .withCardInHand(1, "Unwind Test")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(5) {
            builder.withCardInLibrary(1, "Island")
            builder.withCardInLibrary(2, "Island")
        }
        return builder.build()
    }

    init {
        cardRegistry.register(WakeThrasher)
        cardRegistry.register(GrizzlyBears)
        cardRegistry.register(unwind)

        context("Wake Thrasher") {
            test("a permanent you control untapping pumps it +1/+1") {
                val game = game(bearsOwner = 1)
                val thrasher = game.findPermanent("Wake Thrasher")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Unwind Test", targetId = bears).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(thrasher) shouldBe 2
                game.state.projectedState.getToughness(thrasher) shouldBe 2
            }

            test("an opponent's permanent untapping does not trigger it") {
                val game = game(bearsOwner = 2)
                val thrasher = game.findPermanent("Wake Thrasher")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Unwind Test", targetId = bears).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(thrasher) shouldBe 1
                game.state.projectedState.getToughness(thrasher) shouldBe 1
            }
        }
    }
}
