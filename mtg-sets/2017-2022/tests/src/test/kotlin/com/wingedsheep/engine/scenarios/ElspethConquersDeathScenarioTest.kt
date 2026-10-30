package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Elspeth Conquers Death (THB #13).
 *
 *   I — Exile target permanent an opponent controls with mana value 3 or greater.
 *   II — Noncreature spells your opponents cast cost {2} more to cast until your next turn.
 *   III — Return target creature or planeswalker card from your graveyard to the battlefield.
 *         Put a +1/+1 counter or a loyalty counter on it.
 *
 * Chapter II is the one worth pinning: the tax lands on the opponent, survives the opponent's own
 * untap step, and ends when the Saga controller's next turn begins.
 */
class ElspethConquersDeathScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Elspeth", "Opponent")
            .withCardOnBattlefield(1, "Elspeth Conquers Death")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }

        fun TestGame.advanceToChapter(chapter: Int) {
            val saga = findPermanent("Elspeth Conquers Death")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, chapter - 1))
                    .with(SagaComponent(triggeredChapters = (1 until chapter).toSet()))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        test("chapter I exiles an opponent's permanent with mana value 3 or greater, not a smaller one") {
            val game = builder()
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.advanceToChapter(1)
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            withClue("the 2-mana Bears is not a legal target") {
                decision.legalTargets.values.flatten().contains(bears) shouldBe false
            }
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Hill Giant") shouldBe null
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("chapter II taxes the opponent's noncreature spells until your next turn") {
            val game = builder()
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardInHand(2, "Shock")
                .withCardInHand(2, "Raging Goblin")
                .build()

            game.advanceToChapter(2)
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("the opponent's own untap step does not end it") {
                game.state.spellCostModifiers.size shouldBe 1
            }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("Shock now costs {2}{R}: two Mountains can't pay it") {
                game.castSpellTargetingPlayer(2, "Shock", 1).error shouldNotBe null
            }
            withClue("creature spells are untaxed") {
                game.castSpell(2, "Raging Goblin").error shouldBe null
            }
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            withClue("the tax ended when the Saga controller's next turn began") {
                game.state.spellCostModifiers shouldBe emptyList()
            }
        }

        test("chapter III returns a creature card and lets you choose its counter") {
            val game = builder()
                .withCardInGraveyard(1, "Hill Giant")
                .build()

            game.advanceToChapter(3)
            val giantCard = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.selectTargets(listOf(giantCard)).error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision()
            choice.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(choice.id, 0)).error shouldBe null
            game.resolveStack()

            val giant = game.findPermanent("Hill Giant")!!
            game.state.projectedState.getPower(giant) shouldBe 4
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("Elspeth Conquers Death") shouldBe null
            }
        }
    }
}
