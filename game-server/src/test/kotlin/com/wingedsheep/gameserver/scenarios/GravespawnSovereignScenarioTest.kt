package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Gravespawn Sovereign.
 *
 * Card reference:
 * - Gravespawn Sovereign ({4}{B}{B}): 3/3 Creature — Zombie
 *   "Tap five untapped Zombies you control: Put target creature card from a graveyard
 *    onto the battlefield under your control."
 */
class GravespawnSovereignScenarioTest : ScenarioTestBase() {

    init {
        context("Gravespawn Sovereign tap five zombies ability") {

            test("reanimates creature from own graveyard by tapping five zombies") {
                val game = scenario()
                    .withPlayers("ZombieMaster", "Opponent")
                    .withCardOnBattlefield(1, "Gravespawn Sovereign")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Gravespawn Sovereign")!!
                val zombies = listOf(sovereign) +
                    game.findAllPermanents("Severed Legion") +
                    game.findAllPermanents("Festering Goblin") +
                    game.findAllPermanents("Nantuko Husk")

                withClue("Should have 5 zombies") {
                    zombies.size shouldBe 5
                }

                val bearsId = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val cardDef = cardRegistry.getCard("Gravespawn Sovereign")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sovereign,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD)),
                        costPayment = AdditionalCostPayment(tappedPermanents = zombies)
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Grizzly Bears should be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }

            test("reanimates creature from opponent's graveyard") {
                val game = scenario()
                    .withPlayers("ZombieMaster", "Opponent")
                    .withCardOnBattlefield(1, "Gravespawn Sovereign")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    .withCardInGraveyard(2, "Hill Giant") // In opponent's graveyard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Gravespawn Sovereign")!!
                val zombies = listOf(sovereign) +
                    game.findAllPermanents("Severed Legion") +
                    game.findAllPermanents("Festering Goblin") +
                    game.findAllPermanents("Nantuko Husk")

                val giantId = game.findCardsInGraveyard(2, "Hill Giant").first()

                val cardDef = cardRegistry.getCard("Gravespawn Sovereign")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sovereign,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(giantId, game.player2Id, Zone.GRAVEYARD)),
                        costPayment = AdditionalCostPayment(tappedPermanents = zombies)
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                withClue("Hill Giant should be on the battlefield under player 1's control") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe false
                }
            }

            test("cannot activate with fewer than five zombies") {
                val game = scenario()
                    .withPlayers("ZombieMaster", "Opponent")
                    .withCardOnBattlefield(1, "Gravespawn Sovereign")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    // Only 3 zombies total
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Gravespawn Sovereign")!!
                val zombies = listOf(sovereign) +
                    game.findAllPermanents("Severed Legion") +
                    game.findAllPermanents("Festering Goblin")

                withClue("Should have only 3 zombies") {
                    zombies.size shouldBe 3
                }

                val bearsId = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val cardDef = cardRegistry.getCard("Gravespawn Sovereign")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sovereign,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD)),
                        costPayment = AdditionalCostPayment(tappedPermanents = zombies)
                    )
                )

                withClue("Ability should fail with insufficient zombies") {
                    result.error shouldNotBe null
                }
            }

            test("cannot activate without a creature in any graveyard") {
                val game = scenario()
                    .withPlayers("ZombieMaster", "Opponent")
                    .withCardOnBattlefield(1, "Gravespawn Sovereign")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withCardOnBattlefield(1, "Severed Legion")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withCardOnBattlefield(1, "Nantuko Husk")
                    // No creatures in any graveyard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sovereign = game.findPermanent("Gravespawn Sovereign")!!
                val zombies = listOf(sovereign) +
                    game.findAllPermanents("Severed Legion") +
                    game.findAllPermanents("Festering Goblin") +
                    game.findAllPermanents("Nantuko Husk")

                val cardDef = cardRegistry.getCard("Gravespawn Sovereign")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sovereign,
                        abilityId = ability.id,
                        targets = emptyList(),
                        costPayment = AdditionalCostPayment(tappedPermanents = zombies)
                    )
                )

                withClue("Activation should fail without valid targets") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
