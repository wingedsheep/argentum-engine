package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dáin Ironfoot — ETB mints the Axe and a reflexive trigger attaches it to a targeted creature;
 * his attack trigger hands double strike to every equipped attacking creature.
 *
 * The reflexive half is the interesting wiring: the attach goes on the stack as its own ability
 * (CR 603.12) and has to still be able to name the token the *action* half created, which it does
 * through the carried `CREATED_TOKENS` pipeline collection.
 */
class DainIronfootScenarioTest : ScenarioTestBase() {

    init {
        context("Dáin Ironfoot") {

            test("the ETB creates an Axe and the reflexive trigger attaches it to the chosen creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dáin Ironfoot")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dáin Ironfoot").error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                if (game.hasPendingDecision()) game.selectTargets(listOf(bears))
                game.resolveStack()

                val axe = game.findPermanent("Axe")
                withClue("the Axe token was created") { (axe != null) shouldBe true }
                withClue("and the reflexive trigger attached it to the targeted creature") {
                    game.state.getEntity(axe!!)!!.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("so the Bears is a 3/2") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }

            test("attacking gives double strike to equipped attackers but not to unequipped ones") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dáin Ironfoot", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    // Bonesplitter is a plain static-only Equipment, so nothing but the attach
                    // matters here.
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Dáin Ironfoot" to 2, "Grizzly Bears" to 2, "Hill Giant" to 2))
                game.resolveStack()

                withClue("the equipped attacker gained double strike") {
                    game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
                }
                withClue("the unequipped attacker did not") {
                    game.state.projectedState.hasKeyword(giant, Keyword.DOUBLE_STRIKE) shouldBe false
                }
            }
        }
    }
}
