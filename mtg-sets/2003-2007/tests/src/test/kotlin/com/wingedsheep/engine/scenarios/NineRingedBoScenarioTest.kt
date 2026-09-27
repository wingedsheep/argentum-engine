package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.NineRingedBo
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nine-Ringed Bo (CHK #263) — "{T}: This artifact deals 1 damage to target Spirit creature. If that
 * creature would die this turn, exile it instead."
 */
class NineRingedBoScenarioTest : ScenarioTestBase() {

    private val pingAbility = NineRingedBo.activatedAbilities.single().id

    init {
        fun base() = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Nine-Ringed Bo")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.activate(targetName: String) = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Nine-Ringed Bo")!!,
                abilityId = pingAbility,
                targets = listOf(ChosenTarget.Permanent(findPermanent(targetName)!!)),
            )
        )

        context("Nine-Ringed Bo") {
            test("a Spirit killed by the ping is exiled instead of going to the graveyard") {
                val game = base().withCardOnBattlefield(2, "Hana Kami").build()

                game.activate("Hana Kami").error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Hana Kami") shouldBe true
                game.isInGraveyard(2, "Hana Kami") shouldBe false
            }

            test("a larger Spirit survives with 1 damage") {
                val game = base().withCardOnBattlefield(2, "Kami of the Hunt").build()

                game.activate("Kami of the Hunt").error shouldBe null
                game.resolveStack()

                game.findPermanent("Kami of the Hunt") shouldNotBe null
            }

            test("a non-Spirit creature can't be targeted") {
                val game = base().withCardOnBattlefield(2, "Savannah Lions").build()

                game.activate("Savannah Lions").error shouldNotBe null
            }
        }
    }
}
