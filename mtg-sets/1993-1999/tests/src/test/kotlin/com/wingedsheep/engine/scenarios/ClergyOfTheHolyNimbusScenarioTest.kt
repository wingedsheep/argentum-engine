package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Clergy of the Holy Nimbus (LEG #6).
 *
 * "If this creature would be destroyed, regenerate it.
 *  {1}: This creature can't be regenerated this turn. Only your opponents may activate this ability."
 *
 * The static regeneration is never spent, and the {1} belongs to the Clergy's opponents only.
 */
class ClergyOfTheHolyNimbusScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.getCard("Clergy of the Holy Nimbus")!!.script.activatedAbilities[0].id
    }

    /** Player 1 controls the Clergy; player 2 is active and holds two Doom Blades and lands. */
    private fun board() = scenario()
        .withPlayers("Cleric", "Opponent")
        .withCardOnBattlefield(1, "Clergy of the Holy Nimbus")
        .withCardInHand(2, "Doom Blade")
        .withCardInHand(2, "Doom Blade")
        .withLandsOnBattlefield(2, "Swamp", 5)
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Clergy of the Holy Nimbus") {

            test("regenerates every time it would be destroyed") {
                val game = board()
                val clergy = game.findPermanent("Clergy of the Holy Nimbus")!!

                repeat(2) { round ->
                    game.castSpell(2, "Doom Blade", clergy).error shouldBe null
                    game.resolveStack()
                    withClue("destruction #${round + 1} is regenerated") {
                        game.isOnBattlefield("Clergy of the Holy Nimbus") shouldBe true
                        game.state.getEntity(clergy)!!.has<TappedComponent>() shouldBe true
                    }
                }
            }

            test("an opponent pays {1} so it can't be regenerated, and then it dies") {
                val game = board()
                val clergy = game.findPermanent("Clergy of the Holy Nimbus")!!

                val activation = game.getLegalActions(2).map { it.action }.firstOrNull {
                    it is ActivateAbility && it.sourceId == clergy && it.abilityId == abilityId
                }
                withClue("the opponent is offered the {1} ability") { activation shouldNotBe null }
                game.execute(activation!!).error shouldBe null
                game.resolveStack()

                game.castSpell(2, "Doom Blade", clergy).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Clergy of the Holy Nimbus") shouldBe true
            }

            test("its controller can't activate the ability") {
                val game = scenario()
                    .withPlayers("Cleric", "Opponent")
                    .withCardOnBattlefield(1, "Clergy of the Holy Nimbus")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val clergy = game.findPermanent("Clergy of the Holy Nimbus")!!

                game.getLegalActions(1).map { it.action }.any {
                    it is ActivateAbility && it.sourceId == clergy
                } shouldBe false
                game.execute(ActivateAbility(game.player1Id, clergy, abilityId)).error shouldNotBe null
            }
        }
    }
}
