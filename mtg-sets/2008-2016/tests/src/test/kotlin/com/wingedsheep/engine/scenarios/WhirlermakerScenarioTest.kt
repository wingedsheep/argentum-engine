package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.kld.cards.Whirlermaker
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Whirlermaker (KLD #239) — "{4}, {T}: Create a 1/1 colorless Thopter artifact creature token with flying." */
class WhirlermakerScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = Whirlermaker.activatedAbilities.first().id

        test("{4}, {T} creates a 1/1 colorless flying Thopter artifact creature token") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Whirlermaker")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val maker = game.findPermanent("Whirlermaker")!!
            val before = game.state.getZone(game.player1Id, Zone.BATTLEFIELD).toSet()

            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = maker, abilityId = abilityId)
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("Whirlermaker tapped as part of the cost") {
                game.state.getEntity(maker)?.has<TappedComponent>() shouldBe true
            }

            val created = game.state.getZone(game.player1Id, Zone.BATTLEFIELD).filterNot { it in before }
            withClue("exactly one token was created") { created.size shouldBe 1 }
            val token = created.single()
            val projected = game.state.projectedState
            projected.isCreature(token) shouldBe true
            projected.hasType(token, "ARTIFACT") shouldBe true
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.hasKeyword(token, Keyword.FLYING) shouldBe true
            projected.getColors(token).isEmpty() shouldBe true
        }

        test("can't be activated without {4}") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Whirlermaker")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val maker = game.findPermanent("Whirlermaker")!!
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = maker, abilityId = abilityId)
            )
            result.error shouldNotBe null
        }
    }
}
