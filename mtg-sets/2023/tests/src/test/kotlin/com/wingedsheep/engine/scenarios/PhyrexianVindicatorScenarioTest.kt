package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Phyrexian Vindicator — damage to it is prevented, and a reflexive trigger deals that much
 * damage to any other target.
 */
class PhyrexianVindicatorScenarioTest : ScenarioTestBase() {

    init {
        test("prevented burn is redirected to the opponent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Phyrexian Vindicator")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vindicator = game.findPermanent("Phyrexian Vindicator")!!
            game.castSpell(1, "Lightning Bolt", vindicator).error shouldBe null
            game.resolveStack()

            val sel = game.selectTargets(listOf(game.player2Id))
            withClue("select: ${sel.error}") { sel.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.findPermanent("Phyrexian Vindicator") shouldNotBe null
            (game.state.getEntity(vindicator)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }

        test("prevented damage can kill another creature but cannot target the Vindicator itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Phyrexian Vindicator")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vindicator = game.findPermanent("Phyrexian Vindicator")!!
            game.castSpell(1, "Lightning Bolt", vindicator).error shouldBe null
            game.resolveStack()

            game.selectTargets(listOf(vindicator)).error shouldNotBe null

            val giant = game.findPermanent("Hill Giant")!!
            val sel = game.selectTargets(listOf(giant))
            withClue("select: ${sel.error}") { sel.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(2) shouldBe 20
            game.findPermanent("Phyrexian Vindicator") shouldNotBe null
        }
    }
}
