package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Angelic Intervention (MOM #5) — {1}{W} Instant.
 *
 * "Target creature or planeswalker you control gains protection from colorless or from the color of
 *  your choice until end of turn. If it's a creature, put a +1/+1 counter on it."
 */
class AngelicInterventionScenarioTest : ScenarioTestBase() {

    private val rodAbilityId = cardRegistry.getCard("Rod of Ruin")!!.activatedAbilities.first().id

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast Angelic Intervention on [targetName] and answer the quality choice with [option]. */
    private fun castAndChoose(game: TestGame, targetName: String, option: String) {
        val target = game.findPermanent(targetName)!!
        val cast = game.castSpell(1, "Angelic Intervention", target)
        withClue("Casting Angelic Intervention should succeed: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()

        val decision = game.getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("Colorless plus the five colors are offered") {
            decision.options shouldBe listOf("Colorless", "White", "Blue", "Black", "Red", "Green")
        }
        game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(option)))
    }

    private fun activateRod(game: TestGame, target: EntityId) = game.execute(
        ActivateAbility(
            playerId = game.player1Id,
            sourceId = game.findPermanent("Rod of Ruin")!!,
            abilityId = rodAbilityId,
            targets = listOf(ChosenTarget.Permanent(target)),
        )
    )

    init {
        context("Angelic Intervention") {

            test("colorless: +1/+1 counter, and a colorless source can no longer target the creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Intervention")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Rod of Ruin")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                castAndChoose(game, "Grizzly Bears", "Colorless")

                game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_COLORLESS") shouldBe true
                withClue("It's a creature, so it gets a +1/+1 counter") { plusOneCounters(game, bears) shouldBe 1 }

                withClue("Rod of Ruin is colorless, so its ability can't target the protected creature") {
                    activateRod(game, bears).error shouldNotBe null
                }
                withClue("A red spell still can") {
                    game.castSpell(1, "Shock", bears).error shouldBe null
                }
            }

            test("a color: spells of that color can't target it, colorless sources still can") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Intervention")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Rod of Ruin")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                castAndChoose(game, "Grizzly Bears", "Red")

                game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe true
                game.state.projectedState.hasKeyword(bears, "PROTECTION_FROM_COLORLESS") shouldBe false
                withClue("Shock is red and can't target it") {
                    game.castSpell(1, "Shock", bears).error shouldNotBe null
                }
                withClue("Rod of Ruin is colorless and still can") {
                    activateRod(game, bears).error shouldBe null
                }
            }

            test("a planeswalker gains the protection but no +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Angelic Intervention")
                    .withCardOnBattlefield(1, "Ajani Goldmane")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val ajani = game.findPermanent("Ajani Goldmane").shouldNotBeNull()

                castAndChoose(game, "Ajani Goldmane", "Colorless")

                game.state.projectedState.hasKeyword(ajani, "PROTECTION_FROM_COLORLESS") shouldBe true
                plusOneCounters(game, ajani) shouldBe 0
            }
        }
    }
}
