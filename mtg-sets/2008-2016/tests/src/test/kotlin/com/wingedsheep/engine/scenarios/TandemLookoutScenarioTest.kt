package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Tandem Lookout — {2}{U} 2/1 Human Scout with soulbond and "As long as Tandem
 * Lookout is paired with another creature, each of those creatures has 'Whenever this creature
 * deals damage to an opponent, draw a card.'"
 *
 * The point of the card is that the granted trigger is hosted on *each* half: "this creature" means
 * whichever one dealt the damage, not the Lookout. So the interesting cases are the partner firing
 * it, the Lookout firing it, and neither firing while unpaired.
 */
class TandemLookoutScenarioTest : ScenarioTestBase() {

    init {
        context("Tandem Lookout") {

            test("the Lookout's own copy of the granted trigger draws when it attacks") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Tandem Lookout", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Grizzly Bears")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                withClue("soulbond's 'another creature you control enters' half is a yes/no") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                val handBefore = game.handSize(1)

                // Only the Lookout can attack — the Bears just entered.
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Tandem Lookout" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("2 unblocked combat damage got through") { game.getLifeTotal(2) shouldBe 18 }
                withClue("the Lookout's granted trigger drew one card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("the partner's copy draws too — 'this creature' is whichever half dealt the damage") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Tandem Lookout")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Tandem Lookout")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.hasPendingDecision() shouldBe true
                game.selectCards(listOf(bears))

                val handBefore = game.handSize(1)

                // Only the Bears can attack — the Lookout just entered.
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("2 unblocked combat damage got through") { game.getLifeTotal(2) shouldBe 18 }
                withClue("the partner carries its own copy of the granted trigger") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("an unpaired Lookout draws nothing when it connects") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Tandem Lookout", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Tandem Lookout" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("the damage still happened") { game.getLifeTotal(2) shouldBe 18 }
                withClue("the soulbondPair scope is empty, so nobody has the trigger") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
