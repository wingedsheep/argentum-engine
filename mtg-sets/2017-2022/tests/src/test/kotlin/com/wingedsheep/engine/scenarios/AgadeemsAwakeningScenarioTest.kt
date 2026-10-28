package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Agadeem's Awakening (ZNR #90, {X}{B}{B}{B}, Sorcery // Land).
 *
 *   Return from your graveyard to the battlefield any number of target creature cards that each
 *   have a different mana value X or less.
 *
 * Pins the composition: an unlimited target slot whose filter reads the announced X at cast
 * (mana value X or less) and whose cross-target check keeps the mana values pairwise distinct.
 */
class AgadeemsAwakeningScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Agadeem's Awakening")
        .withCardInGraveyard(1, "Ornithopter")      // MV 0
        .withCardInGraveyard(1, "Llanowar Elves")   // MV 1
        .withCardInGraveyard(1, "Grizzly Bears")    // MV 2
        .withCardInGraveyard(1, "Runeclaw Bear")    // MV 2
        .withCardInGraveyard(1, "Centaur Courser")  // MV 3
        .withLandsOnBattlefield(1, "Swamp", 5)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.gy(name: String) =
        ChosenTarget.Card(findCardsInGraveyard(1, name).single(), player1Id, Zone.GRAVEYARD)

    init {
        context("Agadeem's Awakening") {

            test("X = 2 returns one creature card of each mana value 0, 1 and 2") {
                val game = board()
                val spell = game.findCardsInHand(1, "Agadeem's Awakening").single()
                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(game.gy("Ornithopter"), game.gy("Llanowar Elves"), game.gy("Grizzly Bears")),
                        xValue = 2,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Ornithopter") shouldBe true
                game.isOnBattlefield("Llanowar Elves") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                withClue("untargeted cards stay in the graveyard") {
                    game.isInGraveyard(1, "Runeclaw Bear") shouldBe true
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                }
            }

            test("two targets sharing a mana value are rejected") {
                val game = board()
                val spell = game.findCardsInHand(1, "Agadeem's Awakening").single()
                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(game.gy("Grizzly Bears"), game.gy("Runeclaw Bear")),
                        xValue = 2,
                    )
                ).error shouldNotBe null
            }

            test("a creature card with mana value above X can't be targeted") {
                val game = board()
                val spell = game.findCardsInHand(1, "Agadeem's Awakening").single()
                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        targets = listOf(game.gy("Centaur Courser")),
                        xValue = 2,
                    )
                ).error shouldNotBe null
            }
        }
    }
}
