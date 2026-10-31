package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sorcerer of the Fang (M20 #114) — {1}{B} 1/3 Human Wizard Sorcerer.
 *
 * "{5}{B}, {T}: This creature deals 2 damage to target opponent or planeswalker."
 *
 * Covers the damage to an opponent and that you can't target yourself.
 */
class SorcererOfTheFangScenarioTest : ScenarioTestBase() {

    init {
        context("Sorcerer of the Fang") {

            test("{5}{B}, {T}: deals 2 damage to target opponent") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sorcerer of the Fang")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Sorcerer of the Fang")!!
                val ability = cardRegistry.getCard("Sorcerer of the Fang")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sorcerer,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 20
            }

            test("cannot target its controller") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sorcerer of the Fang")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Sorcerer of the Fang")!!
                val ability = cardRegistry.getCard("Sorcerer of the Fang")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sorcerer,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game.player1Id)),
                    )
                )
                result.error shouldNotBe null
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
