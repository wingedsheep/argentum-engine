package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Defy Death (AVR #16, reprinted J22 #174) — {3}{W}{W} Sorcery.
 *
 * "Return target creature card from your graveyard to the battlefield. If it's an Angel, put two
 * +1/+1 counters on it."
 */
class DefyDeathScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Angel",
                manaCost = ManaCost.parse("{3}{W}"),
                subtypes = setOf(Subtype.ANGEL),
                power = 3,
                toughness = 3
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        fun counters(game: TestGame, id: EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Defy Death") {

            test("returns an Angel card to the battlefield with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Defy Death")
                    .withCardInGraveyard(1, "Test Angel")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingGraveyardCard(1, "Defy Death", 1, "Test Angel")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Test Angel should be on the battlefield") {
                    game.isOnBattlefield("Test Angel") shouldBe true
                }
                val angel = game.findPermanent("Test Angel")!!
                withClue("An Angel returns with two +1/+1 counters") {
                    counters(game, angel) shouldBe 2
                }
            }

            test("returns a non-Angel creature card with no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Defy Death")
                    .withCardInGraveyard(1, "Test Bear")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Defy Death", 1, "Test Bear").error shouldBe null
                game.resolveStack()

                withClue("Test Bear should be on the battlefield") {
                    game.isOnBattlefield("Test Bear") shouldBe true
                }
                val bear = game.findPermanent("Test Bear")!!
                withClue("A non-Angel gets no +1/+1 counters") {
                    counters(game, bear) shouldBe 0
                }
            }
        }
    }
}
