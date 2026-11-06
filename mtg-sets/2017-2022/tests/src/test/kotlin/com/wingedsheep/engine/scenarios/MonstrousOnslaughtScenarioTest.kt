package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Monstrous Onslaught (AER #116) — "deals X damage divided as you choose among any number of target
 * creatures, where X is the greatest power among creatures you control as you cast this spell."
 *
 * X is frozen as the spell is cast (CR 601.2i) and the division is announced at cast (CR 601.2d):
 * shrinking your board in response doesn't shrink X (per the rulings).
 */
class MonstrousOnslaughtScenarioTest : ScenarioTestBase() {

    init {
        context("Monstrous Onslaught") {

            // Alice's greatest power is Hill Giant's 3: X = 3.
            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Monstrous Onslaught")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.cast(targets: List<EntityId>, division: Map<EntityId, Int>? = null) = execute(
                CastSpell(
                    player1Id,
                    findCardsInHand(1, "Monstrous Onslaught").single(),
                    targets.map { ChosenTarget.Permanent(it) },
                    damageDistribution = division
                )
            )

            test("the announced division of X damage is dealt") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.cast(listOf(bears, courser), mapOf(bears to 2, courser to 1)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("Centaur Courser took only its 1") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe false
                }
            }

            test("the division must sum to the greatest power and give each target at least 1") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                withClue("two damage is not X = 3") {
                    game.cast(listOf(bears, courser), mapOf(bears to 1, courser to 1)).error shouldNotBe null
                }
                withClue("a target assigned zero") {
                    game.cast(listOf(bears, courser), mapOf(bears to 3, courser to 0)).error shouldNotBe null
                }
            }

            test("a target that becomes illegal loses its share — the rest is not reassigned") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.cast(listOf(bears, courser), mapOf(bears to 1, courser to 2)).error shouldBe null
                game.passPriority().error shouldBe null
                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("Centaur Courser (3/3) takes only its announced 2, not Grizzly Bears' 1 as well") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe false
                }
            }

            test("X is frozen as the spell is cast — killing the biggest creature in response doesn't shrink it") {
                val game = board()
                val giant = game.findPermanent("Hill Giant")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.cast(listOf(courser)).error shouldBe null
                game.passPriority().error shouldBe null
                game.castSpell(2, "Lightning Bolt", giant).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Hill Giant") shouldBe true
                withClue("Centaur Courser (3/3) is dealt the cast-time 3, not Llanowar Elves' 1") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
            }
        }
    }
}
