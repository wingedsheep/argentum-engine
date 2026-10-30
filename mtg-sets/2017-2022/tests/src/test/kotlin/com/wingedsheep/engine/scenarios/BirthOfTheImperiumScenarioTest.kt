package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Birth of the Imperium (40K #107).
 *
 *   I — Create a 2/2 white Astartes Warrior creature token with vigilance for each opponent you have.
 *   II — Each opponent sacrifices a creature of their choice.
 *   III — Draw two cards for each opponent who controls fewer creatures than you.
 *
 * Chapter III is the composition worth proving: `CountPlayersWith` rebinds the controller to each
 * candidate opponent, so `Player.You` inside the comparison is that opponent's creature count and
 * `Player.ControllerOfSource` is the Saga controller's. A collapsed comparison ("opponent vs.
 * opponent") would never draw; a non-strict one would draw on a tie.
 */
class BirthOfTheImperiumScenarioTest : ScenarioTestBase() {

    init {
        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Birth of the Imperium")
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        /** Sets the Saga so that the precombat-main lore counter reaches [chapter], then walks there. */
        fun TestGame.advanceToChapter(chapter: Int) {
            val saga = findPermanent("Birth of the Imperium")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, chapter - 1))
                    .with(SagaComponent(triggeredChapters = (1 until chapter).toSet()))
            }
            passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        }

        test("chapter I creates one vigilant 2/2 Astartes Warrior per opponent") {
            val game = builder().build()

            game.advanceToChapter(1)
            game.resolveStack()

            val tokens = game.findPermanents("Astartes Warrior Token")
            tokens.size shouldBe 1
            val projected = game.state.projectedState
            projected.getPower(tokens.single()) shouldBe 2
            projected.getToughness(tokens.single()) shouldBe 2
            projected.hasKeyword(tokens.single(), Keyword.VIGILANCE) shouldBe true
        }

        test("chapter II makes the opponent sacrifice a creature") {
            val game = builder()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            game.advanceToChapter(2)
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            withClue("only opponents sacrifice") {
                game.findPermanent("Hill Giant") shouldNotBe null
            }
        }

        test("chapter III draws two for an opponent who controls fewer creatures than you") {
            val game = builder()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            game.advanceToChapter(3)
            val handBefore = game.handSize(1)
            game.resolveStack()

            withClue("1 creature < 2 creatures — that opponent counts, so draw two") {
                game.handSize(1) shouldBe handBefore + 2
            }
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("Birth of the Imperium") shouldBe null
            }
        }

        test("chapter III draws nothing when the opponent controls as many creatures as you") {
            val game = builder()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            game.advanceToChapter(3)
            val handBefore = game.handSize(1)
            game.resolveStack()

            withClue("\"fewer\" is strict: a tie doesn't count") {
                game.handSize(1) shouldBe handBefore
            }
        }

        test("chapter III draws nothing when the opponent controls more creatures") {
            val game = builder()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()

            game.advanceToChapter(3)
            val handBefore = game.handSize(1)
            game.resolveStack()

            game.handSize(1) shouldBe handBefore
        }
    }
}
