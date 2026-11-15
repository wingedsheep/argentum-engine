package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Risen Executioner (DTK #116) — {2}{B}{B} 4/3 Zombie Warrior. Can't block; other Zombie creatures
 * you control get +1/+1; "You may cast this creature from your graveyard if you pay {1} more to
 * cast it for each other creature card in your graveyard."
 *
 * The load-bearing claim is the graveyard tax: counted over the *other* creature cards (never the
 * Executioner itself, nor noncreature cards), and owed only by a graveyard cast.
 */
class RisenExecutionerScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.executionerCastCost(): String? {
        val id = findCardsInGraveyard(1, "Risen Executioner").single()
        return getLegalActions(1).firstOrNull { (it.action as? CastSpell)?.cardId == id }?.manaCostString
    }

    init {
        context("Risen Executioner") {

            test("cast from the graveyard for {1} more per other creature card there") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Risen Executioner")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Swamp") // not a creature card
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("two other creature cards → {2} more; itself and the land don't count") {
                    game.executionerCastCost() shouldBe "{4}{B}{B}"
                }
                game.castSpellFromGraveyard(1, "Risen Executioner").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Risen Executioner") shouldBe true
            }

            test("the tax is paid in full — five lands can't cast it over two other creature cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Risen Executioner")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellFromGraveyard(1, "Risen Executioner").error shouldNotBe null
                game.findCardsInGraveyard(1, "Risen Executioner").size shouldBe 1
            }

            test("a hand cast owes no tax, and other Zombies get +1/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Risen Executioner")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Gravecrawler")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Risen Executioner").error shouldBe null
                game.resolveStack()
                val executioner = game.findPermanent("Risen Executioner")!!
                val crawler = game.findPermanent("Gravecrawler")!!
                withClue("other Zombies get +1/+1, the Executioner itself doesn't") {
                    game.state.projectedState.getPower(crawler) shouldBe 3
                    game.state.projectedState.getToughness(crawler) shouldBe 2
                    game.state.projectedState.getPower(executioner) shouldBe 4
                }
            }
        }
    }
}
