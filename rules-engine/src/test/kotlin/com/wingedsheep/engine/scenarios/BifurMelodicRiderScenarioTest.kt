package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bifur, Melodic Rider — {4}{R/W}{R/W} Legendary Creature — Dwarf Bard 4/5.
 *
 *   Storied.
 *   Whenever Bifur enters or attacks, put a +1/+1 counter on target creature.
 *   As long as you have an enduring story, if a triggered ability of a Dwarf you control triggers,
 *   that ability triggers an additional time.
 *
 * Two things the doubler's parameters have to get right, and neither is visible from the card text
 * alone. `excludeSelf = false` — the text says "a Dwarf you control", not "*another* Dwarf", and
 * Bifur is a Dwarf, so his own enters trigger is one of the abilities he doubles (the printed
 * ruling says so explicitly). And the enduring-story gate rides `AdditionalSourceTriggers.condition`
 * rather than a `ConditionalStaticAbility` wrapper, because `TriggerDetector.duplicateSourceTriggers`
 * only recognizes a *bare* `AdditionalSourceTriggers` — a wrapped one would double nothing, silently.
 *
 * Each firing is a separate trigger, not a copy, so each one picks its own target (CR 603.2d).
 */
class BifurMelodicRiderScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {

        test("without an enduring story Bifur's enters trigger fires exactly once") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bifur, Melodic Rider")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Bifur, Melodic Rider").error shouldBe null
            game.resolveStack()

            withClue("Bifur alone is one legendary permanent — no enduring story") {
                EnduringStoryService.has(game.state, game.player1Id) shouldBe false
            }

            (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            plusOneCounters(game, bears) shouldBe 1
            withClue("no second trigger is waiting behind the first") {
                game.hasPendingDecision() shouldBe false
            }
        }

        test("Bifur entering as your third legendary doubles his own enters trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bifur, Melodic Rider")
                .withCardOnBattlefield(1, "Óin the Brave")
                .withCardOnBattlefield(1, "Thorin Oakenshield")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Bifur, Melodic Rider").error shouldBe null
            game.resolveStack()

            withClue("Bifur is the third legendary permanent, so the story is on before his trigger doubles") {
                EnduringStoryService.has(game.state, game.player1Id) shouldBe true
            }

            // Two independent triggers, each choosing its own target — both aimed at the Bears here.
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            withClue("the doubler produced a second instance of Bifur's own enters trigger") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            plusOneCounters(game, bears) shouldBe 2
        }

        test("the doubler also fires for another Dwarf's trigger, and only with the story on") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bifur, Melodic Rider")
                .withCardOnBattlefield(1, "Fíli the Pathfinder")
                .withCardOnBattlefield(1, "Thorin Oakenshield")
                .withCardInHand(1, "Dwarven Mauler")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            EnduringStoryService.has(game.state, game.player1Id) shouldBe true

            game.castSpell(1, "Dwarven Mauler").error shouldBe null
            game.resolveStack()

            withClue("Fíli's Dwarf-entering trigger is doubled by Bifur, so two tokens, not one") {
                game.findPermanents("Dwarf Token").size shouldBe 2
            }
        }
    }
}
