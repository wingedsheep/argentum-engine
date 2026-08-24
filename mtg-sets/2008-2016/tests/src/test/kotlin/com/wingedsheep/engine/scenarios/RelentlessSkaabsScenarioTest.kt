package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Relentless Skaabs — {3}{U}{U} 4/4 Zombie with "As an additional cost to cast
 * this spell, exile a creature card from your graveyard" and undying.
 *
 * The interesting interaction is the one the printed ruling calls out: the additional cost rides the
 * *cast*, so the undying return (CR 702.92a — not a cast) exiles nothing.
 */
class RelentlessSkaabsScenarioTest : ScenarioTestBase() {

    init {
        context("Relentless Skaabs") {

            fun plusOneCounters(game: TestGame, name: String): Int =
                game.findPermanent(name)?.let { id ->
                    game.state.getEntity(id)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
                } ?: 0

            test("casting it exiles a creature card from your graveyard") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Relentless Skaabs")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .build()

                val skaabs = game.findCardsInHand(1, "Relentless Skaabs").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = skaabs,
                        targets = emptyList(),
                        additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
                    ),
                )
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("the Skaabs resolved") { game.isOnBattlefield("Relentless Skaabs") shouldBe true }
                withClue("the additional cost exiled the creature card out of the graveyard") {
                    game.state.getGraveyard(game.player1Id).none { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } shouldBe true
                    game.state.getExile(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } shouldBe true
                }
            }

            test("with no creature card in the graveyard the cost cannot be paid") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Relentless Skaabs")
                    // A noncreature card in the graveyard is not a legal exile for this cost.
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .build()

                val cast = game.castSpell(1, "Relentless Skaabs")
                withClue("an unpayable additional cost makes the cast illegal") {
                    cast.error shouldNotBe null
                }
                game.isOnBattlefield("Relentless Skaabs") shouldBe false
            }

            test("undying brings it back with a +1/+1 counter and exiles nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Relentless Skaabs")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .build()

                val skaabs = game.findPermanent("Relentless Skaabs")!!
                val kill = game.castSpell(1, "Doom Blade", targetId = skaabs)
                withClue("the removal should succeed: ${kill.error}") { kill.error shouldBe null }
                game.resolveStack()

                withClue("undying returned it") { game.isOnBattlefield("Relentless Skaabs") shouldBe true }
                withClue("it comes back with one +1/+1 counter (CR 702.92a)") {
                    plusOneCounters(game, "Relentless Skaabs") shouldBe 1
                }
                withClue("the return is not a cast, so the additional cost is never paid") {
                    game.state.getGraveyard(game.player1Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } shouldBe true
                }
            }
        }
    }
}
