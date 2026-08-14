package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hollow Scavenger // Bakery Raid (WOE #174).
 *
 * Creature face: {2}{G} 3/2 Wolf — "{1}, Sacrifice a Food: This creature gets +2/+2 until end of
 * turn. Activate only once each turn."
 * Adventure face: Bakery Raid {G}, Sorcery — Adventure — "Create a Food token."
 */
class HollowScavengerScenarioTest : ScenarioTestBase() {

    private val pumpAbilityId by lazy {
        cardRegistry.requireCard("Hollow Scavenger").activatedAbilities[0].id
    }

    init {
        test("Bakery Raid creates a Food token and exiles the card for later") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Hollow Scavenger")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scavenger = game.findCardsInHand(1, "Hollow Scavenger").single()

            // faceIndex = 0 is the Adventure face (CR 715).
            game.execute(CastSpell(playerId = game.player1Id, cardId = scavenger, faceIndex = 0))
                .error shouldBe null
            game.resolveStack()

            withClue("a Food token entered under the caster's control") {
                game.findPermanent("Food") shouldNotBe null
            }
            withClue("the Adventure exiled itself (CR 715.3d)") {
                game.isInExile(1, "Hollow Scavenger") shouldBe true
                game.isInGraveyard(1, "Hollow Scavenger") shouldBe false
            }
        }

        test("sacrificing a Food pumps it +2/+2, and the ability can only be activated once a turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Hollow Scavenger", summoningSickness = false)
                .withCardOnBattlefield(1, "Food")
                .withCardOnBattlefield(1, "Food")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val scavenger = game.findPermanent("Hollow Scavenger")!!
            val foods = game.findPermanents("Food")
            foods.size shouldBe 2

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = scavenger,
                    abilityId = pumpAbilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(foods[0]))
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the Food was eaten and the Wolf is a 5/4 until end of turn") {
                game.isOnBattlefield("Food") shouldBe true // the second Food is untouched
                game.findPermanents("Food").size shouldBe 1
                game.state.projectedState.getPower(scavenger) shouldBe 5
                game.state.projectedState.getToughness(scavenger) shouldBe 4
            }

            withClue("\"Activate only once each turn\" blocks the second activation") {
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = scavenger,
                        abilityId = pumpAbilityId,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanents("Food").single())
                        )
                    )
                ).error shouldNotBe null
            }
        }
    }
}
