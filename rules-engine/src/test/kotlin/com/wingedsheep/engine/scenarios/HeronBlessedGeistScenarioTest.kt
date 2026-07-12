package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Heron-Blessed Geist (VOW #19) — {4}{W} Creature — Spirit, 3/3.
 *
 *   Flying
 *   {3}{W}, Exile this card from your graveyard: Create two 1/1 white Spirit creature tokens
 *   with flying. Activate only if you control an enchantment and only as a sorcery.
 *
 * Exercises the graveyard-activated ability: it requires controlling an enchantment, and paying
 * {3}{W} + exiling the card from the graveyard creates two 1/1 flying Spirit tokens.
 */
class HeronBlessedGeistScenarioTest : ScenarioTestBase() {

    init {
        context("Heron-Blessed Geist — graveyard activation gated on controlling an enchantment") {

            test("with an enchantment you control, paying {3}{W} and exiling creates two 1/1 flying Spirits") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Heron-Blessed Geist")
                    .withCardOnBattlefield(1, "Test Enchantment")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val geist = game.findCardsInGraveyard(1, "Heron-Blessed Geist").single()
                val abilityId = cardRegistry.getCard("Heron-Blessed Geist")!!.activatedAbilities.first().id

                val spiritsBefore = game.findPermanents("Spirit Token").size

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = geist,
                        abilityId = abilityId
                    )
                )
                withClue("activation should succeed while controlling an enchantment: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("the card is exiled from the graveyard") {
                    game.isInGraveyard(1, "Heron-Blessed Geist") shouldBe false
                    game.isInExile(1, "Heron-Blessed Geist") shouldBe true
                }
                withClue("two 1/1 white Spirit tokens with flying are created") {
                    game.findPermanents("Spirit Token").size shouldBe spiritsBefore + 2
                }
            }

            test("without controlling an enchantment, the ability cannot be activated") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Heron-Blessed Geist")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val geist = game.findCardsInGraveyard(1, "Heron-Blessed Geist").single()
                val abilityId = cardRegistry.getCard("Heron-Blessed Geist")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = geist,
                        abilityId = abilityId
                    )
                )
                withClue("activation must fail without controlling an enchantment") {
                    (activation.error != null) shouldBe true
                }
                withClue("Heron-Blessed Geist stays in the graveyard") {
                    game.isInGraveyard(1, "Heron-Blessed Geist") shouldBe true
                }
            }
        }
    }
}
