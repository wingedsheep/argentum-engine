package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lys Alana Dignitary (Lorwyn Eclipsed).
 *
 * Lys Alana Dignitary:
 * - {1}{G} Creature — Elf Advisor 2/3
 * - As an additional cost to cast this spell, behold an Elf or pay {2}.
 * - {T}: Add {G}{G}. Activate only if there is an Elf card in your graveyard.
 */
class LysAlanaDignitaryScenarioTest : ScenarioTestBase() {

    init {
        context("BeholdOrPay additional cost") {
            test("casting with an Elf on the battlefield uses the behold path (base {1}{G})") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lys Alana Dignitary")
                    .withCardOnBattlefield(1, "Elvish Pioneer")
                    .withLandsOnBattlefield(1, "Forest", 2) // exactly {1}{G}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithBeholdCost(1, "Lys Alana Dignitary", "Elvish Pioneer")
                withClue("Should cast via behold path: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Behold does NOT exile or move the card; Elvish Pioneer is still on the battlefield
                withClue("Elvish Pioneer should still be on the battlefield (behold does not exile)") {
                    game.isOnBattlefield("Elvish Pioneer") shouldBe true
                }

                // Lys Alana Dignitary resolves
                game.resolveStack()
                withClue("Lys Alana Dignitary should enter the battlefield") {
                    game.isOnBattlefield("Lys Alana Dignitary") shouldBe true
                }
            }

            test("casting without behold requires paying {2} (total cost {3}{G})") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lys Alana Dignitary")
                    .withLandsOnBattlefield(1, "Forest", 4) // enough for {3}{G}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // No additional cost payment → pay path (extra {2})
                val hand = game.state.getHand(game.player1Id)
                val cardId = hand.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Lys Alana Dignitary"
                }
                val result = game.execute(CastSpell(game.player1Id, cardId))
                withClue("Pay path should succeed with 4 Forests: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()
                withClue("Lys Alana Dignitary should enter the battlefield") {
                    game.isOnBattlefield("Lys Alana Dignitary") shouldBe true
                }
            }

            test("pay path fails when only {1}{G} available and no Elf to behold") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lys Alana Dignitary")
                    .withLandsOnBattlefield(1, "Forest", 2) // only {1}{G} — not enough for {3}{G}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hand = game.state.getHand(game.player1Id)
                val cardId = hand.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Lys Alana Dignitary"
                }
                val result = game.execute(
                    CastSpell(game.player1Id, cardId, additionalCostPayment = AdditionalCostPayment())
                )
                withClue("Pay path should fail with only {1}{G} available") {
                    result.error.shouldNotBeNull()
                }
            }
        }

        context("Activation restriction — only if Elf in graveyard") {
            test("tap ability adds {G}{G} when an Elf card is in your graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lys Alana Dignitary")
                    .withCardInGraveyard(1, "Elvish Pioneer")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Remove summoning sickness so the ability can be activated
                val dignitary = game.findPermanent("Lys Alana Dignitary")!!
                val dignitaryDef = cardRegistry.getCard("Lys Alana Dignitary")!!
                val abilityId = dignitaryDef.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = dignitary,
                        abilityId = abilityId
                    )
                )
                withClue("Mana ability should resolve when an Elf is in graveyard: ${result.error}") {
                    result.error shouldBe null
                }

                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
                withClue("Mana pool should contain 2 green mana") {
                    manaPool.green shouldBe 2
                }
            }

            test("tap ability cannot be activated when no Elf card is in your graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lys Alana Dignitary")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dignitary = game.findPermanent("Lys Alana Dignitary")!!
                val dignitaryDef = cardRegistry.getCard("Lys Alana Dignitary")!!
                val abilityId = dignitaryDef.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = dignitary,
                        abilityId = abilityId
                    )
                )
                withClue("Mana ability should fail without an Elf in graveyard") {
                    result.error.shouldNotBeNull()
                }
            }
        }
    }
}
