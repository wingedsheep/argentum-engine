package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `ReturnSelfFromZoneTransformedEffect.controllerOverride` — who controls the back face that a
 * "return this card to the battlefield transformed" puts onto the battlefield.
 *
 * Without an override the card enters under its owner's control (the god cycle's "return it to
 * the battlefield", an object put onto the battlefield by an effect enters under its owner's
 * control unless the effect says otherwise). With `underYourControl = true` it enters under the
 * ability's controller (Journey to Eternity's "...transformed under your control"), which differs
 * when the controller doesn't own the card.
 */
class ReturnSelfTransformedControllerTest : ScenarioTestBase() {

    private fun returningDfc(name: String, underYourControl: Boolean): CardDefinition =
        CardDefinition.doubleFacedPermanent(
            frontFace = card(name) {
                manaCost = "{1}"
                typeLine = "Creature — Spirit"
                power = 1
                toughness = 1
                oracleText = "When this creature dies, return it to the battlefield transformed."
                triggeredAbility {
                    trigger = Triggers.self.dies()
                    effect = Effects.ReturnSelfFromGraveyardTransformed(underYourControl = underYourControl)
                }
            },
            backFace = card("$name Back") {
                manaCost = ""
                typeLine = "Creature — Spirit"
                power = 3
                toughness = 3
                oracleText = ""
            }
        )

    private val slay = card("Slay Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.Destroy(c)
        }
    }

    init {
        cardRegistry.register(
            listOf(
                slay,
                returningDfc("Owner Return Spirit", underYourControl = false),
                returningDfc("Controller Return Spirit", underYourControl = true)
            )
        )

        fun killStolen(name: String): TestGame {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, name) // owned by player 2
                .withCardInHand(1, "Slay Test Spell")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val spirit = game.findPermanent(name)!!
            // Player 1 controls the opponent-owned creature, so the dies trigger is theirs.
            game.state = game.state.updateEntity(spirit) { it.with(ControllerComponent(game.player1Id)) }

            game.castSpell(1, "Slay Test Spell", targetId = spirit).error shouldBe null
            game.resolveStack()
            return game
        }

        test("without an override the back face returns under its owner's control") {
            val game = killStolen("Owner Return Spirit")
            val returned = game.findPermanent("Owner Return Spirit Back").shouldNotBeNull()
            game.state.getEntity(returned)!!.get<CardComponent>()!!.name shouldBe "Owner Return Spirit Back"
            game.state.projectedState.getController(returned) shouldBe game.player2Id
        }

        test("under your control returns the back face under the ability's controller, not the owner") {
            val game = killStolen("Controller Return Spirit")
            val returned = game.findPermanent("Controller Return Spirit Back").shouldNotBeNull()
            game.state.projectedState.getController(returned) shouldBe game.player1Id
            game.isInGraveyard(2, "Controller Return Spirit") shouldBe false
        }
    }
}
