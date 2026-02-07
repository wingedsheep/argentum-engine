package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Doomed Necromancer's activated ability.
 *
 * Card reference:
 * - Doomed Necromancer ({2}{B}): 2/2 Creature — Human Cleric Mercenary
 *   "{B}, {T}, Sacrifice Doomed Necromancer: Return target creature card from your graveyard to the battlefield."
 */
class DoomedNecromancerScenarioTest : ScenarioTestBase() {

    init {
        context("Doomed Necromancer reanimation ability") {

            test("sacrifice self to return creature from graveyard to battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doomed Necromancer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1) // {B} for activation cost
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necromancerId = game.findPermanent("Doomed Necromancer")!!
                val bearsId = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val cardDef = cardRegistry.getCard("Doomed Necromancer")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the ability targeting Grizzly Bears in graveyard
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = necromancerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD))
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Doomed Necromancer should be sacrificed as part of the cost
                withClue("Doomed Necromancer should be sacrificed to graveyard") {
                    game.isOnBattlefield("Doomed Necromancer") shouldBe false
                    game.isInGraveyard(1, "Doomed Necromancer") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Grizzly Bears should be on the battlefield
                withClue("Grizzly Bears should be returned to the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }

            test("cannot activate without black mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doomed Necromancer")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    // No lands — can't pay {B}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necromancerId = game.findPermanent("Doomed Necromancer")!!
                val bearsId = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val cardDef = cardRegistry.getCard("Doomed Necromancer")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = necromancerId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Card(bearsId, game.player1Id, Zone.GRAVEYARD))
                    )
                )

                withClue("Activation should fail without mana") {
                    result.error shouldNotBe null
                }
            }

            test("cannot activate without a creature in graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doomed Necromancer")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    // No creatures in graveyard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val necromancerId = game.findPermanent("Doomed Necromancer")!!

                val cardDef = cardRegistry.getCard("Doomed Necromancer")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = necromancerId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )

                withClue("Activation should fail without valid targets") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
