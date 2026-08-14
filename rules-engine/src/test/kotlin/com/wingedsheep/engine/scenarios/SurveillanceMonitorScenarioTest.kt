package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Surveillance Monitor (MKM) — "When this creature enters, you may collect evidence 4. Whenever you
 * collect evidence, create a 1/1 colorless Thopter artifact creature token with flying."
 *
 * The payoff shape. Its own enters trigger feeds its own payoff, which is the clearest proof that
 * the collect-evidence *event* is emitted from one shared payment: the ETB collection is an effect,
 * the payoff watches for any collection, and neither knows about the other.
 */
class SurveillanceMonitorScenarioTest : ScenarioTestBase() {

    private fun thopters(game: TestGame): Int = game.findPermanents("Thopter").size

    init {
        test("collecting evidence on entry triggers its own payoff") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Surveillance Monitor")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Surveillance Monitor").error shouldBe null
            game.resolveStack()

            // "You may collect evidence 4" — 3 + 1 = 4, exactly the threshold.
            game.answerYesNo(true)
            game.resolveStack()
            if (game.state.pendingDecision != null) {
                // The whole graveyard is exactly the threshold, so exile all of it.
                game.selectCards(
                    game.state.getZone(ZoneKey(game.player1Id, Zone.GRAVEYARD)).toList()
                )
            }
            game.resolveStack()

            game.isInExile(1, "Centaur Courser") shouldBe true
            game.isInExile(1, "Lightning Bolt") shouldBe true
            thopters(game) shouldBe 1
        }

        test("declining the collection creates no Thopter") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Surveillance Monitor")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Surveillance Monitor").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Centaur Courser") shouldBe true
            thopters(game) shouldBe 0
        }

        test("CR 701.59b — with too little evidence it is never asked, and makes no Thopter") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Surveillance Monitor")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Surveillance Monitor").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            thopters(game) shouldBe 0
        }
    }
}
