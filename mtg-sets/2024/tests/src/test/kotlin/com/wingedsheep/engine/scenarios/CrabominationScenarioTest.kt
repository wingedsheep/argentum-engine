package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crabomination — "Emerge from artifact {5}{B}{B}" (CR 702.119b) plus an ETB that makes the target
 * opponent exile their top library card, a random graveyard card and a random hand card, after
 * which you may cast a spell from among them without paying its mana cost.
 */
class CrabominationScenarioTest : ScenarioTestBase() {
    init {
        fun emergeActionFor(game: TestGame) = game.getLegalActions(1).firstOrNull { la ->
            val cast = la.action as? CastSpell
            cast != null && cast.alternativeCostType == AlternativeCostType.EMERGE
        }

        /** Resolve Crabomination, aim the ETB at the opponent, and resolve the trigger. */
        fun resolveIntoEtb(game: TestGame) {
            game.resolveStack()
            if (game.state.pendingDecision is ChooseTargetsDecision) {
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
            }
            game.resolveStack()
        }

        test("emerge sacrifices an artifact and is reduced by its mana value") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Crabomination")
                .withCardOnBattlefield(1, "Jalum Tome") // {3} -> mana value 3
                .withLandsOnBattlefield(1, "Swamp", 4) // {5}{B}{B} - 3 = {2}{B}{B}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val emerge = emergeActionFor(game)
            emerge shouldNotBe null
            emerge!!.additionalCostInfo!!.validSacrificeTargets shouldContain game.findPermanent("Jalum Tome")!!

            game.castSpellWithEmerge(1, "Crabomination", "Jalum Tome").error shouldBe null
            game.isInGraveyard(1, "Jalum Tome") shouldBe true
            resolveIntoEtb(game)
            game.isOnBattlefield("Crabomination") shouldBe true
        }

        test("a non-artifact creature can't be sacrificed for emerge from artifact") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Crabomination")
                .withCardOnBattlefield(1, "Centaur Courser") // creature, mana value 3, not an artifact
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            emergeActionFor(game) shouldBe null
            game.castSpellWithEmerge(1, "Crabomination", "Centaur Courser").error shouldNotBe null
            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.isInHand(1, "Crabomination") shouldBe true
        }

        test("an artifact creature is offered, a plain creature beside it is not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Crabomination")
                .withCardOnBattlefield(1, "Ornithopter") // artifact creature, mana value 0
                .withCardOnBattlefield(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Swamp", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val candidates = emergeActionFor(game)!!.additionalCostInfo!!.validSacrificeTargets
            candidates shouldContain game.findPermanent("Ornithopter")!!
            candidates shouldNotContain game.findPermanent("Centaur Courser")!!
        }

        test("ETB exiles one card from each zone and you may cast a spell among them for free") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Crabomination")
                .withCardOnBattlefield(1, "Jalum Tome")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Lightning Bolt")
                .withCardInHand(2, "Island")
                .withCardInHand(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithEmerge(1, "Crabomination", "Jalum Tome").error shouldBe null
            resolveIntoEtb(game)

            // One card from each zone: the top card, the only graveyard card, one of two hand cards.
            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Lightning Bolt") shouldBe true
            game.isInExile(2, "Island") shouldBe true
            game.state.getHand(game.player2Id).size shouldBe 1
            game.state.getGraveyard(game.player2Id).size shouldBe 0
            game.librarySize(2) shouldBe 0

            val decision = game.state.pendingDecision as SelectCardsDecision
            val bears = game.state.getExile(game.player2Id).single { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val bolt = game.state.getExile(game.player2Id).single { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Lightning Bolt"
            }
            decision.options shouldContain bears
            decision.options shouldContain bolt

            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getController(bears) shouldBe game.player1Id
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInExile(2, "Lightning Bolt") shouldBe true
            game.isInExile(2, "Island") shouldBe true
        }

        test("may decline the free cast — the exiled cards stay in exile") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Crabomination")
                .withCardOnBattlefield(1, "Jalum Tome")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Centaur Courser")
                .withCardInHand(2, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithEmerge(1, "Crabomination", "Jalum Tome").error shouldBe null
            resolveIntoEtb(game)

            game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            // Only the top library card goes — the rest of the library is untouched.
            game.librarySize(2) shouldBe 1
            game.state.getExile(game.player2Id).size shouldBe 2
            game.isInExile(2, "Lightning Bolt") shouldBe true
        }
    }
}
