package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Urge to Feed (WWK #70) — {B}{B} Instant.
 *
 * "Target creature gets -3/-3 until end of turn. You may tap any number of untapped Vampire
 *  creatures you control. If you do, put a +1/+1 counter on each of those Vampires."
 *
 * Proves the resolution-time choice offers only your untapped Vampires, that counters land on
 * exactly the ones tapped, and the 2010-03-01 ruling: state-based actions wait for the whole spell,
 * so a targeted 3-toughness Vampire of your own survives by being tapped for a counter.
 */
class UrgeToFeedScenarioTest : ScenarioTestBase() {

    init {
        fun tapped(game: TestGame, id: EntityId): Boolean =
            game.state.getEntity(id)?.get<TappedComponent>() != null

        fun plusCounters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun awaitSelection(game: TestGame): SelectCardsDecision {
            var guard = 0
            while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) {
                game.resolveStack()
            }
            return game.getPendingDecision() as? SelectCardsDecision
                ?: error("expected a SelectCardsDecision; got ${game.getPendingDecision()}")
        }

        context("Urge to Feed") {

            test("kills the target and puts a counter on each Vampire tapped; only untapped Vampires are offered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Urge to Feed")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Sengir Vampire")
                    .withCardOnBattlefield(1, "Vampire Nighthawk")
                    .withCardOnBattlefield(1, "Arrogant Vampire", tapped = true)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sengir = game.findPermanent("Sengir Vampire")!!
                val nighthawk = game.findPermanent("Vampire Nighthawk")!!
                val arrogant = game.findPermanent("Arrogant Vampire")!!
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Urge to Feed", bears).error shouldBe null
                val decision = awaitSelection(game)
                withClue("Only untapped Vampires you control are offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(sengir, nighthawk)
                }
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(sengir)))
                game.resolveStack()

                withClue("Grizzly Bears got -3/-3 and died") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("The chosen Vampire is tapped and gets a +1/+1 counter") {
                    tapped(game, sengir) shouldBe true
                    plusCounters(game, sengir) shouldBe 1
                }
                withClue("Unchosen and ineligible creatures are untouched") {
                    tapped(game, nighthawk) shouldBe false
                    plusCounters(game, nighthawk) shouldBe 0
                    plusCounters(game, arrogant) shouldBe 0
                    tapped(game, giant) shouldBe false
                    plusCounters(game, giant) shouldBe 0
                }
            }

            test("a targeted 3-toughness Vampire you tap for a counter survives") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Urge to Feed")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Vampire Nighthawk")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nighthawk = game.findPermanent("Vampire Nighthawk")!!

                game.castSpell(1, "Urge to Feed", nighthawk).error shouldBe null
                val decision = awaitSelection(game)
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(nighthawk)))
                game.resolveStack()

                withClue("Nighthawk (2/3, -3/-3, +1/+1 counter) is a 0/1 and stays on the battlefield") {
                    game.isOnBattlefield("Vampire Nighthawk") shouldBe true
                    tapped(game, nighthawk) shouldBe true
                    plusCounters(game, nighthawk) shouldBe 1
                    game.state.projectedState.getToughness(nighthawk) shouldBe 1
                }
            }

            test("tapping no Vampires puts no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Urge to Feed")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Sengir Vampire")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sengir = game.findPermanent("Sengir Vampire")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Urge to Feed", giant).error shouldBe null
                val decision = awaitSelection(game)
                game.submitDecision(CardsSelectedResponse(decision.id, emptyList()))
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                tapped(game, sengir) shouldBe false
                plusCounters(game, sengir) shouldBe 0
            }
        }
    }
}
