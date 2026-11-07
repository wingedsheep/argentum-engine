package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.GuardianIdol
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Guardian Idol (5DN #128) — {2} Artifact.
 * "This artifact enters tapped. {T}: Add {C}.
 *  {2}: This artifact becomes a 2/2 Golem artifact creature until end of turn."
 */
class GuardianIdolScenarioTest : ScenarioTestBase() {
    init {
        test("it enters tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Guardian Idol")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Guardian Idol").error shouldBe null
            game.resolveStack()

            val idol = game.findPermanent("Guardian Idol")!!
            game.state.getEntity(idol)?.has<TappedComponent>() shouldBe true
        }

        test("{2} turns it into a 2/2 Golem artifact creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Guardian Idol", tapped = true)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val idol = game.findPermanent("Guardian Idol")!!
            game.state.projectedState.isCreature(idol) shouldBe false

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = idol,
                    abilityId = GuardianIdol.activatedAbilities[1].id,
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(idol) shouldBe true
            projected.hasType(idol, "ARTIFACT") shouldBe true
            projected.hasSubtype(idol, "Golem") shouldBe true
            projected.getPower(idol) shouldBe 2
            projected.getToughness(idol) shouldBe 2
        }
    }
}
