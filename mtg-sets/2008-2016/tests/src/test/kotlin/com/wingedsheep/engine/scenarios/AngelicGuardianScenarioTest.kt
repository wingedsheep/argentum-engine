package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Angelic Guardian (PZ2, reprinted in M20 and ANB) — {4}{W}{W} Creature — Angel 5/5, Flying.
 *
 * "Whenever one or more creatures you control attack, they gain indestructible until end of turn."
 *
 * "They" is the batch of declared attackers (`attackedThisCombat()`): attacking creatures gain
 * indestructible, creatures that stayed home don't.
 */
class AngelicGuardianScenarioTest : ScenarioTestBase() {

    private val name = "Angelic Guardian"

    init {
        test("the creatures that attacked gain indestructible; a creature that stayed home does not") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, name, summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(2, "Savannah Lions", summoningSickness = false)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val guardian = game.findPermanent(name)!!
            val lions = game.findPermanent("Savannah Lions")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the attacking Grizzly Bears gains indestructible") {
                projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            }
            withClue("non-attackers — including Angelic Guardian itself — gain nothing") {
                projected.hasKeyword(giant, Keyword.INDESTRUCTIBLE) shouldBe false
                projected.hasKeyword(guardian, Keyword.INDESTRUCTIBLE) shouldBe false
                projected.hasKeyword(lions, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }

        test("Angelic Guardian attacking alongside others gains indestructible too") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, name, summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(name to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.hasKeyword(game.findPermanent(name)!!, Keyword.INDESTRUCTIBLE) shouldBe true
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }
}
