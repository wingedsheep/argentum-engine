package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skeletal Swarming — {3}{B}{G} Enchantment (AFR #232).
 *
 * "Each Skeleton you control has trample, attacks each combat if able, and gets +X/+0, where X is
 * the number of other Skeletons you control. At the beginning of your end step, create a tapped
 * 1/1 black Skeleton creature token. If a creature died this turn, create two of those tokens
 * instead."
 */
class SkeletalSwarmingScenarioTest : ScenarioTestBase() {

    init {
        test("each Skeleton counts the other Skeletons you control, not itself or an opponent's") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Skeletal Swarming")
                .withCardOnBattlefield(1, "Clattering Skeletons")
                .withCardOnBattlefield(1, "Clattering Skeletons")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Clattering Skeletons")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = game.state.projectedState
            val mine = game.findAllPermanents("Clattering Skeletons")
                .filter { game.state.getBattlefield(game.player1Id).contains(it) }
            val theirs = game.findAllPermanents("Clattering Skeletons").single { it !in mine }
            mine.size shouldBe 2
            for (skeleton in mine) {
                withClue("4/3 plus one other Skeleton you control") {
                    projected.getPower(skeleton) shouldBe 5
                    projected.getToughness(skeleton) shouldBe 3
                    projected.hasKeyword(skeleton, Keyword.TRAMPLE) shouldBe true
                }
            }
            withClue("an opponent's Skeleton gets nothing") {
                projected.getPower(theirs) shouldBe 4
                projected.hasKeyword(theirs, Keyword.TRAMPLE) shouldBe false
            }
            withClue("a non-Skeleton gets nothing") {
                val bears = game.findPermanent("Grizzly Bears")!!
                projected.getPower(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
            }
        }

        test("end step makes one tapped Skeleton when nothing died") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Skeletal Swarming")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val tokens = game.findAllPermanents("Skeleton Token")
            tokens.size shouldBe 1
            val token = tokens.single()
            game.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
            withClue("a lone Skeleton has no other Skeletons to count") {
                game.state.projectedState.getPower(token) shouldBe 1
            }
        }

        test("end step makes two tapped Skeletons if a creature died this turn") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardOnBattlefield(1, "Skeletal Swarming")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            game.castSpell(1, "Doom Blade", targetId = game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val tokens = game.findAllPermanents("Skeleton Token")
            tokens.size shouldBe 2
            for (token in tokens) {
                game.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
                withClue("each token counts the other one") {
                    game.state.projectedState.getPower(token) shouldBe 2
                }
            }
        }
    }
}
