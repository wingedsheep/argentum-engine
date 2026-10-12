package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.EmblemOwnedActivatedAbilityComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KarnLivingLegacyScenarioTest : ScenarioTestBase() {

    private val karn get() = cardRegistry.getCard("Karn, Living Legacy")!!

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { container ->
            container
                .with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
                .without<AbilityActivatedThisTurnComponent>()
        }
    }

    /** Ultimate Karn from exactly 7 loyalty; he dies to the state-based action, the emblem stays. */
    private fun ultimate(game: TestGame): EntityId {
        val karnId = game.findPermanent("Karn, Living Legacy")!!
        seedLoyalty(game, karnId, 7)
        game.execute(ActivateAbility(game.player1Id, karnId, karn.script.activatedAbilities[2].id))
            .error shouldBe null
        game.resolveStack()
        game.findPermanent("Karn, Living Legacy") shouldBe null
        return game.state.entities.entries
            .single { it.value.has<EmblemOwnedActivatedAbilityComponent>() }
            .key
    }

    private fun emblemActions(game: TestGame, playerNumber: Int, emblem: EntityId) =
        game.getLegalActions(playerNumber).filter { (it.action as? ActivateAbility)?.sourceId == emblem }

    init {
        test("+1 creates a tapped Powerstone token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Karn, Living Legacy")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karnId = game.findPermanent("Karn, Living Legacy")!!
            seedLoyalty(game, karnId, 4)

            game.execute(ActivateAbility(game.player1Id, karnId, karn.script.activatedAbilities[0].id))
                .error shouldBe null
            game.resolveStack()

            val powerstone = game.findPermanent("Powerstone").shouldNotBeNull()
            game.state.getEntity(powerstone)!!.has<TappedComponent>() shouldBe true
        }

        test("-1 pays X, looks at X cards, keeps one and bottoms the rest") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Karn, Living Legacy")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Raging Goblin")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val karnId = game.findPermanent("Karn, Living Legacy")!!
            seedLoyalty(game, karnId, 4)
            val library = game.state.getZone(game.player1Id, Zone.LIBRARY)
            val topTwo = library.take(2)
            val handBefore = game.handSize(1)

            game.execute(ActivateAbility(game.player1Id, karnId, karn.script.activatedAbilities[1].id))
                .error shouldBe null
            game.resolveStack()

            val payX = game.getPendingDecision()
            withClue("the -1 asks how much mana to pay") { (payX is ChooseNumberDecision) shouldBe true }
            game.chooseNumber(2).error shouldBe null

            game.selectCards(listOf(topTwo[1])).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
            (topTwo[1] in game.state.getZone(game.player1Id, Zone.HAND)) shouldBe true
            withClue("the unchosen card goes to the bottom") {
                game.state.getZone(game.player1Id, Zone.LIBRARY).last() shouldBe topTwo[0]
            }
            game.state.getEntity(game.findPermanents("Island")[0])!!.has<TappedComponent>() shouldBe true
        }

        test("-7 emblem's own ability: the emblem is the source and outlives Karn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Karn, Living Legacy")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val emblem = ultimate(game)
            val ornithopter = game.findPermanent("Ornithopter")!!

            val offered = emblemActions(game, 1, emblem)
            withClue("the emblem offers its ability with itself as the source") { offered shouldHaveSize 1 }
            withClue("an opponent can't activate an emblem they don't control") {
                emblemActions(game, 2, emblem).shouldBeEmpty()
            }

            val action = offered.single().action as ActivateAbility
            game.execute(
                action.copy(
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ornithopter))
                )
            ).error shouldBe null

            val onStack = game.state.getEntity(game.state.stack.last())
                ?.get<ActivatedAbilityOnStackComponent>()
                .shouldNotBeNull()
            onStack.sourceId shouldBe emblem
            game.state.getEntity(ornithopter)!!.has<TappedComponent>() shouldBe true

            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19

            withClue("no untapped artifact left: the ability can't be paid") {
                emblemActions(game, 1, emblem).shouldBeEmpty()
            }
        }

        test("tapping a non-artifact for the emblem's cost is rejected") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Karn, Living Legacy")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val emblem = ultimate(game)
            val bears = game.findPermanent("Grizzly Bears")!!
            val action = emblemActions(game, 1, emblem).single().action as ActivateAbility

            val result = game.execute(
                action.copy(
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears))
                )
            )
            result.error shouldNotBe null
            game.getLifeTotal(2) shouldBe 20
        }

        test("the emblem's ability can be activated at instant speed on the opponent's turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Karn, Living Legacy")
                .withCardOnBattlefield(1, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val emblem = ultimate(game)
            val ornithopter = game.findPermanent("Ornithopter")!!

            // Hand the turn to the opponent and priority back to Karn's controller: an emblem's
            // ability has no sorcery-speed restriction of its own.
            game.state = game.state.copy(activePlayerId = game.player2Id, priorityPlayerId = game.player1Id)
            val action = emblemActions(game, 1, emblem).single().action as ActivateAbility
            game.execute(
                action.copy(
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ornithopter))
                )
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19
            game.state.getEntity(emblem).shouldNotBeNull()
            game.findPermanent("Karn, Living Legacy").shouldBeNull()
        }
    }
}
