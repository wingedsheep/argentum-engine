package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Relentless Assault (P02 #115) — untaps only the creatures that attacked this turn, then queues an
 * additional combat phase followed by an additional main phase.
 */
class RelentlessAssaultScenarioTest : ScenarioTestBase() {
    init {
        test("untaps attackers only and queues an extra combat and main") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                .withCardInHand(1, "Relentless Assault")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("attacker is tapped before the spell") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }
            game.castSpell(1, "Relentless Assault").error shouldBe null
            game.resolveStack()

            withClue("the attacker untaps") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            }
            withClue("a creature that did not attack stays tapped") {
                game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
            }
            val queued = game.state.getEntity(game.state.activePlayerId!!)
                ?.get<AdditionalPhasesComponent>()?.phases.orEmpty().map { it.kind }
            withClue("extra combat then extra main queued") {
                queued shouldBe listOf(ExtraPhaseKind.COMBAT, ExtraPhaseKind.MAIN)
            }
        }
    }
}
