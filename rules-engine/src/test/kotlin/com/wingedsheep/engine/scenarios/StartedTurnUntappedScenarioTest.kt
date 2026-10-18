package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ControlHistory
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.shouldBe

/**
 * `StatePredicate.StartedTurnUntapped` — "if <this> started the turn untapped" (Rasputin
 * Dreamweaver), read off the turn-start snapshot `ControlHistory` takes before the untap step.
 *
 * | Rule / ruling | Covered by |
 * |---|---|
 * | untapped on the battlefield as the turn begins → matches at the upkeep | "an untapped permanent started the turn untapped" |
 * | tapped as the turn begins → no match, though the untap step untapped it | "a permanent the untap step untapped did not" |
 * | the answer is fixed for the turn: tapping it later doesn't change it | "tapping it later in the turn doesn't change the answer" |
 * | a permanent that entered this turn didn't start the turn on the battlefield | "a permanent that entered this turn did not" |
 * | an imported board treats permanents that entered this turn as new | "an imported board" |
 */
class StartedTurnUntappedScenarioTest : ScenarioTestBase() {

    private val watcher = card("Test Turn Start Watcher") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            interveningIf = Conditions.SourceMatches(GameObjectFilter.Permanent.startedTurnUntapped())
            effect = Effects.GainLife(1)
        }
    }

    private fun board(tapped: Boolean) = scenario()
        .withPlayers("Watcher", "Opponent")
        .withCardOnBattlefield(1, "Test Turn Start Watcher", tapped = tapped)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.toWatcherMain() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        state.activePlayerId shouldBe player1Id
    }

    init {
        cardRegistry.register(watcher)

        test("an untapped permanent started the turn untapped") {
            val game = board(tapped = false).build()
            game.toWatcherMain()
            game.getLifeTotal(1) shouldBe 21
        }

        test("a permanent the untap step untapped did not") {
            val game = board(tapped = true).build()
            game.toWatcherMain()
            val id = game.findPermanent("Test Turn Start Watcher")!!
            game.state.getEntity(id)!!.has<TappedComponent>() shouldBe false
            game.getLifeTotal(1) shouldBe 20
            ControlHistory.startedTurnUntapped(game.state, id) shouldBe false
        }

        test("tapping it later in the turn doesn't change the answer") {
            val game = board(tapped = false).build()
            game.toWatcherMain()
            val id = game.findPermanent("Test Turn Start Watcher")!!
            game.state = game.state.updateEntity(id) { it.with(TappedComponent) }
            ControlHistory.startedTurnUntapped(game.state, id) shouldBe true
        }

        test("a permanent that entered this turn did not") {
            val game = scenario()
                .withPlayers("Watcher", "Opponent")
                .withCardInHand(1, "Test Turn Start Watcher")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Test Turn Start Watcher").error shouldBe null
            game.resolveStack()
            val id = game.findPermanent("Test Turn Start Watcher")!!
            ControlHistory.startedTurnUntapped(game.state, id) shouldBe false
        }

        test("an imported board treats permanents that entered this turn as new") {
            val game = scenario()
                .withPlayers("Watcher", "Opponent")
                .withCardOnBattlefield(1, "Test Turn Start Watcher", enteredThisTurn = true)
                .withCardOnBattlefield(2, "Test Turn Start Watcher")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val state = ControlHistory.initialize(game.state)
            val (fresh, settled) = game.findPermanents("Test Turn Start Watcher")
                .partition { state.projectedState.getController(it) == game.player1Id }
            ControlHistory.startedTurnUntapped(state, fresh.single()) shouldBe false
            ControlHistory.startedTurnUntapped(state, settled.single()) shouldBe true
        }
    }
}
