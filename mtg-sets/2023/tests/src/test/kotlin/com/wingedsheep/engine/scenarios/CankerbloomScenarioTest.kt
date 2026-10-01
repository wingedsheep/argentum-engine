package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.Cankerbloom
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cankerbloom (ONE #161) — {1}{G} 3/2 Creature — Phyrexian Fungus.
 *
 * "{1}, Sacrifice this creature: Choose one — Destroy target artifact; destroy target
 * enchantment; or proliferate."
 */
class CankerbloomScenarioTest : ScenarioTestBase() {

    private val abilityId = Cankerbloom.activatedAbilities.first().id

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("mode 1 destroys target artifact; Cankerbloom is sacrificed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Cankerbloom")
                .withCardOnBattlefield(2, "Bonesplitter")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bloom = game.findPermanent("Cankerbloom")!!
            val artifact = game.findPermanent("Bonesplitter")!!

            game.execute(ActivateAbility(game.player1Id, bloom, abilityId)).error shouldBe null
            game.resolveStack()

            val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
            game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 0))

            val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                ?: error("expected a ChooseTargetsDecision; got ${game.state.pendingDecision}")
            game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(artifact))))
            game.resolveStack()

            withClue("The artifact is destroyed") { game.isInGraveyard(2, "Bonesplitter") shouldBe true }
            withClue("Cankerbloom was sacrificed as a cost") { game.isInGraveyard(1, "Cankerbloom") shouldBe true }
        }

        test("mode 3 proliferates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Cankerbloom")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)
            val bloom = game.findPermanent("Cankerbloom")!!

            game.execute(ActivateAbility(game.player1Id, bloom, abilityId)).error shouldBe null
            game.resolveStack()

            val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
                ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
            game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = 2))

            var prompts = 0
            var guard = 0
            while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
                if (game.hasPendingDecision()) { game.selectCards(listOf(bears)); prompts++ } else game.resolveStack()
            }

            prompts shouldBe 1
            counters(game, bears) shouldBe 2
            game.isInGraveyard(1, "Cankerbloom") shouldBe true
        }
    }
}
