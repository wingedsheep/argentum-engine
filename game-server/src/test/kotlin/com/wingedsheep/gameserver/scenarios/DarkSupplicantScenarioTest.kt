package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Dark Supplicant.
 *
 * Card reference:
 * - Dark Supplicant ({B}): 1/1 Creature — Human Cleric
 *   "{T}, Sacrifice three Clerics: Search your graveyard, hand, and/or library for a card
 *   named Scion of Darkness and put it onto the battlefield. If you search your library
 *   this way, shuffle."
 */
class DarkSupplicantScenarioTest : ScenarioTestBase() {

    init {
        context("Dark Supplicant activated ability") {

            test("sacrifice three Clerics to find Scion of Darkness from library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dark Supplicant")
                    .withCardOnBattlefield(1, "Blood Celebrant")
                    .withCardOnBattlefield(1, "Daru Mender")
                    .withCardOnBattlefield(1, "Starlight Invoker")
                    .withCardInLibrary(1, "Scion of Darkness")
                    .withCardInLibrary(1, "Swamp") // padding
                    .withCardInLibrary(2, "Swamp") // padding
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val supplicantId = game.findPermanent("Dark Supplicant")!!
                val celebrantId = game.findPermanent("Blood Celebrant")!!
                val menderId = game.findPermanent("Daru Mender")!!
                val invokerId = game.findPermanent("Starlight Invoker")!!

                val cardDef = cardRegistry.getCard("Dark Supplicant")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate: {T}, Sacrifice three Clerics
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = supplicantId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(celebrantId, menderId, invokerId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Three Clerics should be sacrificed
                withClue("Blood Celebrant should be in graveyard") {
                    game.isInGraveyard(1, "Blood Celebrant") shouldBe true
                }
                withClue("Daru Mender should be in graveyard") {
                    game.isInGraveyard(1, "Daru Mender") shouldBe true
                }
                withClue("Starlight Invoker should be in graveyard") {
                    game.isInGraveyard(1, "Starlight Invoker") shouldBe true
                }

                // Resolve the ability on stack
                game.resolveStack()

                // Should get a decision to select Scion of Darkness from search
                withClue("Should have a pending decision to select from search") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Scion of Darkness from the search options
                val decision = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(decision.options.take(1))

                // Scion of Darkness should be on the battlefield
                withClue("Scion of Darkness should be on the battlefield") {
                    game.isOnBattlefield("Scion of Darkness") shouldBe true
                }
            }

            test("sacrifice three Clerics to find Scion of Darkness from graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dark Supplicant")
                    .withCardOnBattlefield(1, "Blood Celebrant")
                    .withCardOnBattlefield(1, "Daru Mender")
                    .withCardOnBattlefield(1, "Starlight Invoker")
                    .withCardInGraveyard(1, "Scion of Darkness")
                    .withCardInLibrary(1, "Swamp") // padding
                    .withCardInLibrary(2, "Swamp") // padding
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val supplicantId = game.findPermanent("Dark Supplicant")!!
                val celebrantId = game.findPermanent("Blood Celebrant")!!
                val menderId = game.findPermanent("Daru Mender")!!
                val invokerId = game.findPermanent("Starlight Invoker")!!

                val cardDef = cardRegistry.getCard("Dark Supplicant")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = supplicantId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(celebrantId, menderId, invokerId)
                        )
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Select Scion of Darkness from graveyard search
                withClue("Should have a pending decision") {
                    game.hasPendingDecision() shouldBe true
                }

                val scionId = game.findCardsInGraveyard(1, "Scion of Darkness").first()
                game.selectCards(listOf(scionId))

                withClue("Scion of Darkness should be on the battlefield") {
                    game.isOnBattlefield("Scion of Darkness") shouldBe true
                }
            }

            test("cannot activate with fewer than three Clerics") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dark Supplicant")
                    .withCardOnBattlefield(1, "Blood Celebrant")
                    // Only 2 Clerics total (Dark Supplicant + Blood Celebrant), need 3 to sacrifice
                    .withCardInLibrary(1, "Scion of Darkness")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val supplicantId = game.findPermanent("Dark Supplicant")!!
                val celebrantId = game.findPermanent("Blood Celebrant")!!

                val cardDef = cardRegistry.getCard("Dark Supplicant")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = supplicantId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(supplicantId, celebrantId)
                        )
                    )
                )

                withClue("Activation should fail with only 2 sacrifice targets") {
                    result.error shouldNotBe null
                }
            }

            test("fail to find if no Scion of Darkness in any zone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dark Supplicant")
                    .withCardOnBattlefield(1, "Blood Celebrant")
                    .withCardOnBattlefield(1, "Daru Mender")
                    .withCardOnBattlefield(1, "Starlight Invoker")
                    // No Scion of Darkness anywhere
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val supplicantId = game.findPermanent("Dark Supplicant")!!
                val celebrantId = game.findPermanent("Blood Celebrant")!!
                val menderId = game.findPermanent("Daru Mender")!!
                val invokerId = game.findPermanent("Starlight Invoker")!!

                val cardDef = cardRegistry.getCard("Dark Supplicant")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = supplicantId,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(celebrantId, menderId, invokerId)
                        )
                    )
                )

                withClue("Ability should still activate (cost is paid regardless): ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Decision should show empty options; select nothing
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                }

                // No Scion should be on battlefield
                withClue("No Scion of Darkness should be on the battlefield") {
                    game.isOnBattlefield("Scion of Darkness") shouldBe false
                }
            }
        }
    }
}
