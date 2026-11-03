package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The City on the Edge of Forever — {3}{W}{B} Enchantment — Saga (TRK #228).
 *
 * I — All artifacts and creatures phase out. Create a 0/1 white Human creature token with
 *     "Permanents can't phase in."
 * II — You draw two cards and each player loses 2 life.
 * III — Sacrifice a creature token. If you do, you gain 5 life.
 *
 * The Human holds every phased-out permanent out through the untap steps until chapter III
 * sacrifices it; after that they phase in at their controllers' next untap steps.
 */
class TheCityOnTheEdgeOfForeverScenarioTest : ScenarioTestBase() {

    private fun TestGame.isPhasedIn(id: EntityId) = id in state.getBattlefield()

    private fun TestGame.human(): EntityId? = state.getBattlefield().firstOrNull {
        state.getEntity(it)?.get<CardComponent>()?.name?.startsWith("Human") == true
    }

    private fun TestGame.toUpkeepOf(playerId: EntityId) {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        state.activePlayerId shouldBe playerId
    }

    /** Move to [playerId]'s precombat main and let the chapter trigger there resolve. */
    private fun TestGame.toChapterOf(playerId: EntityId) {
        toUpkeepOf(playerId)
        passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        resolveStack()
    }

    init {
        test("chapter I phases artifacts and creatures out and the Human keeps them out until chapter III") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(2, "Runeclaw Bear")
                .withCardInHand(1, "The City on the Edge of Forever")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(10) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Plains") } }
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val thopter = game.findPermanent("Ornithopter")!!
            val runeclaw = game.findPermanent("Runeclaw Bear")!!
            val lands = game.state.getBattlefield().filter { it != bears && it != thopter && it != runeclaw }

            game.castSpell(1, "The City on the Edge of Forever").error shouldBe null
            game.resolveStack()

            withClue("chapter I: every artifact and creature phased out; lands and the Saga stay") {
                game.isPhasedIn(bears) shouldBe false
                game.isPhasedIn(thopter) shouldBe false
                game.isPhasedIn(runeclaw) shouldBe false
                lands.all { game.isPhasedIn(it) } shouldBe true
            }
            val human = game.human()!!
            withClue("a 0/1 white Human token was created after the phase-out, so it stays in") {
                game.state.projectedState.getPower(human) shouldBe 0
                game.state.projectedState.getToughness(human) shouldBe 1
                game.state.projectedState.getColors(human) shouldBe setOf(Color.WHITE.name)
            }

            withClue("the opponent's untap step leaves their creature phased out") {
                game.toUpkeepOf(game.player2Id)
                game.isPhasedIn(runeclaw) shouldBe false
            }

            val handBefore = game.handSize(1)
            game.toChapterOf(game.player1Id)
            withClue("your untap step leaves yours phased out too") {
                game.isPhasedIn(bears) shouldBe false
                game.isPhasedIn(thopter) shouldBe false
            }
            withClue("chapter II: you draw two (plus the draw step) and each player loses 2") {
                game.handSize(1) shouldBe handBefore + 3
                game.getLifeTotal(1) shouldBe 18
                game.getLifeTotal(2) shouldBe 18
            }

            game.toUpkeepOf(game.player2Id)
            game.isPhasedIn(runeclaw) shouldBe false

            game.toUpkeepOf(game.player1Id)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectCards(listOf(human))
                game.resolveStack()
            }
            withClue("chapter III: the Human is sacrificed, so you gain 5") {
                (human in game.state.getBattlefield()) shouldBe false
                game.getLifeTotal(1) shouldBe 23
            }
            withClue("the lock is gone, but nothing phases in until an untap step") {
                game.isPhasedIn(bears) shouldBe false
                game.isPhasedIn(runeclaw) shouldBe false
            }

            game.toUpkeepOf(game.player2Id)
            game.isPhasedIn(runeclaw) shouldBe true
            game.toUpkeepOf(game.player1Id)
            game.isPhasedIn(bears) shouldBe true
            game.isPhasedIn(thopter) shouldBe true
        }

        test("chapter III with no creature token to sacrifice gains no life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "The City on the Edge of Forever")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(10) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Plains") } }
                .build()
            val saga = game.findPermanent("The City on the Edge of Forever")!!
            game.state = game.state.updateEntity(saga) {
                it.with(CountersComponent(mapOf(CounterType.LORE to 2)))
            }

            game.toUpkeepOf(game.player2Id)
            game.toUpkeepOf(game.player1Id)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            withClue("a nontoken creature can't pay for it") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
