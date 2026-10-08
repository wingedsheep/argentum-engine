package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Battery Bearer (BRO #207) — creatures you control have "{T}: Add {C}. This mana can't be spent
 * to cast a nonartifact spell." Whenever you cast an artifact spell with mana value 6 or greater,
 * draw a card.
 */
class BatteryBearerScenarioTest : ScenarioTestBase() {

    init {
        test("another creature you control gains the restricted {C} mana ability") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Battery Bearer")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val granted = game.getLegalActions(1)
                .map { it.action }
                .filterIsInstance<ActivateAbility>()
                .firstOrNull { it.sourceId == bears }
            granted shouldNotBe null

            game.execute(granted!!).error shouldBe null

            val restricted = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.restrictedMana
            restricted.size shouldBe 1
            restricted.single().color shouldBe null
            restricted.single().restriction shouldBe
                ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT))
        }

        test("casting an artifact spell with mana value 6 or greater draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Battery Bearer")
                .withCardInHand(1, "Darksteel Colossus")
                .withLandsOnBattlefield(1, "Island", 11)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Darksteel Colossus").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Darksteel Colossus") shouldBe true
            game.handSize(1) shouldBe 1
        }

        test("a cheap artifact spell draws nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Battery Bearer")
                .withCardInHand(1, "Ornithopter")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Ornithopter").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe true
            game.handSize(1) shouldBe 0
        }
    }
}
