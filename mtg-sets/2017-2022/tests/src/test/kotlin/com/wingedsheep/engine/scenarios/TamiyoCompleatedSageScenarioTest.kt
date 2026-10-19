package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.neo.cards.TamiyoCompleatedSage
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tamiyo, Compleated Sage (NEO #238).
 *
 * The −X target must have mana value exactly the loyalty X paid; the card is exiled and a token
 * copy of it enters. The −7 makes Tamiyo's Notebook, whose cost reduction and draw ability work.
 */
class TamiyoCompleatedSageScenarioTest : ScenarioTestBase() {

    private val minusX = TamiyoCompleatedSage.activatedAbilities[1].id
    private val minusSeven = TamiyoCompleatedSage.activatedAbilities[2].id
    private val notebookDraw = PredefinedTokens.TamiyosNotebook.activatedAbilities[0].id

    init {
        context("Tamiyo, Compleated Sage") {

            test("−X exiles a nonland permanent card with mana value exactly X and creates a token copy") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, Compleated Sage")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val tamiyo = game.findPermanent("Tamiyo, Compleated Sage")!!
                seedLoyalty(game, tamiyo, 5)

                withClue("Hill Giant (mana value 4) doesn't match X = 2") {
                    game.execute(
                        ActivateAbility(
                            game.player1Id, tamiyo, minusX,
                            targets = listOf(ChosenTarget.Card(graveyardCard(game, "Hill Giant"), game.player1Id, Zone.GRAVEYARD)),
                            xValue = 2
                        )
                    ).error shouldNotBe null
                }

                game.execute(
                    ActivateAbility(
                        game.player1Id, tamiyo, minusX,
                        targets = listOf(ChosenTarget.Card(graveyardCard(game, "Grizzly Bears"), game.player1Id, Zone.GRAVEYARD)),
                        xValue = 2
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                val token = game.findPermanent("Grizzly Bears")
                token.shouldNotBeNull()
                loyalty(game, tamiyo) shouldBe 3
            }

            test("−7 creates Tamiyo's Notebook, which reduces spell costs by {2} and taps to draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, Compleated Sage")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val tamiyo = game.findPermanent("Tamiyo, Compleated Sage")!!
                seedLoyalty(game, tamiyo, 7)

                game.execute(ActivateAbility(game.player1Id, tamiyo, minusSeven)).error shouldBe null
                game.resolveStack()

                val notebook = game.findPermanent("Tamiyo's Notebook")
                notebook.shouldNotBeNull()

                withClue("Hill Giant ({3}{R}) costs {1}{R} with the Notebook out") {
                    game.castSpell(1, "Hill Giant").error shouldBe null
                    game.resolveStack()
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }

                val handBefore = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, notebook, notebookDraw)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe handBefore + 1
                game.state.getEntity(notebook)?.has<TappedComponent>() shouldBe true
            }
        }
    }

    private fun graveyardCard(game: TestGame, name: String): EntityId =
        game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).first {
            game.state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        // The scenario builder skips the "enters with its starting loyalty" step, so seed it.
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
