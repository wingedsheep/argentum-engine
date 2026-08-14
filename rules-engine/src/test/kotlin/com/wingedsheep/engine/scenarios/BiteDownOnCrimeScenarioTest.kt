package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Bite Down on Crime (MKM) — "As an additional cost to cast this spell, you may collect evidence 6.
 * This spell costs {2} less to cast if evidence was collected. Target creature you control gets
 * +2/+0 until end of turn. It deals damage equal to its power to target creature you don't control."
 *
 * The cost-gate shape of the linkage: the reduction has to be known while the cost is calculated,
 * even though the collection is a choice made during casting. It works because the enumerator prices
 * each *cast branch* separately, so these tests pin the prices of both branches and the resolution
 * order that makes the +2/+0 count toward the damage.
 */
class BiteDownOnCrimeScenarioTest : ScenarioTestBase() {

    init {
        test("the collect-evidence branch costs {1}{G} and the plain branch {3}{G}") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bite Down on Crime")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val branches = game.getLegalActions(1)
                .filter { it.description.contains("Bite Down on Crime") }

            branches.first { it.description.contains("Collect evidence 6") }
                .manaCostString shouldBe "{1}{G}"
            branches.first { !it.description.contains("Collect evidence") }
                .manaCostString shouldBe "{3}{G}"
        }

        test("collecting evidence lets it be cast off two lands and the pumped power deals the damage") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bite Down on Crime")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()

            game.castSpellCollectingEvidenceWithTargets(
                1, "Bite Down on Crime",
                evidenceNames = listOf("Centaur Courser", "Centaur Courser"),
                targetIds = listOf(bears, giant),
            ).error shouldBe null
            game.resolveStack()

            // Grizzly Bears is 2/2; +2/+0 makes it 4 power, which kills the 3/3 Hill Giant.
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInExile(1, "Centaur Courser") shouldBe true
        }

        test("without collecting evidence the spell is unaffordable at two lands") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Bite Down on Crime")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInGraveyard(1, "Centaur Courser")
                .withCardInGraveyard(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.getLegalActions(1)
                .filter { it.description.contains("Bite Down on Crime") }
                .filterNot { it.description.contains("Collect evidence") }
                .none { it.isAffordable } shouldBe true
        }
    }
}
