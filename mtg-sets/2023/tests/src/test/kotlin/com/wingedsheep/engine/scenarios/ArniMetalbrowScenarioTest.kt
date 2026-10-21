package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Arni Metalbrow — "Whenever a creature you control attacks or enters attacking, you may pay
 * {1}{R}. If you do, you may put a creature card with mana value less than that creature's mana
 * value from your hand onto the battlefield tapped and attacking."
 */
class ArniMetalbrowScenarioTest : ScenarioTestBase() {

    private fun TestGame.handId(name: String) =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        test("an attacker lets you put a cheaper creature in attacking, and that one re-triggers Arni") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Arni Metalbrow")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Raging Goblin")
                .withCardInHand(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val bears = g.handId("Grizzly Bears")
            val goblin = g.handId("Raging Goblin")

            g.declareAttackers(mapOf("Arni Metalbrow" to 2)).error shouldBe null
            g.resolveStack()
            g.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            g.answerYesNo(true)
            g.submitManaSourcesAutoPay()

            val pick = g.state.pendingDecision
            pick.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only creature cards with mana value less than Arni's 3 are offered") {
                pick.options shouldContainExactlyInAnyOrder listOf(bears, goblin)
            }
            g.selectCards(listOf(bears)).error shouldBe null

            withClue("the Bears enter tapped and attacking") {
                g.state.getEntity(bears)!!.has<AttackingComponent>() shouldBe true
                g.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }

            // The Bears entered attacking: Arni triggers again, now capped below the Bears' 2.
            g.resolveStack()
            g.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            g.answerYesNo(true)
            g.submitManaSourcesAutoPay()
            val second = g.state.pendingDecision
            second.shouldBeInstanceOf<SelectCardsDecision>()
            second.options shouldBe listOf(goblin)
            g.selectCards(listOf(goblin)).error shouldBe null
            g.state.getEntity(goblin)!!.has<AttackingComponent>() shouldBe true

            // The 1-drop entering attacking triggers once more; nothing has mana value below 1.
            g.resolveStack()
            g.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            g.answerYesNo(false)
            g.resolveStack()
            g.state.pendingDecision shouldBe null
        }

        test("declining to pay puts nothing onto the battlefield") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Arni Metalbrow")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            g.declareAttackers(mapOf("Arni Metalbrow" to 2)).error shouldBe null
            g.resolveStack()
            g.answerYesNo(false)
            g.resolveStack()

            g.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("a creature entering the battlefield without attacking does not trigger Arni") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Arni Metalbrow")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            g.castSpell(1, "Grizzly Bears").error shouldBe null
            g.resolveStack()

            g.findPermanent("Grizzly Bears").shouldNotBeNull()
            withClue("no 'you may pay {1}{R}' prompt and nothing left on the stack") {
                g.state.pendingDecision shouldBe null
                g.state.stack shouldBe emptyList()
            }
        }
    }
}
