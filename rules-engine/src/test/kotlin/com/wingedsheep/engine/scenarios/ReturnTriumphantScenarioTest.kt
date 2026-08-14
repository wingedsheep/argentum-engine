package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Return Triumphant. */
class ReturnTriumphantScenarioTest : ScenarioTestBase() {

    private val outriderAbilityId by lazy {
        cardRegistry.requireCard("Verdant Outrider").activatedAbilities[0].id
    }

    init {
        context("Return Triumphant — reanimate, then crown the same creature") {
            test("the returned creature enters and gets a Young Hero Role attached to it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Return Triumphant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Return Triumphant", 1, "Grizzly Bears")
                game.resolveStack()

                val bear = game.findPermanent("Grizzly Bears")
                withClue("mana value 2 <= 3, so the Bears come back") { bear shouldNotBe null }
                withClue("it left the graveyard") { game.isInGraveyard(1, "Grizzly Bears") shouldBe false }

                val role = game.findPermanent("Young Hero Role")
                withClue("the Role token was created") { role shouldNotBe null }
                withClue("attached to the creature this spell just reanimated") {
                    game.state.getEntity(role!!)?.get<AttachedToComponent>()?.targetId shouldBe bear
                }
                withClue("the Young Hero Role is stats-neutral — the Bears are still 2/2") {
                    game.state.projectedState.getPower(bear!!) shouldBe 2
                    game.state.projectedState.getToughness(bear) shouldBe 2
                }
            }
        }
    }
}
