package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Druid Class (AFR #180).
 *
 * Pins the two class abilities whose correctness depends on how the engine reads the script:
 * level 2's extra land drop is a static inside a `classLevel` block (the land-drop reader must
 * see class-level statics), and level 3's animate is a permanent `BecomeCreature` whose P/T
 * tracks the number of lands you control.
 */
class DruidClassScenarioTest : ScenarioTestBase() {

    init {
        test("level 1 only allows one land drop; landfall gains 1 life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Druid Class", classLevel = 1)
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (first, second) = game.findCardsInHand(1, "Forest")
            game.execute(PlayLand(game.player1Id, first)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21

            withClue("Level 1 grants no extra land drop") {
                game.execute(PlayLand(game.player1Id, second)).error shouldNotBe null
            }
        }

        test("level 2 lets you play an additional land, and each land triggers landfall") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Druid Class", classLevel = 2)
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (first, second) = game.findCardsInHand(1, "Forest")
            game.execute(PlayLand(game.player1Id, first)).error shouldBe null
            game.resolveStack()
            withClue("Level 2 grants a second land drop") {
                game.execute(PlayLand(game.player1Id, second)).error shouldBe null
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22
        }

        test("becoming level 3 animates target land with haste and P/T equal to lands you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Druid Class", classLevel = 2)
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInHand(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val classId = game.findPermanent("Druid Class").shouldNotBeNull()
            game.execute(
                ActivateAbility(game.player1Id, classId, AbilityId.classLevelUp(3))
            ).error shouldBe null
            game.resolveStack()

            val land = game.findPermanents("Forest").first()
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(land)).error shouldBe null
            }
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("The land is now a creature and still a land") {
                projected.isCreature(land) shouldBe true
                projected.hasType(land, "LAND") shouldBe true
            }
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 5
            projected.getToughness(land) shouldBe 5

            withClue("P/T grows as you get more lands") {
                val forest = game.findCardsInHand(1, "Forest").single()
                game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getPower(land) shouldBe 6
                game.state.projectedState.getToughness(land) shouldBe 6
            }
        }
    }
}
