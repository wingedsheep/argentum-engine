package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.PhaseInLock
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PhaseOutEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `PhaseOut(cantPhaseIn = Duration.EndOfYourNextTurn)` — "Until the end of your next turn, they
 * can't phase in" (Teferi, Timeless Voyager). Phased-out permanents normally phase in during their
 * controller's untap step (Rule 702.26a); the lock skips every untap step up to the end of the
 * effect controller's next turn, after which the permanent phases in at its controller's *next*
 * untap step — the end of the window itself phases nothing in.
 */
class PhaseOutUntilEndOfYourNextTurnTest : ScenarioTestBase() {

    private val sorcery = card("Lock Phase Sorcery") {
        manaCost = "{U}"; typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.PhaseOut(t, cantPhaseIn = Duration.EndOfYourNextTurn)
        }
    }

    private val instant = card("Lock Phase Instant") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.PhaseOut(t, cantPhaseIn = Duration.EndOfYourNextTurn)
        }
    }

    init {
        cardRegistry.register(sorcery)
        cardRegistry.register(instant)

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, sorcery.name)
            .withCardInHand(1, instant.name)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(8) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        fun TestGame.isPhasedIn(id: EntityId) = id in state.getBattlefield()

        /** Pass to the next untap-step-completed upkeep, asserting whose turn it is. */
        fun TestGame.toUpkeepOf(playerId: EntityId) {
            passUntilPhase(Phase.ENDING, Step.END)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            state.activePlayerId shouldBe playerId
        }

        test("cast on your turn: skips its controller's next untap and phases in at the one after your next turn") {
            val game = builder().build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, sorcery.name, bears).error shouldBe null
            game.resolveStack()
            game.isPhasedIn(bears) shouldBe false
            game.state.getEntity(bears)!!.get<PhasedOutComponent>()!!.phaseInLock
                .shouldBeInstanceOf<PhaseInLock.UntilEndOfControllersNextTurn>()

            withClue("the turn the effect was created on never counts as 'your next turn'") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(bears) shouldBe false
            }

            game.toUpkeepOf(game.player1Id)
            game.isPhasedIn(bears) shouldBe false

            withClue("still locked during your next turn's end step — the window lasts the whole turn") {
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.isPhasedIn(bears) shouldBe false
                game.state.getEntity(bears)!!.get<PhasedOutComponent>()!!.phaseInLock
                    .shouldBeInstanceOf<PhaseInLock.UntilEndOfControllersNextTurn>()
            }

            withClue("it phases in during its controller's next untap step after your next turn") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(bears) shouldBe true
            }
        }

        test("cast on an opponent's turn: your next turn is the following one") {
            val game = builder().withActivePlayer(2).withPriorityPlayer(1).build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, instant.name, bears).error shouldBe null
            game.resolveStack()
            game.isPhasedIn(bears) shouldBe false

            withClue("your next turn is the one after this, so the lock outlasts this turn's cleanup") {
                game.toUpkeepOf(game.player1Id)
                game.state.getEntity(bears)!!.get<PhasedOutComponent>()!!.phaseInLock
                    .shouldBeInstanceOf<PhaseInLock.UntilEndOfControllersNextTurn>()
            }

            game.toUpkeepOf(game.player2Id)
            withClue("your next turn has ended, so its controller's untap step phases it in") {
                game.isPhasedIn(bears) shouldBe true
            }
        }

        test("only the WhileYouControlSource and EndOfYourNextTurn windows are accepted") {
            shouldThrow<IllegalArgumentException> { PhaseOutEffect(cantPhaseIn = Duration.EndOfTurn) }
        }
    }
}
