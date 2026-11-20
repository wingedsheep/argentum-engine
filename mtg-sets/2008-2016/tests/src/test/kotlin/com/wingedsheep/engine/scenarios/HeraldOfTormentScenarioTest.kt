package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Herald of Torment: "At the beginning of your upkeep, you lose 1 life" is the Herald's own
 * trigger, so it fires for its controller whether it is a 3/3 flyer or bestowed as an Aura
 * (whose host gets +3/+3 and flying).
 */
class HeraldOfTormentScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature, its controller loses 1 life at the beginning of their upkeep") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Herald of Torment")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
        }

        test("bestowed, the host gets +3/+3 and flying and the controller still loses 1 on their own upkeep") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Herald of Torment")
                .withLandsOnBattlefield(1, "Swamp", 5)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Herald of Torment").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val herald = game.findPermanent("Herald of Torment")!!
            game.state.getEntity(herald)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.isCreature(herald) shouldBe false
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 5
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true

            withClue("the opponent's upkeep is not 'your upkeep'") {
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            withClue("the Aura's controller loses 1 at their own upkeep") {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 19
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
