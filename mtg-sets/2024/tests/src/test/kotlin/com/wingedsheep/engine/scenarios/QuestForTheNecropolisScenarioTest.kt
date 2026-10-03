package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Quest for the Necropolis (MH3 #104).
 *
 *   Landfall — Whenever a land you control enters, put a quest counter on this enchantment.
 *   {5}{B}, Sacrifice this enchantment: Put target creature card from a graveyard onto the
 *   battlefield under your control. This ability costs {1} less to activate for each quest
 *   counter on this enchantment. Activate only as a sorcery.
 */
class QuestForTheNecropolisScenarioTest : ScenarioTestBase() {

    private fun abilityId() = cardRegistry.getCard("Quest for the Necropolis")!!.activatedAbilities.first().id

    private fun quest(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.QUEST) ?: 0

    private fun setQuest(game: TestGame, id: EntityId, n: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.QUEST, n))
        }
    }

    init {
        context("Quest for the Necropolis") {

            test("landfall adds a quest counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Quest for the Necropolis")
                    .withCardInHand(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val questId = game.findPermanent("Quest for the Necropolis")!!
                game.execute(PlayLand(game.player1Id, game.findCardsInHand(1, "Swamp").single())).error shouldBe null
                game.resolveStack()
                quest(game, questId) shouldBe 1
            }

            test("four quest counters cut the cost to {1}{B}; reanimates an opponent's creature under your control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Quest for the Necropolis")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val questId = game.findPermanent("Quest for the Necropolis")!!
                setQuest(game, questId, 4)
                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

                val result = game.execute(
                    ActivateAbility(
                        game.player1Id, questId, abilityId(),
                        targets = listOf(ChosenTarget.Card(bears, game.player2Id, Zone.GRAVEYARD))
                    )
                )
                withClue("{1}{B} from two Swamps: ${result.error}") { result.error shouldBe null }
                withClue("sacrificed as a cost") { game.isInGraveyard(1, "Quest for the Necropolis") shouldBe true }
                game.resolveStack()

                game.state.getBattlefield().contains(bears) shouldBe true
                game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            }

            test("the discount is one per quest counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Quest for the Necropolis")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val questId = game.findPermanent("Quest for the Necropolis")!!
                setQuest(game, questId, 3)
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                val result = game.execute(
                    ActivateAbility(
                        game.player1Id, questId, abilityId(),
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD))
                    )
                )
                withClue("{2}{B} can't be paid from two Swamps") { result.error shouldNotBe null }
            }
        }
    }
}
