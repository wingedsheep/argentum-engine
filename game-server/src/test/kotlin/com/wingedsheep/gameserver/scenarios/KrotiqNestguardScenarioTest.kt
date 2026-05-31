package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Krotiq Nestguard (TDM) — {2}{G} Insect, 4/4 with Defender.
 *
 * "{2}{G}: This creature can attack this turn as though it didn't have defender."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.effects.CanAttackThisTurnEffect]: a creature
 * with Defender can't normally attack, but after activating the ability it gains a transient
 * marker letting it attack this turn. The marker is removed at end of turn.
 */
class KrotiqNestguardScenarioTest : ScenarioTestBase() {

    private val krotiqAbilityId =
        cardRegistry.getCard("Krotiq Nestguard")!!.activatedAbilities.first().id

    init {
        context("Krotiq Nestguard can-attack-despite-defender ability") {

            test("cannot attack with defender before activating the ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krotiq Nestguard", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val result = game.declareAttackers(mapOf("Krotiq Nestguard" to 2))
                withClue("Krotiq has Defender and cannot attack without the ability") {
                    (result.error != null) shouldBe true
                }
            }

            test("attacks after activating '{2}{G}: can attack as though it didn't have defender'") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Krotiq Nestguard", tapped = false, summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val krotiq = game.findPermanent("Krotiq Nestguard")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = krotiq,
                        abilityId = krotiqAbilityId
                    )
                )
                withClue("Activating the ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                val result = game.declareAttackers(mapOf("Krotiq Nestguard" to 2))
                withClue("After activation Krotiq can attack despite Defender: ${result.error}") {
                    result.error shouldBe null
                }
                withClue("Krotiq is now an attacker") {
                    game.state.getEntity(krotiq)?.get<AttackingComponent>().shouldNotBeNull()
                }
            }
        }
    }
}
