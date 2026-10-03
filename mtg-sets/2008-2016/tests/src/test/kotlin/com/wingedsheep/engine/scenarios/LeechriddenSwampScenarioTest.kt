package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Leechridden Swamp (SHM #273) — Land — Swamp. "This land enters tapped. {B}, {T}: Each opponent
 * loses 1 life. Activate only if you control two or more black permanents."
 */
class LeechriddenSwampScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Leechridden Swamp")!!.activatedAbilities.first().id

        test("with two black permanents, {B}, {T} drains each opponent for 1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Leechridden Swamp")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Walking Corpse")
                .withCardOnBattlefield(1, "Vampire Bats")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val swamp = game.findPermanent("Leechridden Swamp")!!
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = swamp, abilityId = abilityId)
            ).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 20
        }

        test("with only one black permanent the ability can't be activated") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Leechridden Swamp")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Walking Corpse")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val swamp = game.findPermanent("Leechridden Swamp")!!
            withClue("lands are colourless, so the Swamps don't count as black permanents") {
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = swamp, abilityId = abilityId)
                ).error shouldNotBe null
            }
            game.getLifeTotal(2) shouldBe 20
        }

        test("it enters tapped and taps for {B} as a Swamp") {
            val card = cardRegistry.getCard("Leechridden Swamp")!!
            card.typeLine.subtypes.map { it.value } shouldBe listOf("Swamp")
            card.script.replacementEffects.size shouldBe 1
        }
    }
}
