package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Brimstone Roundup (OTJ #115, {1}{R} Enchantment).
 *
 *   Whenever you cast your second spell each turn, create a 1/1 red Mercenary creature token with
 *   "{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 *   Plot {2}{R}
 *
 * The trigger reuses [com.wingedsheep.sdk.dsl.Triggers.NthSpellCast] (n = 2, You), and the token is
 * the standard OTJ Mercenary (same shape as Form a Posse). Plot is the shared keyword and is covered
 * by other plot scenario tests; here we exercise the unique second-spell trigger + token ability.
 */
class BrimstoneRoundupScenarioTest : ScenarioTestBase() {

    init {
        context("Brimstone Roundup") {

            test("casting your second spell each turn creates one Mercenary token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Brimstone Roundup")
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First spell of the turn — no trigger.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                game.state.getBattlefield().count { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Mercenary Token"
                } shouldBe 0

                // Second spell — triggers the token creation.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                withClue("The second spell created exactly one Mercenary token") {
                    game.state.getBattlefield().count { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Mercenary Token"
                    } shouldBe 1
                }
            }

            test("the Mercenary token's tap ability gives a creature you control +1/+0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Brimstone Roundup")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 vanilla
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                val token = game.findPermanent("Mercenary Token")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // The token is a creature — its {T} ability is summoning-sick the turn it enters
                // (CR 302.6). Clear it so we can exercise the ability itself.
                game.state = game.state.withEntity(
                    token, game.state.getEntity(token)!!.without<SummoningSicknessComponent>()
                )

                val abilityId = game.state.grantedActivatedAbilities.first { it.entityId == token }.ability.id
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = token,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating the token's tap ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears gets +1/+0 (becomes 3/2)") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
