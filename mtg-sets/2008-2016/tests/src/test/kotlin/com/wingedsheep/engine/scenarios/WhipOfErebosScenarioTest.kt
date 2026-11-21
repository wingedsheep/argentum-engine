package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Whip of Erebos: the reanimated creature gains haste (and lifelink from the static), is exiled at
 * the next end step, and is exiled instead of dying if it would leave the battlefield first.
 */
class WhipOfErebosScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Whip of Erebos")!!.activatedAbilities.single().id

        fun reanimate(withDoomBlade: Boolean): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Whip of Erebos")
                .withLandsOnBattlefield(1, "Swamp", if (withDoomBlade) 6 else 4)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            if (withDoomBlade) builder.withCardInHand(1, "Doom Blade")
            val game = builder.build()

            val whip = game.findPermanent("Whip of Erebos")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = whip,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                )
            ).error shouldBe null
            game.resolveStack()
            return game
        }

        test("returned creature has haste and lifelink, and is exiled at the next end step") {
            val game = reanimate(withDoomBlade = false)

            val bears = game.findPermanent("Grizzly Bears")
            bears shouldNotBe null
            val projected = game.state.projectedState
            projected.hasKeyword(bears!!, Keyword.HASTE) shouldBe true
            projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }

        test("if the returned creature would die, it is exiled instead") {
            val game = reanimate(withDoomBlade = true)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Doom Blade", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }
    }
}
