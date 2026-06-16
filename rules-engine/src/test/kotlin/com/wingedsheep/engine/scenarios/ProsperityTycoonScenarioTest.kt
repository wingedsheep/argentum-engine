package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Prosperity Tycoon (OTJ #25) — {3}{W} 4/2 Creature — Human Noble.
 *
 *   "When this creature enters, create a 1/1 red Mercenary creature token with
 *    '{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery.'
 *    {2}, Sacrifice a token: This creature gains indestructible until end of turn. Tap it."
 *
 * Verifies (1) the ETB creates a 1/1 red Mercenary token, and (2) the {2}, Sacrifice a token
 * ability grants the Tycoon indestructible and taps it.
 */
class ProsperityTycoonScenarioTest : ScenarioTestBase() {

    init {
        context("Prosperity Tycoon") {

            test("ETB creates a Mercenary token; sac-a-token ability grants indestructible and taps Tycoon") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Prosperity Tycoon")
                    .withLandsOnBattlefield(1, "Plains", 6) // {3}{W} cast + {2} ability
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun ownedTokens() = game.state.getBattlefield().filter {
                    val e = game.state.getEntity(it)
                    e?.has<TokenComponent>() == true &&
                        e.get<ControllerComponent>()?.playerId == game.player1Id
                }

                game.castSpell(1, "Prosperity Tycoon").error shouldBe null
                game.resolveStack() // resolve the creature, then the ETB trigger

                val tokens = ownedTokens()
                withClue("ETB creates exactly one token") { tokens.size shouldBe 1 }
                val token = tokens.first()
                val tokenCard = game.state.getEntity(token)!!.get<CardComponent>()!!
                tokenCard.colors shouldBe setOf(Color.RED)
                tokenCard.typeLine.subtypes.map { it.value }.contains("Mercenary") shouldBe true

                val tycoon = game.findPermanent("Prosperity Tycoon")!!
                withClue("Tycoon starts without indestructible") {
                    game.state.projectedState.hasKeyword(tycoon, Keyword.INDESTRUCTIBLE) shouldBe false
                }

                // Activate {2}, Sacrifice a token: gains indestructible, tap it.
                val abilityId = cardRegistry.getCard("Prosperity Tycoon")!!
                    .activatedAbilities.first().id
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tycoon,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(token))
                    )
                )
                withClue("Activating the sacrifice ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The token was sacrificed as a cost") {
                    ownedTokens().contains(token) shouldBe false
                }
                withClue("Tycoon gains indestructible until end of turn") {
                    game.state.projectedState.hasKeyword(tycoon, Keyword.INDESTRUCTIBLE) shouldBe true
                }
                withClue("Tycoon is tapped by the ability") {
                    game.state.getEntity(tycoon)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
