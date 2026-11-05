package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dowsing Dagger // Lost Vale (XLN #235) — "Whenever equipped creature deals combat damage to a
 * player, you may transform this Equipment."
 *
 * Pins what the script alone can't show: an attached Equipment that transforms into a land comes
 * off its creature (taking the +2/+1 with it) and stays untapped, ready to tap for mana (ruling);
 * declining the "may" leaves the Dagger equipped.
 */
class DowsingDaggerScenarioTest : ScenarioTestBase() {
    init {
        fun connect(): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Dowsing Dagger", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 16 // 2/2 Bears + 2/+1
            game.hasPendingDecision() shouldBe true // the "you may transform" prompt
            return game
        }

        test("transforming into Lost Vale unattaches it and leaves an untapped land") {
            val game = connect()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.findPermanent("Dowsing Dagger") shouldBe null
            val vale = game.findPermanent("Lost Vale").shouldNotBeNull()
            game.state.getEntity(vale)!!.get<AttachedToComponent>() shouldBe null
            game.state.projectedState.isCreature(vale) shouldBe false
            game.state.projectedState.hasType(vale, "LAND") shouldBe true
            game.state.getEntity(vale)!!.has<TappedComponent>() shouldBe false

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 2
        }

        test("declining keeps the Dagger equipped") {
            val game = connect()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            val dagger = game.findPermanent("Dowsing Dagger").shouldNotBeNull()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(dagger)!!.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.getPower(bears) shouldBe 4
        }
    }
}
