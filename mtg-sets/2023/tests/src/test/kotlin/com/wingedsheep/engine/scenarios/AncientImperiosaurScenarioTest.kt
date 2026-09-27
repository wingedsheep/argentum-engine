package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ancient Imperiosaur (MOM #174) — {5}{G}{G} 6/6, convoke, trample, ward {2}. "This creature
 * enters with two +1/+1 counters on it for each creature that convoked it."
 *
 * The count is the creatures tapped for its convoke (CR 702.51c); a hard-cast Imperiosaur enters
 * with none.
 */
class AncientImperiosaurScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Ancient Imperiosaur") {

            test("three creatures convoke it: it enters with six +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ancient Imperiosaur")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanents("Grizzly Bears")
                bears.size shouldBe 3
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Ancient Imperiosaur").single(),
                        alternativePayment = AlternativePaymentChoice(
                            convokedCreatures = mapOf(
                                bears[0] to ConvokePayment(color = Color.GREEN),
                                bears[1] to ConvokePayment(color = null),
                                bears[2] to ConvokePayment(color = null)
                            )
                        )
                    )
                )
                withClue("three bears + four Forests pay {5}{G}{G}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.findPermanent("Ancient Imperiosaur").shouldNotBeNull()
                withClue("two +1/+1 counters for each of the three creatures that convoked it") {
                    plusOnes(game, "Ancient Imperiosaur") shouldBe 6
                }
            }

            test("cast entirely with mana: nothing convoked it, so no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ancient Imperiosaur")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = game.findCardsInHand(1, "Ancient Imperiosaur").single()
                    )
                )
                cast.error shouldBe null
                game.resolveStack()

                plusOnes(game, "Ancient Imperiosaur") shouldBe 0
            }
        }
    }
}
