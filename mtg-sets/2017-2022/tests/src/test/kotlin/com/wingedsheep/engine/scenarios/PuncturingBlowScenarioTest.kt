package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.collections.shouldContain

class PuncturingBlowScenarioTest : ScenarioTestBase() {
    init {
        test("lethal damage exiles the creature instead of putting it in the graveyard") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Puncturing Blow").withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Puncturing Blow", bears).error shouldBe null
            game.resolveStack()
            game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE)) shouldContain bears
            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
        }
        test("prevented damage still marks the creature for exile when it is destroyed later") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Puncturing Blow").withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Mountain", 4).withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Grizzly Bears").withCardOnBattlefield(2, "Blessed Sanctuary")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Puncturing Blow", bears).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            (game.state.getEntity(bears)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            game.castSpell(1, "Murder", bears).error shouldBe null
            game.resolveStack()
            game.state.getZone(ZoneKey(game.player2Id, Zone.EXILE)) shouldContain bears
            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
        }
    }
}
