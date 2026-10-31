package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vivien, Champion of the Wilds (WAR #180, {2}{G}, Loyalty 4).
 *
 *   You may cast creature spells as though they had flash.
 *   +1: Until your next turn, up to one target creature gains reach and vigilance.
 *   −2: Look at the top three cards of your library. Exile one face down and put the rest on the
 *       bottom of your library in any order. For as long as it remains exiled, you may cast it if
 *       it's a creature spell.
 */
class VivienChampionOfTheWildsScenarioTest : ScenarioTestBase() {

    private val vivien = "Vivien, Champion of the Wilds"

    private fun TestGame.activate(index: Int, targets: List<ChosenTarget> = emptyList()) {
        val source = findPermanent(vivien)!!
        val ability = cardRegistry.getCard(vivien)!!.script.activatedAbilities[index]
        execute(ActivateAbility(playerId = player1Id, sourceId = source, abilityId = ability.id, targets = targets))
            .error shouldBe null
    }

    private fun TestGame.libraryCard(name: String): EntityId =
        state.getLibrary(player1Id).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.castOffered(cardId: EntityId): Boolean =
        getLegalActions(1).any { (it.action as? CastSpell)?.cardId == cardId }

    /** Activate the −2 over a three-card library and exile [pick] from it. */
    private fun TestGame.minusTwoExiling(pick: String): EntityId {
        val card = libraryCard(pick)
        activate(index = 1)
        resolveStack()
        selectCards(listOf(card)).error shouldBe null
        if (hasPendingDecision()) keepLibraryOrder()
        return card
    }

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, vivien)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Lightning Bolt")
            .withCardInLibrary(1, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("−2 exiles a creature card face down that stays castable from exile") {
            val game = board()
            val bears = game.minusTwoExiling("Grizzly Bears")

            withClue("the chosen card is in exile, the other two on the bottom") {
                game.state.getExile(game.player1Id) shouldBe listOf(bears)
                game.state.getLibrary(game.player1Id).size shouldBe 2
            }
            withClue("Vivien went from 4 to 2 loyalty") {
                game.state.getEntity(game.findPermanent(vivien)!!)!!.get<CountersComponent>()!!
                    .getCount(CounterType.LOYALTY) shouldBe 2
            }
            game.castOffered(bears) shouldBe true
            game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
        }

        test("−2 must exile a card even when it isn't a creature, and that card can't be cast") {
            val game = board()
            val bolt = game.minusTwoExiling("Lightning Bolt")

            game.state.getExile(game.player1Id) shouldBe listOf(bolt)
            game.castOffered(bolt) shouldBe false
            game.execute(CastSpell(game.player1Id, bolt)).error shouldNotBe null
        }

        test("creature spells can be cast as though they had flash") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, vivien)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Rampant Growth")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val actions = game.getLegalActions(1).filter { it.actionType == "CastSpell" }
            actions.any { it.description.contains("Grizzly Bears") } shouldBe true
            actions.any { it.description.contains("Rampant Growth") } shouldBe false
        }

        test("+1 gives up to one target creature reach and vigilance") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, vivien)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.activate(index = 0, targets = listOf(ChosenTarget.Permanent(bears)))
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.REACH) shouldBe true
            game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        }
    }
}
