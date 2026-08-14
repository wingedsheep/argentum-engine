package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Last Light of Durin's Day (HOB #103) — {1}{R} Enchantment.
 *
 * "Whenever a Mountain you control enters, put a quest counter on this enchantment. If it has six
 * or more quest counters on it, sacrifice it. If you do, search your hand and/or library for a
 * Dragon card and put it onto the battlefield. If you search your library this way, shuffle."
 * Mountaincycling {2}.
 *
 * Covers the three things composition could get wrong: the trigger firing on a Mountain (and only
 * once, below threshold, without paying off), the six-counter payoff sacrificing the enchantment
 * and reanimating a Dragon, and the search reaching the *hand* as well as the library — the half
 * that a plain `searchLibrary` would silently drop.
 */
class LastLightOfDurinsDayScenarioTest : ScenarioTestBase() {

    private fun questCount(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.QUEST) ?: 0

    private fun seedQuestCounters(game: TestGame, id: EntityId, count: Int) {
        game.state = game.state.updateEntity(id) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.QUEST, count))
        }
    }

    private fun playMountain(game: TestGame) {
        val mountain = game.findCardsInHand(1, "Mountain").first()
        game.execute(PlayLand(game.player1Id, mountain)).error shouldBe null
        game.resolveStack()
    }

    init {
        test("a Mountain entering adds a quest counter and, below six, nothing else happens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Last Light of Durin's Day")
                .withCardInHand(1, "Mountain")
                .withCardInLibrary(1, "Shivan Dragon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lastLight = game.findPermanent("Last Light of Durin's Day")!!

            playMountain(game)

            withClue("one quest counter, enchantment still around, no Dragon yet") {
                questCount(game, lastLight) shouldBe 1
                game.isOnBattlefield("Last Light of Durin's Day") shouldBe true
                game.isOnBattlefield("Shivan Dragon") shouldBe false
            }
        }

        test("the sixth quest counter sacrifices it and puts a Dragon from the library onto the battlefield") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Last Light of Durin's Day")
                .withCardInHand(1, "Mountain")
                .withCardInLibrary(1, "Shivan Dragon")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lastLight = game.findPermanent("Last Light of Durin's Day")!!
            seedQuestCounters(game, lastLight, 5)

            playMountain(game)

            // The search pauses to choose the Dragon among the eligible cards.
            val search = game.getPendingDecision()
            withClue("the payoff pauses for the hand/library search") { (search != null) shouldBe true }
            game.selectCards(game.findCardsInLibrary(1, "Shivan Dragon"))
            game.resolveStack()

            withClue("sacrificed itself and reanimated the Dragon onto the battlefield") {
                game.isOnBattlefield("Last Light of Durin's Day") shouldBe false
                game.isInGraveyard(1, "Last Light of Durin's Day") shouldBe true
                game.isOnBattlefield("Shivan Dragon") shouldBe true
            }
        }

        test("the search also reaches a Dragon in hand, not just the library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Last Light of Durin's Day")
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Shivan Dragon")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lastLight = game.findPermanent("Last Light of Durin's Day")!!
            seedQuestCounters(game, lastLight, 5)

            playMountain(game)

            val search = game.getPendingDecision()
            withClue("the Dragon in hand is offered by the search") { (search != null) shouldBe true }
            game.selectCards(game.findCardsInHand(1, "Shivan Dragon"))
            game.resolveStack()

            withClue("the hand Dragon is on the battlefield and no longer in hand") {
                game.isOnBattlefield("Shivan Dragon") shouldBe true
                game.isInHand(1, "Shivan Dragon") shouldBe false
            }
        }
    }
}
