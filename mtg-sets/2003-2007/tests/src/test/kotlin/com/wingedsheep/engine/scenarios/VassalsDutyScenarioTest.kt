package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.VassalsDuty
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vassal's Duty (CHK #48) — "{1}: The next 1 damage that would be dealt to target legendary
 * creature you control this turn is dealt to you instead."
 */
class VassalsDutyScenarioTest : ScenarioTestBase() {

    private val redirect = VassalsDuty.activatedAbilities[0].id

    private fun ScenarioTestBase.TestGame.kodamaOf(playerId: EntityId): EntityId =
        findPermanents("Kodama of the South Tree").single {
            state.getEntity(it)?.get<ControllerComponent>()?.playerId == playerId
        }

    init {
        context("Vassal's Duty") {

            fun build() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Vassal's Duty")
                .withCardOnBattlefield(1, "Kodama of the South Tree")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Kodama of the South Tree")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("redirects exactly 1 damage from the legendary creature to its controller") {
                val game = build()
                val duty = game.findPermanent("Vassal's Duty")!!
                val kodama = game.kodamaOf(game.player1Id)

                game.execute(
                    ActivateAbility(
                        game.player1Id, duty, redirect,
                        targets = listOf(ChosenTarget.Permanent(kodama))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Lightning Bolt", kodama).error shouldBe null
                game.resolveStack()

                withClue("Kodama took 2 of Bolt's 3 damage") {
                    (game.state.getEntity(kodama)?.get<DamageComponent>()?.amount ?: 0) shouldBe 2
                }
                withClue("Alice took the redirected 1 damage") { game.getLifeTotal(1) shouldBe 19 }
            }

            test("can't target a nonlegendary creature") {
                val game = build()
                val duty = game.findPermanent("Vassal's Duty")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        game.player1Id, duty, redirect,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                ).error shouldNotBe null
            }

            test("can't target a legendary creature an opponent controls") {
                val game = build()
                val duty = game.findPermanent("Vassal's Duty")!!
                val theirKodama = game.kodamaOf(game.player2Id)

                game.execute(
                    ActivateAbility(
                        game.player1Id, duty, redirect,
                        targets = listOf(ChosenTarget.Permanent(theirKodama))
                    )
                ).error shouldNotBe null
            }
        }
    }
}
