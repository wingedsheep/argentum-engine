package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Healing Grace.
 *
 * "Prevent the next 3 damage that would be dealt to any target this turn by a source of your choice.
 *  You gain 3 life."
 */
class HealingGraceScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.chooseSource(sourceName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        val selectDecision = decision
        val entityId = selectDecision.options.first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == sourceName
        }
        submitDecision(CardsSelectedResponse(decision.id, listOf(entityId)))
    }

    init {
        context("Healing Grace") {

            test("prevents combat damage from chosen source and gains 3 life") {
                val game = scenario()
                    .withPlayers("White Mage", "Attacker")
                    .withCardInHand(1, "Healing Grace")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Alpine Grizzly") // 4/2
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Player 2 attacks with Alpine Grizzly
                game.declareAttackers(mapOf("Alpine Grizzly" to 1))

                // P2 passes, P1 casts Healing Grace targeting themselves
                game.passPriority()
                game.castSpellTargetingPlayer(1, "Healing Grace", 1)

                // Resolve Healing Grace
                game.passPriority()  // P1 passes
                game.passPriority()  // P2 passes → resolves

                // Choose Alpine Grizzly as the source
                game.chooseSource("Alpine Grizzly")

                // P1 gains 3 life immediately
                game.getLifeTotal(1) shouldBe 23

                // Advance through blockers and combat damage
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Alpine Grizzly deals 4 damage, 3 prevented, 1 gets through
                game.getLifeTotal(1) shouldBe (23 - 1)
            }

            test("prevents spell damage when cast in response targeting a creature") {
                val game = scenario()
                    .withPlayers("White Mage", "Red Mage")
                    .withCardInHand(1, "Healing Grace")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerId = game.findPermanent("Glory Seeker")!!

                // P2 casts Shock targeting Glory Seeker
                game.castSpell(2, "Shock", glorySeekerId)

                // P2 passes priority, P1 responds with Healing Grace targeting Glory Seeker
                game.passPriority()
                game.castSpell(1, "Healing Grace", glorySeekerId)

                // Resolve Healing Grace (LIFO)
                game.passPriority()  // P1 passes
                game.passPriority()  // P2 passes → Healing Grace resolves

                // Choose Shock as the source (it's on the stack)
                game.chooseSource("Shock")

                // P1 gains 3 life from Healing Grace
                game.getLifeTotal(1) shouldBe 23

                // Resolve Shock - both pass priority
                game.passPriority()  // P1 passes
                game.passPriority()  // P2 passes → Shock resolves

                // Glory Seeker should survive (2 damage fully prevented by 3 shield)
                game.findPermanent("Glory Seeker").shouldNotBeNull()
                game.getLifeTotal(1) shouldBe 23
            }

            test("gains 3 life even with no damage to prevent") {
                val game = scenario()
                    .withPlayers("White Mage", "Opponent")
                    .withCardInHand(1, "Healing Grace")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // P1 casts Healing Grace targeting themselves
                game.castSpellTargetingPlayer(1, "Healing Grace", 1)

                // Resolve
                game.resolveStack()

                // Choose any source (Plains is the only permanent)
                game.chooseSource("Plains")

                // P1 gains 3 life
                game.getLifeTotal(1) shouldBe 23
            }
        }
    }
}
