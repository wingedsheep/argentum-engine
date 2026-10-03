package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fowl Strike (MH3 #155) — {1}{G} Instant.
 *
 *   Destroy target creature with flying.
 *   Reinforce 2—{2}{G} ({2}{G}, Discard this card: Put two +1/+1 counters on target creature.)
 */
class FowlStrikeScenarioTest : ScenarioTestBase() {

    private fun reinforceAbilityId() = cardRegistry.getCard("Fowl Strike")!!
        .activatedAbilities.first { it.activateFromZone == Zone.HAND }.id

    init {
        context("Fowl Strike") {

            test("destroys a creature with flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fowl Strike")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drake = game.findPermanent("Wind Drake")!!
                game.castSpell(1, "Fowl Strike", drake).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Wind Drake") shouldBe false
                game.isInGraveyard(1, "Fowl Strike") shouldBe true
            }

            test("cannot target a creature without flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fowl Strike")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Fowl Strike", bears).error shouldNotBe null
            }

            test("reinforce discards it from hand to put two +1/+1 counters on target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fowl Strike")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handCard = game.findCardsInHand(1, "Fowl Strike").first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = handCard,
                        abilityId = reinforceAbilityId(),
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("reinforce activation: ${result.error}") { result.error shouldBe null }
                game.isInGraveyard(1, "Fowl Strike") shouldBe true
                game.resolveStack()

                val counters = game.state.getEntity(bears)?.get<CountersComponent>()?.counters ?: emptyMap()
                counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
            }
        }
    }
}
