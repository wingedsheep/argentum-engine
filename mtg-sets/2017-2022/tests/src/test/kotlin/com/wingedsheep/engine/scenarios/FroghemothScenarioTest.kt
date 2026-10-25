package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Froghemoth — {3}{G}{G} Creature — Frog Horror 4/4 (AFR #184).
 *
 * "Trample, haste. Whenever this creature deals combat damage to a player, exile up to that many
 * target cards from their graveyard. Put a +1/+1 counter on this creature for each creature card
 * exiled this way. You gain 1 life for each noncreature card exiled this way."
 */
class FroghemothScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardOnBattlefield(1, "Froghemoth", summoningSickness = false)
        .withCardInGraveyard(2, "Hill Giant")
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInGraveyard(2, "Mountain")
        .withCardInGraveyard(2, "Lightning Bolt")
        .withCardInGraveyard(2, "Forest")
        .withCardInGraveyard(1, "Force of Nature")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("targets are capped at the damage dealt and limited to the damaged player's graveyard") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Froghemoth" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            val decision = game.state.pendingDecision as ChooseTargetsDecision
            withClue("4 combat damage -> up to 4 targets") {
                decision.targetRequirements.first().maxTargets shouldBe 4
            }
            val legal = decision.legalTargets.getValue(0)
            withClue("only the opponent's five graveyard cards are legal") {
                legal.size shouldBe 5
                legal.contains(game.findCardsInGraveyard(1, "Force of Nature").first()) shouldBe false
            }
        }

        test("counters for creature cards exiled, life for noncreature cards exiled") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Froghemoth" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            val chosen = listOf(
                game.findCardsInGraveyard(2, "Hill Giant").first(),
                game.findCardsInGraveyard(2, "Grizzly Bears").first(),
                game.findCardsInGraveyard(2, "Mountain").first(),
            )
            game.selectTargets(chosen).error shouldBe null
            game.resolveStack()

            withClue("the chosen cards are exiled") {
                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Mountain") shouldBe true
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            }
            withClue("two creature cards -> two +1/+1 counters") {
                game.plusOnes(game.findPermanent("Froghemoth")!!) shouldBe 2
            }
            withClue("one noncreature card -> 1 life; opponent took 4") {
                game.getLifeTotal(1) shouldBe 21
                game.getLifeTotal(2) shouldBe 16
            }
        }
    }
}
