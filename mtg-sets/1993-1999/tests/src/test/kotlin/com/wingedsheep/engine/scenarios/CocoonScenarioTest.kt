package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cocoon (LEG #178) — "When this Aura enters, tap enchanted creature and put three pupa counters on
 * this Aura. Enchanted creature doesn't untap during your untap step if this Aura has a pupa counter
 * on it. At the beginning of your upkeep, remove a pupa counter from this Aura. If you can't,
 * sacrifice it, put a +1/+1 counter on enchanted creature, and that creature gains flying."
 *
 * Proved here: the pupa counters land on the Aura, not the creature; the untap lock follows the
 * Aura's counters; and the hatch — run after the Aura is already sacrificed — still finds the
 * creature it enchanted, which keeps its +1/+1 counter and flying for good.
 */
class CocoonScenarioTest : ScenarioTestBase() {

    init {
        context("Cocoon") {

            fun board() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Cocoon")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .apply {
                    repeat(12) {
                        withCardInLibrary(1, "Forest")
                        withCardInLibrary(2, "Forest")
                    }
                }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.castCocoon(target: EntityId) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first {
                        state.getEntity(it)?.get<CardComponent>()?.name == "Cocoon"
                    },
                    targets = listOf(ChosenTarget.Permanent(target))
                )
            )

            fun TestGame.pupa(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PUPA) ?: 0

            fun TestGame.plusOnes(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)!!.has<TappedComponent>()

            /** Pass from Player 1's main phase to Player 1's next precombat main. */
            fun TestGame.myNextTurnMain() {
                repeat(2) {
                    passUntilPhase(Phase.ENDING, Step.END)
                    passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                }
                state.activePlayerId shouldBe player1Id
            }

            test("enters tapping the creature, with three pupa counters on the Aura") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castCocoon(bears).error shouldBe null
                game.resolveStack()

                val cocoon = game.findPermanent("Cocoon")!!
                withClue("the enchanted creature is tapped") { game.isTapped(bears) shouldBe true }
                withClue("three pupa counters on the Aura") { game.pupa(cocoon) shouldBe 3 }
                withClue("none on the creature") { game.pupa(bears) shouldBe 0 }
            }

            test("holds the creature for three upkeeps, then hatches it into a 3/3 flier") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castCocoon(bears).error shouldBe null
                game.resolveStack()
                val cocoon = game.findPermanent("Cocoon")!!

                for (left in 2 downTo 0) {
                    game.myNextTurnMain()
                    withClue("a pupa counter at the untap step keeps it tapped") { game.isTapped(bears) shouldBe true }
                    withClue("upkeep removed one pupa counter") { game.pupa(cocoon) shouldBe left }
                }

                game.myNextTurnMain()
                withClue("no pupa counter at the untap step, so it untapped") { game.isTapped(bears) shouldBe false }
                withClue("Cocoon was sacrificed") { game.findPermanent("Cocoon") shouldBe null }
                withClue("Cocoon is in its owner's graveyard") {
                    game.state.getGraveyard(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Cocoon"
                    } shouldBe true
                }
                game.findPermanent("Grizzly Bears") shouldNotBe null
                withClue("the creature it enchanted got a +1/+1 counter") { game.plusOnes(bears) shouldBe 1 }
                withClue("and flying") { game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true }

                game.myNextTurnMain()
                withClue("the flying outlasts the Aura and the turn") {
                    game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
                }
                game.plusOnes(bears) shouldBe 1
            }
        }
    }
}
