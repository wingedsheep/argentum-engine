package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Twisted Fealty. */
class TwistedFealtyScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    /**
     * Twisted Fealty has two independent targets, which the base fixture's single-target
     * [ScenarioTestBase.TestGame.castSpell] can't express — cast it directly so the "up to one"
     * second target can be present or absent.
     */
    private fun TestGame.castTwistedFealty(targets: List<EntityId>) = execute(
        CastSpell(
            player1Id,
            findCardsInHand(1, "Twisted Fealty").first(),
            targets.map { ChosenTarget.Permanent(it) }
        )
    )

    init {
        context("Twisted Fealty — borrow a creature, and crown a second one") {
            test("the stolen creature is untapped, hasty and yours; the Role lands on the other target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twisted Fealty")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castTwistedFealty(listOf(giant, bears)).error shouldBe null
                game.resolveStack()

                withClue("the Giant changed controller for the turn and untapped with haste") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                    game.state.getEntity(giant)?.get<TappedComponent>() shouldBe null
                    game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe true
                }
                withClue("the Wicked Role went on the second target, not the stolen creature") {
                    auraOn(game, "Wicked Role", bears).shouldNotBeNull()
                    auraOn(game, "Wicked Role", giant) shouldBe null
                }
                withClue("Wicked Role grants +1/+1 — the 2/2 Bears projects as 3/3") {
                    power(game, bears) shouldBe 3
                    toughness(game, bears) shouldBe 3
                }
            }

            test("with only the first target chosen the spell still steals, and makes no Role") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twisted Fealty")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

                game.castTwistedFealty(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the steal half resolved on its own") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                    game.state.getEntity(giant)?.get<TappedComponent>() shouldBe null
                }
                withClue("no second target was chosen, so no Role token exists") {
                    game.findPermanent("Wicked Role") shouldBe null
                }
            }
        }
    }
}
