package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Abundant Maw — {8} 6/4 Eldrazi Leech with emerge {6}{B} and
 * "When you cast this spell, target opponent loses 3 life and you gain 3 life."
 *
 * The drain is a cast trigger, so it resolves before the Leech itself.
 */
class AbundantMawScenarioTest : ScenarioTestBase() {

    init {
        context("Abundant Maw") {

            test("emerge cast drains the targeted opponent for 3 before the Leech resolves") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Abundant Maw")
                    .withCardOnBattlefield(1, "Centaur Courser") // {2}{G} → mana value 3
                    // Emerge {6}{B} reduced by 3 → {3}{B}: four Swamps is exactly enough.
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .build()

                val cast = game.castSpellWithEmerge(
                    1, "Abundant Maw", "Centaur Courser",
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Centaur Courser") shouldBe true

                game.resolveStack()

                withClue("the opponent lost 3 and the caster gained 3") {
                    game.getLifeTotal(2) shouldBe 17
                    game.getLifeTotal(1) shouldBe 23
                }
                game.isOnBattlefield("Abundant Maw") shouldBe true
            }

            test("the two amounts are independent — you gain 3 even if the opponent had less to lose") {
                val game = scenario()
                    .withPlayers()
                    .withLifeTotal(2, 2)
                    .withCardInHand(1, "Abundant Maw")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .build()

                game.castSpellWithEmerge(
                    1, "Abundant Maw", "Centaur Courser",
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                ).error shouldBe null
                game.resolveStack()

                withClue("an opponent at 2 goes to -1 and you still gain the full 3") {
                    game.getLifeTotal(2) shouldBe -1
                    game.getLifeTotal(1) shouldBe 23
                }
            }
        }
    }
}
