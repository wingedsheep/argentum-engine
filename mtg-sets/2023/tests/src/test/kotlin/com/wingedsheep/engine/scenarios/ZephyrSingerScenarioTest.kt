package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Zephyr Singer (MOM #86) — {2}{U}{U} 3/4, convoke, flying, vigilance. "When this creature enters,
 * put a flying counter on each creature that convoked it."
 *
 * Only the creatures tapped for its convoke (CR 702.51c) get the counter — not an untapped
 * bystander, and not the Singer itself.
 */
class ZephyrSingerScenarioTest : ScenarioTestBase() {

    private fun flyingCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.FLYING) ?: 0

    init {
        context("Zephyr Singer") {

            test("each creature that convoked it gets a flying counter and flies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Zephyr Singer")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val (first, second, bystander) = game.findPermanents("Grizzly Bears")
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Zephyr Singer").single(),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(
                                first to ConvokePayment(color = null),
                                second to ConvokePayment(color = null)
                            )
                        )
                    )
                )
                withClue("two bears pay {2}, two Islands pay {U}{U}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val singer = game.findPermanent("Zephyr Singer")!!
                withClue("the two creatures that convoked it get a flying counter") {
                    flyingCounters(game, first) shouldBe 1
                    flyingCounters(game, second) shouldBe 1
                    game.state.projectedState.hasKeyword(first, Keyword.FLYING) shouldBe true
                }
                withClue("an untapped bystander and the Singer itself get nothing") {
                    flyingCounters(game, bystander) shouldBe 0
                    game.state.projectedState.hasKeyword(bystander, Keyword.FLYING) shouldBe false
                    flyingCounters(game, singer) shouldBe 0
                }
            }

            test("hard-cast with mana: no creature convoked it, no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Zephyr Singer")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Zephyr Singer").single()
                    )
                )
                cast.error shouldBe null
                game.resolveStack()

                flyingCounters(game, game.findPermanent("Grizzly Bears")!!) shouldBe 0
            }
        }
    }
}
