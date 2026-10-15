package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Body Dropper (SNC #168) — {B}{R} Creature — Devil Warrior 2/2
 *
 *   Whenever you sacrifice another creature, put a +1/+1 counter on this creature.
 *   {B}{R}, Sacrifice another creature: This creature gains menace until end of turn.
 *
 * Paying its own activation cost feeds its own trigger: one sacrifice both grows it and
 * grants menace.
 */
class BodyDropperScenarioTest : ScenarioTestBase() {

    init {
        context("Body Dropper") {

            test("sacrificing another creature to its ability grows it and grants menace") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Body Dropper")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dropper = game.findPermanent("Body Dropper")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val ability = cardRegistry.getCard("Body Dropper")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = dropper,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears))
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                val projected = game.state.projectedState
                withClue("the sacrifice trigger put a +1/+1 counter on it") {
                    projected.getPower(dropper) shouldBe 3
                    projected.getToughness(dropper) shouldBe 3
                }
                withClue("the ability granted menace") {
                    projected.hasKeyword(dropper, Keyword.MENACE) shouldBe true
                }
            }
        }
    }
}
