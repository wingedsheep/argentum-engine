package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Telekinesis (LEG #79) — "Tap target creature. Prevent all combat damage that would be dealt by that
 * creature this turn. It doesn't untap during its controller's next two untap steps."
 */
class TelekinesisScenarioTest : ScenarioTestBase() {

    init {
        context("Telekinesis") {

            test("an attacker's combat damage is prevented and it stays tapped through two untap steps") {
                val builder = scenario()
                    .withPlayers("Caster", "Attacker")
                    .withCardInHand(1, "Telekinesis")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()
                val wurm = game.findPermanent("Craw Wurm")!!
                fun tapped() = game.state.getEntity(wurm)
                    ?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() == true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Craw Wurm" to 1)).error shouldBe null
                if (game.state.priorityPlayerId == game.player2Id) game.passPriority()
                game.castSpell(1, "Telekinesis", targetId = wurm).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("the attacking Craw Wurm's combat damage was prevented") {
                    game.getLifeTotal(1) shouldBe 20
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // caster's turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("doesn't untap during its controller's first untap step") { tapped() shouldBe true }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // caster's turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("…nor its second") { tapped() shouldBe true }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // caster's turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("it untaps normally during the third") { tapped() shouldBe false }
            }
        }
    }
}
