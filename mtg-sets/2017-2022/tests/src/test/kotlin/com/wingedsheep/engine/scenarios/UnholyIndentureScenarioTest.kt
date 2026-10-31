package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.shouldBe

/**
 * Unholy Indenture (M20 #119) — {2}{B} Aura
 *   Enchant creature
 *   When enchanted creature dies, return that card to the battlefield under your control with a
 *   +1/+1 counter on it.
 */
class UnholyIndentureScenarioTest : ScenarioTestBase() {

    private val slay = card("Slay Test Spell") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.Destroy(c)
        }
    }

    init {
        cardRegistry.register(listOf(slay))

        test("an opponent's enchanted creature returns under the Aura controller's control with a +1/+1 counter") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardAttachedTo(1, "Unholy Indenture", "Grizzly Bears")
                .withCardInHand(1, "Slay Test Spell")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Slay Test Spell", targetId = bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            val returned = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getController(returned) shouldBe game.player1Id
            game.state.getEntity(returned)!!.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(returned) shouldBe 3
            game.isInGraveyard(1, "Unholy Indenture") shouldBe true
        }
    }
}
