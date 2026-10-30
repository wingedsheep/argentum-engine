package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Historian's Boon (DMC #21) — {3}{W} Enchantment.
 *
 *   Whenever this enchantment or another nontoken enchantment you control enters, create a 1/1
 *   white Soldier creature token.
 *   Whenever the final chapter ability of a Saga you control triggers, create a 4/4 white Angel
 *   creature token with flying and vigilance.
 *
 * The Angel trigger keys on the final chapter ability *triggering* (CR 714.2e), so it goes on the
 * stack above that chapter and resolves first — Birth of the Imperium's chapter III then counts the
 * Angel. Earlier chapters don't trigger it.
 */
class HistoriansBoonScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.stackSourceNames(): List<String?> = state.stack.map {
            state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.sourceName
        }

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Historian's Boon")
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

        context("Historian's Boon") {

            test("the final chapter triggering creates a 4/4 flying, vigilant Angel before the chapter resolves") {
                val game = builder().build()

                game.advanceToChapter(3)

                withClue("the Boon's trigger lands above the chapter III ability it saw") {
                    game.stackSourceNames() shouldBe listOf("Birth of the Imperium", "Historian's Boon")
                }
                val handBefore = game.handSize(1)
                game.resolveStack()

                val angel = game.findPermanents("Angel Token").single()
                val projected = game.state.projectedState
                projected.getPower(angel) shouldBe 4
                projected.getToughness(angel) shouldBe 4
                projected.hasKeyword(angel, Keyword.FLYING) shouldBe true
                projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe true
                withClue("chapter III resolved after the Angel arrived: 1 creature vs the opponent's 0, so draw two") {
                    game.handSize(1) shouldBe handBefore + 2
                }
            }

            test("an earlier chapter doesn't trigger it") {
                val game = builder().build()

                game.advanceToChapter(2)
                game.stackSourceNames() shouldBe listOf("Birth of the Imperium")
                game.resolveStack()

                game.findPermanents("Angel Token").size shouldBe 0
            }

            test("itself and another nontoken enchantment entering each create a Soldier") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Historian's Boon")
                    .withCardInHand(1, "The Flame of Keld")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Historian's Boon").error shouldBe null
                game.resolveStack()
                withClue("the Boon's own entry") { game.findPermanents("Soldier Token").size shouldBe 1 }

                game.castSpell(1, "The Flame of Keld").error shouldBe null
                game.resolveStack()
                withClue("a nontoken Saga entering") { game.findPermanents("Soldier Token").size shouldBe 2 }
                withClue("chapter I isn't the final chapter") { game.findPermanents("Angel Token").size shouldBe 0 }
            }
        }
    }
}
