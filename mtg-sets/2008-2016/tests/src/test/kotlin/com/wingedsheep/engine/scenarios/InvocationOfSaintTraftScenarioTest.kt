package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invocation of Saint Traft (SOI #246) — the Aura grants Geist of Saint Traft's attack trigger to
 * the enchanted creature. Pins the three things the grant could get wrong: the token itself
 * (tapped, attacking, exiled at end of combat), whose token it is when the Aura sits on someone
 * else's creature, and the delayed exile surviving the enchanted creature's death (2016-04-08 ruling).
 */
class InvocationOfSaintTraftScenarioTest : ScenarioTestBase() {
    init {
        test("attacking creates a tapped and attacking 4/4 flying Angel, exiled at end of combat") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Invocation of Saint Traft", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            val angel = game.findPermanent("Angel Token")
            angel shouldNotBe null
            withClue("the token is a 4/4 flier controlled by the attacking player") {
                game.state.projectedState.getPower(angel!!) shouldBe 4
                game.state.projectedState.getToughness(angel) shouldBe 4
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
                game.state.getEntity(angel)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
            }
            withClue("it enters tapped and attacking") {
                game.state.getEntity(angel!!)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(angel)!!.has<AttackingComponent>() shouldBe true
            }

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("the delayed trigger exiles the Angel; the Bears and the Aura stay") {
                game.findPermanent("Angel Token") shouldBe null
                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.findPermanent("Invocation of Saint Traft") shouldNotBe null
            }
        }

        test("on an opponent's creature the Angel belongs to that creature's controller") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Invocation of Saint Traft", "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.resolveStack()

            val angel = game.findPermanent("Angel Token")
            angel shouldNotBe null
            withClue("the granted trigger is controlled by the enchanted creature's controller") {
                game.state.getEntity(angel!!)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
            }
        }

        test("the Angel is still exiled at end of combat after the enchanted creature dies") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "Invocation of Saint Traft", "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()
            game.findPermanent("Angel Token") shouldNotBe null

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            withClue("the Bears die and the Aura goes with them") {
                game.findPermanent("Grizzly Bears") shouldBe null
                game.findPermanent("Invocation of Saint Traft") shouldBe null
            }
            game.findPermanent("Angel Token") shouldNotBe null

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("the delayed trigger does not depend on its source staying on the battlefield") {
                game.findPermanent("Angel Token") shouldBe null
            }
        }
    }
}
