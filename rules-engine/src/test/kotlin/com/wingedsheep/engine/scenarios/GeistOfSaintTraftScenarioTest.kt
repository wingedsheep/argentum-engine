package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GeistOfSaintTraftScenarioTest : ScenarioTestBase() {
    init {
        test("has hexproof") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Geist of Saint Traft")
                .build()

            val geist = game.findPermanent("Geist of Saint Traft")!!
            game.state.projectedState.hasKeyword(geist, Keyword.HEXPROOF) shouldBe true
        }

        test("attacking creates a 4/4 flying Angel that is tapped and attacking") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Geist of Saint Traft", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Geist of Saint Traft" to 2)).error shouldBe null
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            val angel = game.findPermanent("Angel Token")
            angel shouldNotBe null
            withClue("the token is a 4/4 flier") {
                game.state.projectedState.getPower(angel!!) shouldBe 4
                game.state.projectedState.getToughness(angel) shouldBe 4
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
            }
            withClue("it enters tapped and attacking") {
                game.state.getEntity(angel!!)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(angel)!!.has<AttackingComponent>() shouldBe true
            }
        }

        test("the Angel token is exiled at end of combat") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Geist of Saint Traft", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Geist of Saint Traft" to 2)).error shouldBe null
            if (game.state.stack.isNotEmpty()) game.resolveStack()
            game.findPermanent("Angel Token") shouldNotBe null

            // Walk real game flow into end of combat so the delayed trigger fires.
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("the delayed trigger exiles the token; Geist survives") {
                game.findPermanent("Angel Token") shouldBe null
                game.state.getZone(game.player1Id, Zone.BATTLEFIELD).size shouldBe 1
                game.findPermanent("Geist of Saint Traft") shouldNotBe null
            }
        }
    }
}
