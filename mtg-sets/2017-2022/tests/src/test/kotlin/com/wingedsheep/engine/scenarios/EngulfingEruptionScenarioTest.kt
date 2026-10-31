package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Engulfing Eruption (M20 #328) — {2}{R}{R} Sorcery: "Engulfing Eruption deals 5 damage to
 * target creature."
 */
class EngulfingEruptionScenarioTest : ScenarioTestBase() {

    init {
        test("deals exactly 5 damage to the target creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Engulfing Eruption")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Enormous Baloth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val baloth = game.findPermanent("Enormous Baloth")!!
            game.castSpell(1, "Engulfing Eruption", baloth).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Enormous Baloth") shouldBe true
            game.state.getEntity(baloth)?.get<DamageComponent>()?.amount shouldBe 5
            game.isInGraveyard(1, "Engulfing Eruption") shouldBe true
        }

        test("kills a creature with 4 toughness") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Engulfing Eruption")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Engulfing Eruption", wurm).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Craw Wurm") shouldBe false
            game.isInGraveyard(2, "Craw Wurm") shouldBe true
        }
    }
}
