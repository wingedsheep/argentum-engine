package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Warfire Javelineer (AKH #155) — "When this creature enters, it deals X damage to target creature
 * an opponent controls, where X is the number of instant and sorcery cards in your graveyard."
 */
class WarfireJavelineerScenarioTest : ScenarioTestBase() {

    init {
        test("deals damage equal to the instant and sorcery cards in your own graveyard only") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Warfire Javelineer")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInGraveyard(1, "Shock")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Giant Growth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Warfire Javelineer").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
            withClue("only creatures an opponent controls are legal targets") {
                decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(giant)
            }
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("X = 2: Shock and Lightning Bolt, not the Bears or the opponent's Giant Growth") {
                game.state.getEntity(giant)?.get<DamageComponent>()?.amount shouldBe 2
            }
            game.findPermanent("Hill Giant") shouldBe giant
        }

        test("with no instants or sorceries in your graveyard it deals no damage") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Warfire Javelineer")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInGraveyard(2, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Warfire Javelineer").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            (game.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }
    }
}
