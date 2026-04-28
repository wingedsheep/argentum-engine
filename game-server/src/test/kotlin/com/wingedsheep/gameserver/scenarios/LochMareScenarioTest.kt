package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Scenario tests for Loch Mare.
 *
 * Loch Mare ({1}{U}) — Creature — Horse Serpent, 4/5
 * This creature enters with three -1/-1 counters on it.
 * {1}{U}, Remove a counter from this creature: Draw a card.
 * {2}{U}, Remove two counters from this creature: Tap target creature.
 *   Put a stun counter on it.
 *
 * Regression: both activated abilities would remain offered as legal actions
 * after the creature's -1/-1 counters were exhausted, because
 * ActivatedAbilityEnumerator did not check AbilityCost.RemoveCounterFromSelf
 * payability.
 */
class LochMareScenarioTest : ScenarioTestBase() {

    private fun setCounters(game: TestGame, entityId: EntityId, count: Int) {
        val counters = CountersComponent().withAdded(CounterType.MINUS_ONE_MINUS_ONE, count)
        game.state = game.state.updateEntity(entityId) { c -> c.with(counters) }
    }

    private fun newSession(game: TestGame): GameSession {
        val session = GameSession(cardRegistry = cardRegistry)
        val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
        val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
        session.injectStateForTesting(
            game.state,
            mapOf(
                game.player1Id to PlayerSession(mockWs1, game.player1Id, "Player"),
                game.player2Id to PlayerSession(mockWs2, game.player2Id, "Opponent")
            )
        )
        return session
    }

    init {
        context("Loch Mare activated-ability enumeration") {

            test("with no counters, neither activated ability is offered") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loch Mare", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Loch Mare's counters have been fully removed.
                val mare = game.findPermanent("Loch Mare")!!
                setCounters(game, mare, 0)

                val legalActions = newSession(game).getLegalActions(game.player1Id)
                val mareAbilities = legalActions.filter {
                    val act = it.action
                    it.actionType == "ActivateAbility" &&
                        act is ActivateAbility &&
                        act.sourceId == mare &&
                        it.isAffordable
                }

                withClue("Loch Mare abilities should not be offered as affordable when it has no counters") {
                    mareAbilities shouldBe emptyList()
                }
            }

            test("with one counter, only the draw ability is offered") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loch Mare", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mare = game.findPermanent("Loch Mare")!!
                setCounters(game, mare, 1)

                val legalActions = newSession(game).getLegalActions(game.player1Id)
                val mareAbilities = legalActions.filter {
                    val act = it.action
                    it.actionType == "ActivateAbility" &&
                        act is ActivateAbility &&
                        act.sourceId == mare &&
                        it.isAffordable
                }

                withClue("Only one ability should be affordable (the draw ability costs one counter; tap+stun costs two)") {
                    mareAbilities.size shouldBe 1
                }
                withClue("The offered ability should be the draw ability") {
                    mareAbilities.single().description.contains("Draw") shouldBe true
                }
            }

            test("entering from graveyard via reanimation enters with three -1/-1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doomed Necromancer", summoningSickness = false)
                    .withCardInGraveyard(1, "Loch Mare")
                    .withLandsOnBattlefield(1, "Swamp", 1) // {B} for activation cost
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necromancerId = game.findPermanent("Doomed Necromancer")!!
                val mareInGraveyard = game.findCardsInGraveyard(1, "Loch Mare").first()

                val cardDef = cardRegistry.getCard("Doomed Necromancer")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = necromancerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(mareInGraveyard, game.player1Id, Zone.GRAVEYARD))
                    )
                )

                withClue("Reanimation ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Loch Mare should be back on the battlefield after reanimation") {
                    game.isOnBattlefield("Loch Mare") shouldBe true
                }

                val mareOnBattlefield = game.findPermanent("Loch Mare")!!
                val counters = game.state.getEntity(mareOnBattlefield)?.get<CountersComponent>()
                val minusOneCount = counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

                withClue("Loch Mare should enter with three -1/-1 counters even when reanimated from graveyard, but had $minusOneCount") {
                    minusOneCount shouldBe 3
                }
            }

            test("with three counters, both activated abilities are offered") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loch Mare", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mare = game.findPermanent("Loch Mare")!!
                setCounters(game, mare, 3)

                val legalActions = newSession(game).getLegalActions(game.player1Id)
                val mareAbilities = legalActions.filter {
                    val act = it.action
                    it.actionType == "ActivateAbility" &&
                        act is ActivateAbility &&
                        act.sourceId == mare &&
                        it.isAffordable
                }

                withClue("Both activated abilities should be offered when counters are sufficient") {
                    mareAbilities.size shouldBe 2
                }
            }
        }
    }
}
