package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Mourner's Surprise — {1}{B} Sorcery
 *
 * "Return up to one target creature card from your graveyard to your hand. Create a 1/1 red
 *  Mercenary creature token with "{T}: Target creature you control gets +1/+0 until end of
 *  turn. Activate only as a sorcery.""
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.effects.CreateTokenEffect.activatedAbilities]
 * field: the created token carries an activated ability as one of its copiable values
 * ([CardComponent.copyActivatedAbilities]).
 */
class MournersSurpriseScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Mourner's Surprise") {

            test("returns a creature card from the graveyard and creates a Mercenary token with its activated ability") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Mourner's Surprise")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grizzlyInGraveyard = game.state.getGraveyard(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                val cast = game.castSpellTargetingGraveyardCard(1, "Mourner's Surprise", listOf(grizzlyInGraveyard))
                withClue("Casting Mourner's Surprise should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Creature card returned from graveyard to hand.
                withClue("Grizzly Bears should have left the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears").shouldBeFalse()
                }
                withClue("Grizzly Bears should be back in hand") {
                    game.state.getHand(game.player1Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } shouldBe true
                }

                // 1/1 red Mercenary token created.
                val token = game.findPermanent("Mercenary Token").shouldNotBeNull()
                val tokenCard = game.state.getEntity(token)!!.get<CardComponent>()!!
                game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
                tokenCard.colors shouldBe setOf(Color.RED)
                val projected = projector.project(game.state)
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1

                // The token was created with its activated ability, sorcery-speed.
                val ability = tokenCard.copyActivatedAbilities.firstOrNull().shouldNotBeNull()
                withClue("Token's tap ability must be sorcery-speed (\"Activate only as a sorcery\")") {
                    ability.timing shouldBe TimingRule.SorcerySpeed
                }
            }

            test("up to one — casting with no graveyard target still creates the token") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Mourner's Surprise")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellTargetingGraveyardCard(1, "Mourner's Surprise", emptyList())
                withClue("Casting with zero graveyard targets should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.findPermanent("Mercenary Token").shouldNotBeNull()
            }

            test("the token's {T} ability gives a creature you control +1/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Mourner's Surprise")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Mourner's Surprise", emptyList())
                game.resolveStack()

                val token = game.findPermanent("Mercenary Token").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                // The token is a creature — its {T} ability is summoning-sick the turn it enters
                // (CR 302.6). Clear it so we can exercise the ability itself.
                game.state = game.state.withEntity(
                    token, game.state.getEntity(token)!!.without<SummoningSicknessComponent>()
                )

                val abilityId = game.state.getEntity(token)!!.get<CardComponent>()!!.copyActivatedAbilities.first().id
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

                withClue("Activating a {T} ability taps the token") {
                    game.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
                }
                val projected = projector.project(game.state)
                withClue("Grizzly Bears should be 3/2 after +1/+0") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
