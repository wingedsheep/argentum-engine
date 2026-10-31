package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.CavalierOfFlame
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cavalier of Flame — the enters trigger draws exactly as many cards as were discarded (zero
 * discards, zero draws), the pump hits every creature you control, and the dies trigger counts
 * land cards in your graveyard to hit each opponent and each planeswalker they control.
 */
class CavalierOfFlameScenarioTest : ScenarioTestBase() {

    init {
        test("enters: discarding two cards draws two") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cavalier of Flame")
                .withCardsInHand(1, "Grizzly Bears", 3)
                .withLandsOnBattlefield(1, "Mountain", 5)
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cavalier of Flame").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val bears = game.findCardsInHand(1, "Grizzly Bears")
            game.selectCards(bears.take(2)).error shouldBe null
            game.resolveStack()

            game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
            game.findCardsInHand(1, "Island").size shouldBe 2
            game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 2
        }

        test("enters: discarding zero cards draws nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cavalier of Flame")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withLandsOnBattlefield(1, "Mountain", 5)
                .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cavalier of Flame").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
            game.findCardsInHand(1, "Island").size shouldBe 0
        }

        test("{1}{R}: creatures you control get +1/+0 and gain haste; opponents' don't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cavalier of Flame")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cavalier = game.findPermanent("Cavalier of Flame")!!
            val abilityId = CavalierOfFlame.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, cavalier, abilityId)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val (mine, theirs) = game.findPermanents("Grizzly Bears")
                .partition { projected.getController(it) == game.player1Id }
            projected.getPower(cavalier) shouldBe 7
            projected.getToughness(cavalier) shouldBe 5
            projected.hasKeyword(cavalier, Keyword.HASTE) shouldBe true
            projected.getPower(mine.single()) shouldBe 3
            projected.hasKeyword(mine.single(), Keyword.HASTE) shouldBe true
            projected.getPower(theirs.single()) shouldBe 2
            projected.hasKeyword(theirs.single(), Keyword.HASTE) shouldBe false
        }

        test("dies: deals X damage to each opponent and their planeswalkers, X = land cards in your graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cavalier of Flame")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInGraveyard(1, "Mountain")
                .withCardInGraveyard(1, "Forest")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Island")
                .withCardInGraveyard(2, "Island")
                .withCardOnBattlefield(2, "Jace Beleren")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cavalier = game.findPermanent("Cavalier of Flame")!!
            game.castSpell(1, "Murder", cavalier).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Cavalier of Flame") shouldBe true
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
            val jace = game.findPermanent("Jace Beleren")!!
            game.state.getEntity(jace)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 1
        }
    }
}
