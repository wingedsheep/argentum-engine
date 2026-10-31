package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.WitchingWell
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Witching Well (ELD #74) — {U} Artifact.
 *
 * "When this artifact enters, scry 2. {3}{U}, Sacrifice this artifact: Draw two cards."
 */
class WitchingWellScenarioTest : ScenarioTestBase() {

    private val drawAbility = WitchingWell.activatedAbilities[0].id

    init {
        context("Witching Well") {

            test("entering the battlefield scries 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Witching Well")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Witching Well").error shouldBe null
                game.resolveStack()
                game.resolveStack()

                game.isOnBattlefield("Witching Well") shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.options shouldHaveSize 2
            }

            test("paying {3}{U} and sacrificing it draws two cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Witching Well")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val well = game.findPermanent("Witching Well")!!
                val handBefore = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, well, drawAbility)).error shouldBe null
                game.isInGraveyard(1, "Witching Well") shouldBe true
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 2
            }
        }
    }
}
