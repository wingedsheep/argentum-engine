package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Death Match.
 *
 * Card reference:
 * - Death Match ({3}{B}): Enchantment
 *   "Whenever a creature enters, that creature's controller may have target creature
 *   of their choice get -3/-3 until end of turn."
 *
 * Flow: MayEffect + target triggered abilities ask yes/no FIRST, then targets.
 * 1. Creature enters → trigger fires → YesNoDecision
 * 2. answerYesNo(true) → unwraps MayEffect → ChooseTargetsDecision
 * 3. selectTargets() → ability goes on stack
 * 4. resolveStack() → effect applies
 */
class DeathMatchScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Death Match triggered ability") {

            test("controller's creature entering triggers and can give -3/-3 to opponent's creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Death Match")
                    .withCardInHand(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Glory Seeker
                game.castSpell(1, "Glory Seeker")
                game.resolveStack()

                // Death Match triggers with MayEffect - answer yes/no FIRST
                game.answerYesNo(true)

                // Now select target for -3/-3
                val balothId = game.findPermanent("Towering Baloth")!!
                game.selectTargets(listOf(balothId))

                // Resolve the ability on the stack
                game.resolveStack()

                // Towering Baloth should be 4/3
                val projected = stateProjector.project(game.state)
                withClue("Towering Baloth should have power 4 after -3/-3") {
                    projected.getPower(balothId) shouldBe 4
                }
                withClue("Towering Baloth should have toughness 3 after -3/-3") {
                    projected.getToughness(balothId) shouldBe 3
                }
            }

            test("controller can decline the may ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Death Match")
                    .withCardInHand(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Towering Baloth")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Glory Seeker
                game.castSpell(1, "Glory Seeker")
                game.resolveStack()

                // Decline the may ability
                game.answerYesNo(false)
                game.resolveStack()

                // Towering Baloth should still be 7/6
                val projected = stateProjector.project(game.state)
                val balothId = game.findPermanent("Towering Baloth")!!
                withClue("Towering Baloth should still have power 7") {
                    projected.getPower(balothId) shouldBe 7
                }
                withClue("Towering Baloth should still have toughness 6") {
                    projected.getToughness(balothId) shouldBe 6
                }
            }

            test("opponent's creature entering gives opponent the choice (controlledByTriggeringEntityController)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Death Match")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 on P1's side
                    .withCardInHand(2, "Elvish Warrior") // 2/3
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // P2 casts Elvish Warrior
                game.castSpell(2, "Elvish Warrior")
                game.resolveStack()

                // Death Match triggers - P2 (entering creature's controller) answers yes
                game.answerYesNo(true)

                // P2 targets P1's Glory Seeker
                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(glorySeekerId))

                // Resolve
                game.resolveStack()

                // Glory Seeker (2/2) gets -3/-3 = -1/-1, should die from 0 or less toughness
                withClue("Glory Seeker should be dead after getting -3/-3") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }

            test("-3/-3 that reduces toughness to 0 or less kills the creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Death Match")
                    .withCardInHand(1, "Elvish Warrior") // 2/3
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Elvish Warrior")
                game.resolveStack()

                // Answer yes to may
                game.answerYesNo(true)

                // Target opponent's Glory Seeker (2/2 -> -1/-1, dies)
                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(glorySeekerId))
                game.resolveStack()

                withClue("Glory Seeker should be dead") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }
        }
    }

}
