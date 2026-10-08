package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.matchers.shouldBe

/**
 * The Mightstone and Weakstone (BRO #238) — when it enters, choose one: draw two cards; or target
 * creature gets -5/-5 until end of turn. {T}: Add {C}{C}, spendable only on artifact spells.
 */
class TheMightstoneAndWeakstoneScenarioTest : ScenarioTestBase() {

    private fun castAndReachModeChoice(game: TestGame): ChooseOptionDecision {
        game.castSpell(1, "The Mightstone and Weakstone").error shouldBe null
        game.resolveStack()
        return game.getPendingDecision() as? ChooseOptionDecision
            ?: error("expected the ETB mode choice; got ${game.getPendingDecision()}")
    }

    init {
        test("the draw mode draws two cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "The Mightstone and Weakstone")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mode = castAndReachModeChoice(game)
            game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 0))
            game.resolveStack()

            game.handSize(1) shouldBe 2
        }

        test("the -5/-5 mode kills the targeted creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "The Mightstone and Weakstone")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val mode = castAndReachModeChoice(game)
            game.submitDecision(OptionChosenResponse(mode.id, optionIndex = 1))
            val targets = game.getPendingDecision() as? ChooseTargetsDecision
                ?: error("expected a target choice for the -5/-5 mode; got ${game.getPendingDecision()}")
            game.submitDecision(TargetsResponse(targets.id, mapOf(0 to listOf(giant))))
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.handSize(1) shouldBe 0
        }

        test("taps for two colorless mana restricted to artifact spells") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "The Mightstone and Weakstone")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val stone = game.findPermanent("The Mightstone and Weakstone")!!
            val abilityId = cardRegistry.getCard("The Mightstone and Weakstone")!!.script.activatedAbilities[0].id
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = stone, abilityId = abilityId))
                .error shouldBe null

            val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
            pool.colorless shouldBe 0
            pool.restrictedMana.size shouldBe 2
            pool.restrictedMana.all {
                it.color == null && it.restriction == ManaRestriction.CannotCastSpellsOtherThan(setOf(CardType.ARTIFACT))
            } shouldBe true
        }
    }
}
