package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Harald Unites the Elves (KHM #213).
 *
 *   I — Mill three cards. You may put an Elf or Tyvar card from your graveyard onto the battlefield.
 *   III — Whenever an Elf you control attacks this turn, target creature an opponent controls gets
 *         -1/-1 until end of turn.
 *
 * Pins chapter I's "Elf or Tyvar" choice over the whole graveyard, and chapter III's delayed trigger:
 * it outlives the sacrificed Saga, fires only for attacking Elves you control, and targets each time.
 */
class HaraldUnitesTheElvesScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Harald Unites the Elves")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(5) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }

        /** Set the Saga one counter short of [chapter] and advance to the main phase so it triggers. */
        fun TestGame.playChapter(chapter: Int) {
            val saga = findPermanent("Harald Unites the Elves")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, chapter - 1))
                    .with(SagaComponent(triggeredChapters = (1 until chapter).toSet()))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            resolveStack()
        }

        test("chapter I mills three and may put an Elf or Tyvar card from the graveyard onto the battlefield") {
            val game = builder()
                .withCardInGraveyard(1, "Tyvar Kell")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withCardInGraveyard(1, "Grizzly Bears")
                .build()
            game.playChapter(1)

            val tyvar = game.findCardsInGraveyard(1, "Tyvar Kell").single()
            val elf = game.findCardsInGraveyard(1, "Llanowar Elves").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.minSelections shouldBe 0
            decision.maxSelections shouldBe 1
            decision.options shouldContain tyvar
            decision.options shouldContain elf
            withClue("a non-Elf, non-Tyvar card is not offered") { decision.options shouldNotContain bears }
            withClue("three cards milled") { game.findCardsInGraveyard(1, "Forest").size shouldBe 3 }

            game.selectCards(listOf(tyvar)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Tyvar Kell") shouldBe true
            game.isInGraveyard(1, "Llanowar Elves") shouldBe true
        }

        test("chapter III shrinks a target for each attacking Elf, even after the Saga is gone") {
            val game = builder()
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.playChapter(3)
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("Harald Unites the Elves") shouldBe null
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Llanowar Elves" to 2, "Grizzly Bears" to 2)).error shouldBe null
            withClue("only the attacking Elf triggers") {
                game.selectTargets(listOf(giant)).error shouldBe null
            }
            game.resolveStack()

            game.state.projectedState.getPower(giant) shouldBe 2
            game.state.projectedState.getToughness(giant) shouldBe 2
            withClue("no second trigger for the non-Elf attacker") { game.state.stack.isEmpty() shouldBe true }
        }
    }
}
