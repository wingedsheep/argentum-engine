package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mischief and Mayhem (BNG #126) — "Up to two target creatures each get +4/+4 until end of turn."
 */
class MischiefAndMayhemScenarioTest : ScenarioTestBase() {
    init {
        test("each of two target creatures gets +4/+4") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Mischief and Mayhem")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.state.getHand(game.player1Id).single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Mischief and Mayhem"
            }

            game.execute(
                CastSpell(game.player1Id, spell, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Grizzly Bears 2/2 -> 6/6") {
                projected.getPower(bears) shouldBe 6
                projected.getToughness(bears) shouldBe 6
            }
            withClue("Hill Giant 3/3 -> 7/7") {
                projected.getPower(giant) shouldBe 7
                projected.getToughness(giant) shouldBe 7
            }
        }
    }
}
