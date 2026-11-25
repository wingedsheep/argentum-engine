package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jace, Architect of Thought (RTR #44, {2}{U}{U}, loyalty 4).
 *
 * Pins the two abilities whose correctness depends on how the engine runs the script:
 *  - the +1 watcher shrinks an opponent's attacker on the opponent's own turn;
 *  - the −8 iterates every player's library while *Jace's controller* searches and picks each
 *    nonland card (the per-player loop rebinds "you"), and the exiled cards are then cast for free
 *    during the ability's resolution — even though Jace, spent down to 0 loyalty, is already in
 *    the graveyard by then.
 */
class JaceArchitectOfThoughtScenarioTest : ScenarioTestBase() {

    init {
        context("the +1") {

            test("an opponent's attacking creature gets -1/-0 on their turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Architect of Thought")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Architect of Thought")!!
                setLoyalty(game, jace, 4)
                activate(game, jace, index = 0)
                game.resolveStack()
                loyalty(game, jace) shouldBe 5

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant (3/3) attacks and becomes 2/3") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }
        }

        context("the −8") {

            test("Jace's controller exiles a nonland card from each library and casts them for free") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Architect of Thought")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Architect of Thought")!!
                setLoyalty(game, jace, 8)
                activate(game, jace, index = 2)
                game.resolveStack()

                // One search per library, both made by Jace's controller, lands never offered.
                val searched = mutableSetOf<String>()
                repeat(2) {
                    val search = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                    withClue("Jace's controller searches every library, including the opponent's") {
                        search.playerId shouldBe game.player1Id
                    }
                    val names = optionNames(game, search)
                    withClue("only the nonland card is findable") {
                        (names == setOf("Grizzly Bears") || names == setOf("Hill Giant")) shouldBe true
                    }
                    searched += names
                    game.selectCards(search.options).error shouldBe null
                }
                searched shouldBe setOf("Grizzly Bears", "Hill Giant")

                withClue("Jace hit 0 loyalty paying the cost and is gone before the free casts") {
                    game.isOnBattlefield("Jace, Architect of Thought") shouldBe false
                }

                val cast = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                cast.playerId shouldBe game.player1Id
                optionNames(game, cast) shouldBe setOf("Grizzly Bears", "Hill Giant")
                // The free casts are offered one card at a time until none remain.
                var next: SelectCardsDecision? = cast
                while (next != null && next.options.isNotEmpty()) {
                    next.playerId shouldBe game.player1Id
                    game.selectCards(listOf(next.options.first())).error shouldBe null
                    next = game.getPendingDecision() as? SelectCardsDecision
                }
                game.resolveStack()

                withClue("both cast cards resolve under Jace's controller") {
                    val bears = game.findPermanent("Grizzly Bears")!!
                    val giant = game.findPermanent("Hill Giant")!!
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                }
                withClue("the lands stay in their libraries") {
                    game.findCardsInLibrary(1, "Island").size shouldBe 1
                    game.findCardsInLibrary(2, "Forest").size shouldBe 1
                }
            }
        }
    }

    private fun optionNames(game: TestGame, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    private fun activate(game: TestGame, source: EntityId, index: Int) {
        val ability = cardRegistry.getCard("Jace, Architect of Thought")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
