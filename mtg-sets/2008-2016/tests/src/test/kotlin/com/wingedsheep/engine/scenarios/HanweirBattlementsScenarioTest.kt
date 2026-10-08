package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Hanweir Battlements (EMN) — "{3}{R}{R}, {T}: If you both own and control this land and a
 * creature named Hanweir Garrison, exile them, then meld them into Hanweir, the Writhing
 * Township." (CR 701.42)
 */
class HanweirBattlementsScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.activateMeld() {
            val battlements = findPermanent("Hanweir Battlements")!!
            val abilityId = cardRegistry.getCard("Hanweir Battlements")!!.script.activatedAbilities[2].id
            execute(ActivateAbility(playerId = player1Id, sourceId = battlements, abilityId = abilityId)).error shouldBe null
            resolveStack()
        }

        test("with Hanweir Garrison, the ability melds the pair into Hanweir, the Writhing Township") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hanweir Battlements")
                .withCardOnBattlefield(1, "Hanweir Garrison")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val battlements = game.findPermanent("Hanweir Battlements")!!

            game.activateMeld()

            val township = game.findPermanent("Hanweir, the Writhing Township").shouldNotBeNull()
            withClue("the Battlements' entity is the melded permanent") { township shouldBe battlements }
            game.findPermanent("Hanweir Battlements") shouldBe null
            game.findPermanent("Hanweir Garrison") shouldBe null
            game.isInExile(1, "Hanweir Battlements") shouldBe false
            game.isInExile(1, "Hanweir Garrison") shouldBe false
            game.state.getEntity(township)!!.has<MeldedComponent>() shouldBe true
            game.state.projectedState.isCreature(township) shouldBe true
            game.state.projectedState.getPower(township) shouldBe 7
            game.state.projectedState.getToughness(township) shouldBe 4
        }

        test("without Hanweir Garrison, the ability resolves and does nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hanweir Battlements")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activateMeld()

            game.findPermanent("Hanweir Battlements").shouldNotBeNull()
            game.findPermanent("Hanweir, the Writhing Township") shouldBe null
            game.isInExile(1, "Hanweir Battlements") shouldBe false
        }
    }
}
