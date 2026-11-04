package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.xln.cards.FireShrineKeeper
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FireShrineKeeperScenarioTest : ScenarioTestBase() {
    init {
        fun setup() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Fire Shrine Keeper")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withLandsOnBattlefield(1, "Mountain", 8)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        for (targetCount in 0..2) {
            test("sacrifices as a cost and damages each of $targetCount chosen creatures") {
                val game = setup()
                val names = listOf("Grizzly Bears", "Hill Giant").take(targetCount)
                val targets = names.map { ChosenTarget.Permanent(game.findPermanent(it)!!) }
                game.execute(ActivateAbility(
                    game.player1Id, game.findPermanent("Fire Shrine Keeper")!!,
                    FireShrineKeeper.activatedAbilities.single().id, targets = targets
                )).error shouldBe null
                game.isInGraveyard(1, "Fire Shrine Keeper") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                game.resolveStack()
                for (name in listOf("Grizzly Bears", "Hill Giant")) {
                    game.isInGraveyard(2, name) shouldBe (name in names)
                }
            }
        }

        test("the same creature cannot be chosen twice") {
            val game = setup()
            val target = ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)
            game.execute(ActivateAbility(
                game.player1Id, game.findPermanent("Fire Shrine Keeper")!!,
                FireShrineKeeper.activatedAbilities.single().id, targets = listOf(target, target)
            )).error shouldNotBe null
            game.isInGraveyard(1, "Fire Shrine Keeper") shouldBe false
        }
    }
}
