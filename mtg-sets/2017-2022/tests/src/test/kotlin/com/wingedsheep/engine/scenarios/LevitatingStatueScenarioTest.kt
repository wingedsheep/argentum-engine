package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Levitating Statue (BRO #236) — Flying. Whenever you cast a noncreature spell, put a +1/+1
 * counter on this artifact. {2}: This artifact becomes a 1/1 Construct artifact creature until
 * end of turn.
 */
class LevitatingStatueScenarioTest : ScenarioTestBase() {

    init {
        test("a noncreature spell adds a counter, and animating it makes a 2/2 flying Construct") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Levitating Statue")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val statue = game.findPermanent("Levitating Statue")!!
            game.state.projectedState.isCreature(statue) shouldBe false

            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()

            game.state.getEntity(statue)!!.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

            val abilityId = cardRegistry.getCard("Levitating Statue")!!.script.activatedAbilities[0].id
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = statue, abilityId = abilityId))
                .error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(statue) shouldBe true
            projected.hasType(statue, "ARTIFACT") shouldBe true
            projected.hasSubtype(statue, "Construct") shouldBe true
            projected.hasKeyword(statue, Keyword.FLYING) shouldBe true
            projected.getPower(statue) shouldBe 2
            projected.getToughness(statue) shouldBe 2
        }
    }
}
