package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Nova Cleric's activated ability.
 *
 * Card reference:
 * - Nova Cleric (W): 1/2 Creature — Human Cleric
 *   "{2}{W}, {T}, Sacrifice Nova Cleric: Destroy all enchantments."
 */
class NovaClericScenarioTest : ScenarioTestBase() {

    private val testEnchantment1 = CardDefinition.enchantment(
        name = "Test Enchantment A",
        manaCost = ManaCost.parse("{1}{W}")
    )

    private val testEnchantment2 = CardDefinition.enchantment(
        name = "Test Enchantment B",
        manaCost = ManaCost.parse("{1}{U}")
    )

    init {
        cardRegistry.register(testEnchantment1)
        cardRegistry.register(testEnchantment2)

        context("Nova Cleric activated ability") {

            test("destroys all enchantments on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nova Cleric")
                    .withCardOnBattlefield(1, "Test Enchantment A")
                    .withCardOnBattlefield(2, "Test Enchantment B")
                    .withLandsOnBattlefield(1, "Plains", 3) // {2}{W} for activation cost
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val clericId = game.findPermanent("Nova Cleric")!!

                val cardDef = cardRegistry.getCard("Nova Cleric")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the ability (no targets — destroys all enchantments)
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = clericId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Nova Cleric should be sacrificed as part of the cost
                withClue("Nova Cleric should be sacrificed to graveyard") {
                    game.isOnBattlefield("Nova Cleric") shouldBe false
                    game.isInGraveyard(1, "Nova Cleric") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Both enchantments should be destroyed
                withClue("Player 1's enchantment should be destroyed") {
                    game.isOnBattlefield("Test Enchantment A") shouldBe false
                    game.isInGraveyard(1, "Test Enchantment A") shouldBe true
                }

                withClue("Player 2's enchantment should be destroyed") {
                    game.isOnBattlefield("Test Enchantment B") shouldBe false
                    game.isInGraveyard(2, "Test Enchantment B") shouldBe true
                }
            }

            test("does not destroy non-enchantment permanents") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nova Cleric")
                    .withCardOnBattlefield(1, "Test Enchantment A")
                    .withCardOnBattlefield(2, "Grizzly Bears") // creature — should survive
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val clericId = game.findPermanent("Nova Cleric")!!

                val cardDef = cardRegistry.getCard("Nova Cleric")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = clericId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Enchantment should be destroyed
                withClue("Enchantment should be destroyed") {
                    game.isOnBattlefield("Test Enchantment A") shouldBe false
                }

                // Grizzly Bears should survive
                withClue("Grizzly Bears should still be on battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("cannot activate without enough mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nova Cleric")
                    .withCardOnBattlefield(2, "Test Enchantment A")
                    .withLandsOnBattlefield(1, "Plains", 1) // only 1 mana, need 3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val clericId = game.findPermanent("Nova Cleric")!!

                val cardDef = cardRegistry.getCard("Nova Cleric")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = clericId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )

                withClue("Activation should fail without enough mana") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
