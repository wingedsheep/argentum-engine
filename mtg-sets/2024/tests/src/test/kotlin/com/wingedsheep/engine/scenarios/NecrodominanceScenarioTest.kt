package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.player.CantLoseLifeComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Necrodominance (MH3) — "At the beginning of your end step, you may pay any amount of life. If you
 * do, draw that many cards." Proves `Gate.MayPayAnyAmountOfLife`: a 0..life-total chooser whose
 * answer is paid and then read back as X by the draw. Also pins the skipped draw step.
 */
class NecrodominanceScenarioTest : ScenarioTestBase() {

    private fun necrodominanceGame(life: Int = 20) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Necrodominance")
        .withLifeTotal(1, life)
        .apply { repeat(6) { withCardInLibrary(1, "Swamp") } }
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Necrodominance — pay any amount of life at your end step") {

            test("paying 3 life draws three cards") {
                val game = necrodominanceGame()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseNumberDecision>()
                withClue("the ceiling is the controller's life total") {
                    decision.minValue shouldBe 0
                    decision.maxValue shouldBe 20
                }
                game.chooseNumber(3).error shouldBe null

                game.getLifeTotal(1) shouldBe 17
                game.handSize(1) shouldBe 3
            }

            test("the ceiling follows the current life total") {
                val game = necrodominanceGame(life = 4)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseNumberDecision>()
                decision.maxValue shouldBe 4
            }

            test("choosing 0 pays nothing and draws nothing") {
                val game = necrodominanceGame()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.chooseNumber(0).error shouldBe null

                game.getLifeTotal(1) shouldBe 20
                game.handSize(1) shouldBe 0
            }

            test("a player who can't lose life can't pay life, so isn't asked and draws nothing") {
                val game = necrodominanceGame()
                game.state = game.state.updateEntity(game.player1Id) { it.with(CantLoseLifeComponent()) }
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("no payable life means no prompt (CR 119.8)") {
                    game.getPendingDecision() shouldBe null
                }
                game.getLifeTotal(1) shouldBe 20
                game.handSize(1) shouldBe 0
            }

            test("a card that would go to your graveyard is exiled instead; an opponent's is not") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Necrodominance")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                game.isInExile(1, "Lightning Bolt") shouldBe true
                withClue("only your own graveyard is replaced") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("skip your draw step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Necrodominance")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.handSize(1) shouldBe 0
            }
        }
    }
}
