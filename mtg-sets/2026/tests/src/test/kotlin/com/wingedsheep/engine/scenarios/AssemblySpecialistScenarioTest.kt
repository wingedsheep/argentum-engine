package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Assembly Specialist ({1}{W} Artifact Creature — Borg, 1/1):
 * "When this creature enters, create a 1/1 colorless Borg artifact creature token."
 */
class AssemblySpecialistScenarioTest : ScenarioTestBase() {

    init {
        test("entering creates a 1/1 colorless Borg artifact creature token") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Assembly Specialist")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Assembly Specialist").error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val borg = projected.getBattlefieldControlledBy(game.player1Id).filter { projected.hasSubtype(it, "Borg") }
            borg.size shouldBe 2
            val token = borg.single { game.state.getEntity(it)?.get<CardComponent>()?.name != "Assembly Specialist" }
            projected.isCreature(token) shouldBe true
            projected.hasType(token, "ARTIFACT") shouldBe true
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.getColors(token).shouldBeEmpty()
        }
    }
}
