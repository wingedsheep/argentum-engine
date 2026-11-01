package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Finale of Eternity (WAR #91, {X}{B}{B}, Sorcery).
 *
 *   Destroy up to three target creatures with toughness X or less. If X is 10 or more, return all
 *   creature cards from your graveyard to the battlefield.
 *
 * Pins the X-reading target filter (each target's toughness against the announced X) and the
 * X >= 10 rider: it runs after the destroys, so your own destroyed creature comes back, and it
 * applies with no targets chosen at all.
 */
class FinaleOfEternityScenarioTest : ScenarioTestBase() {

    init {
        context("Finale of Eternity") {

            test("X = 2 destroys up to three creatures with toughness 2 or less and returns nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Finale of Eternity")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Finale of Eternity").single()
                val targets = listOf("Grizzly Bears", "Llanowar Elves")
                    .map { ChosenTarget.Permanent(game.findPermanent(it)!!) }
                game.execute(CastSpell(game.player1Id, spell, targets, xValue = 2)).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Llanowar Elves") shouldBe false
                withClue("X below 10 must not return creature cards from the graveyard") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                }
            }

            test("a creature with toughness above X can't be targeted") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Finale of Eternity")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Finale of Eternity").single()
                val courser = ChosenTarget.Permanent(game.findPermanent("Centaur Courser")!!)
                game.execute(CastSpell(game.player1Id, spell, listOf(courser), xValue = 2)).error shouldNotBe null
            }

            test("X = 10 returns all your creature cards, including your own creature it destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Finale of Eternity")
                    .withLandsOnBattlefield(1, "Swamp", 12)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInGraveyard(1, "Swamp")
                    .withCardInGraveyard(2, "Runeclaw Bear")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Finale of Eternity").single()
                val targets = listOf("Grizzly Bears", "Centaur Courser")
                    .map { ChosenTarget.Permanent(game.findPermanent(it)!!) }
                game.execute(CastSpell(game.player1Id, spell, targets, xValue = 10)).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                withClue("Your destroyed creature and your graveyard creature card come back") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Llanowar Elves") shouldBe true
                }
                withClue("Non-creature cards and the opponent's graveyard stay put") {
                    game.isInGraveyard(1, "Swamp") shouldBe true
                    game.isInGraveyard(2, "Runeclaw Bear") shouldBe true
                }
            }

            test("X = 10 with no targets still returns your creature cards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Finale of Eternity")
                    .withLandsOnBattlefield(1, "Swamp", 12)
                    .withCardInGraveyard(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "Finale of Eternity").single()
                game.execute(CastSpell(game.player1Id, spell, emptyList(), xValue = 10)).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isOnBattlefield("Centaur Courser") shouldBe true
            }
        }
    }
}
