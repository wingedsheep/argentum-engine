package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Goblin Dark-Dwellers (OGW #110) — "When this creature enters, you may cast target instant or
 * sorcery card with mana value 3 or less from your graveyard without paying its mana cost. If that
 * spell would be put into your graveyard, exile it instead."
 *
 * The card is targeted when the trigger goes on the stack and cast while the trigger resolves, so
 * only a card that is actually cast leaves the graveyard — and one that is cast ends up in exile.
 */
class GoblinDarkDwellersScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Goblin Dark-Dwellers")
            .withLandsOnBattlefield(1, "Mountain", 5)
            .withCardInGraveyard(1, "Divination") // sorcery, mana value 3
            .withCardInGraveyard(1, "Wrath of God") // sorcery, mana value 4 — too expensive
            .withCardInGraveyard(1, "Grizzly Bears") // not an instant or sorcery
            .withCardInGraveyard(2, "Giant Growth") // the opponent's graveyard doesn't count
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(4) { builder = builder.withCardInLibrary(1, "Island") }
        return builder.build()
    }

    /** Cast the Goblin and aim its trigger at [target] from the controller's graveyard. */
    private fun TestGame.castGoblin(target: String): ChooseTargetsDecision {
        castSpell(1, "Goblin Dark-Dwellers").error shouldBe null
        resolveStack()
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        selectTargets(findCardsInGraveyard(1, target)).error shouldBe null
        return decision
    }

    init {
        context("Goblin Dark-Dwellers") {
            test("only your own instant and sorcery cards with mana value 3 or less can be targeted") {
                val game = setup()

                val decision = game.castGoblin("Divination")

                val legal = decision.legalTargets.getValue(0)
                withClue("Divination is the only legal target") {
                    legal shouldBe game.findCardsInGraveyard(1, "Divination")
                }
            }

            test("casting it for free resolves it, and it is exiled instead of going to the graveyard") {
                val game = setup()
                val handBefore = game.handSize(1) - 1 // without the Goblin, which is about to be cast

                game.castGoblin("Divination")
                game.resolveStack()
                withClue("the cast is a choice made while the trigger resolves") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Divination resolved: you drew two cards") { game.handSize(1) shouldBe handBefore + 2 }
                withClue("it was exiled, not put into the graveyard") {
                    game.isInExile(1, "Divination") shouldBe true
                    game.isInGraveyard(1, "Divination") shouldBe false
                }
            }

            test("declining leaves the card in the graveyard") {
                val game = setup()
                val handBefore = game.handSize(1) - 1 // without the Goblin, which is about to be cast

                game.castGoblin("Divination")
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                game.isInGraveyard(1, "Divination") shouldBe true
                game.isInExile(1, "Divination") shouldBe false
            }
        }
    }
}
