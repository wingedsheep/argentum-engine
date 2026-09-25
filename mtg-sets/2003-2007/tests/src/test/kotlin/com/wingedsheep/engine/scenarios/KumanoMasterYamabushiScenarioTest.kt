package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.KumanoMasterYamabushi
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Kumano, Master Yamabushi (CHK #176) — "{1}{R}: Kumano deals 1 damage to any target. If a creature
 * dealt damage by Kumano this turn would die, exile it instead."
 */
class KumanoMasterYamabushiScenarioTest : ScenarioTestBase() {

    private val pingAbility = KumanoMasterYamabushi.activatedAbilities.single().id

    init {
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Kumano, Master Yamabushi")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.ping(target: ChosenTarget) {
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = findPermanent("Kumano, Master Yamabushi")!!,
                    abilityId = pingAbility,
                    targets = listOf(target),
                )
            ).error shouldBe null
            resolveStack()
        }

        context("Kumano, Master Yamabushi") {
            test("a creature Kumano's ping kills is exiled") {
                val game = base().withCardOnBattlefield(2, "Savannah Lions").build()

                game.ping(ChosenTarget.Permanent(game.findPermanent("Savannah Lions")!!))

                game.isInExile(2, "Savannah Lions") shouldBe true
                game.isInGraveyard(2, "Savannah Lions") shouldBe false
            }

            test("the ping can hit a player") {
                val game = base().build()

                game.ping(ChosenTarget.Player(game.player2Id))

                game.getLifeTotal(2) shouldBe 19
            }
        }
    }
}
