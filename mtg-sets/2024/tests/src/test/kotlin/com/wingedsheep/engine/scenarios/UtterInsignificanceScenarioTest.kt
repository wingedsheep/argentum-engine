package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Utter Insignificance (MH3 #78) — {1}{U} Enchantment — Aura.
 *
 *   Flash
 *   Enchant creature
 *   Enchanted creature loses all abilities and has base power and toughness 1/1.
 *   {2}{C}: Exile enchanted creature.
 */
class UtterInsignificanceScenarioTest : ScenarioTestBase() {

    private val exileAbilityId =
        cardRegistry.getCard("Utter Insignificance")!!.activatedAbilities.first().id

    init {
        context("Utter Insignificance") {

            test("enchanted creature loses all abilities and becomes a base 1/1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Utter Insignificance")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!
                withClue("Serra Angel starts with flying") {
                    game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
                }

                val cast = game.castSpell(1, "Utter Insignificance", targetId = angel)
                withClue("Casting Utter Insignificance should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("base power and toughness become 1/1") {
                    projected.getPower(angel) shouldBe 1
                    projected.getToughness(angel) shouldBe 1
                }
                withClue("the enchanted creature loses flying and vigilance") {
                    projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
                    projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
                    projected.hasLostAllAbilities(angel) shouldBe true
                }
            }

            test("{2}{C}: exiles the enchanted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Utter Insignificance")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Wastes", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Utter Insignificance", targetId = bears)
                game.resolveStack()

                val aura = game.findPermanent("Utter Insignificance")
                withClue("Aura should be on the battlefield before activation") {
                    aura.shouldNotBeNull()
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = aura!!,
                        abilityId = exileAbilityId,
                    )
                )
                withClue("Activating the exile ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the enchanted creature is exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.state.getExile(game.player2Id).contains(bears) shouldBe true
                }
            }

            test("{2}{C}: colored mana can't pay the {C}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Utter Insignificance")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Utter Insignificance", targetId = bears)
                game.resolveStack()

                val aura = game.findPermanent("Utter Insignificance")!!
                val exile = game.getLegalActions(1).firstOrNull { la ->
                    val a = la.action
                    a is ActivateAbility && a.sourceId == aura && a.abilityId == exileAbilityId
                }
                withClue("four untapped Islands can't produce {C}") {
                    (exile?.isAffordable ?: false) shouldBe false
                }
            }
        }
    }
}
