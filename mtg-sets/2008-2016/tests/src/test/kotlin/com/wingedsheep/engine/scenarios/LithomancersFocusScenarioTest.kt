package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lea.cards.RodOfRuin
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lithomancer's Focus (BFZ #38) — {W} Instant. "Target creature gets +2/+2 until end of turn.
 * Prevent all damage that would be dealt to that creature this turn by colorless sources."
 *
 * The shield covers only the targeted creature and only colorless sources: Rod of Ruin's damage
 * to it is prevented, Shock's red damage to it is dealt, and Rod's damage to another creature is
 * dealt.
 */
class LithomancersFocusScenarioTest : ScenarioTestBase() {
    init {
        val rodAbility = RodOfRuin.activatedAbilities.first().id

        fun TestGame.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

        fun TestGame.rodPings(target: EntityId) {
            val rod = findPermanent("Rod of Ruin")!!
            val result = execute(
                ActivateAbility(player1Id, rod, rodAbility, targets = listOf(ChosenTarget.Permanent(target)))
            )
            withClue("${result.error}") { result.error shouldBe null }
            if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
            resolveStack()
        }

        fun focusedGame(): Pair<TestGame, EntityId> {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Lithomancer's Focus")
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Rod of Ruin")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val cast = game.castSpell(1, "Lithomancer's Focus", bears)
            withClue("${cast.error}") { cast.error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()
            return game to bears
        }

        test("the target gets +2/+2 and a colorless source's damage to it is prevented") {
            val (game, bears) = focusedGame()
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4

            game.rodPings(bears)

            game.damageOn(bears) shouldBe 0
        }

        test("damage to it from a colored source is still dealt") {
            val (game, bears) = focusedGame()

            val shock = game.castSpell(1, "Shock", bears)
            withClue("${shock.error}") { shock.error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.damageOn(bears) shouldBe 2
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("a colorless source's damage to another creature is still dealt") {
            val (game, _) = focusedGame()
            val giant = game.findPermanent("Hill Giant")!!

            game.rodPings(giant)

            game.damageOn(giant) shouldBe 1
        }
    }
}
