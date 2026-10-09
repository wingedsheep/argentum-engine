package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class UrzaPlaneswalkerScenarioTest : ScenarioTestBase() {
    private val name = "Urza, Planeswalker"
    private fun board() = scenario().withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, name).withCardOnBattlefield(1, "Ornithopter")
        .withCardOnBattlefield(1, "Grizzly Bears").withCardOnBattlefield(1, "Island")
        .withCardOnBattlefield(1, "Saheeli, Filigree Master")
        .withCardOnBattlefield(2, "Ornithopter").withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Forest")
        .withCardInLibrary(1, "Island").withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Island")
        .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
    private fun action(game: TestGame, index: Int, target: EntityId? = null) = ActivateAbility(
        game.player1Id, game.findPermanent(name)!!,
        cardRegistry.getCard(name)!!.script.activatedAbilities[index].id,
        targets = target?.let { listOf(ChosenTarget.Permanent(it)) } ?: emptyList())
    private fun activate(game: TestGame, index: Int, target: EntityId? = null) {
        game.execute(action(game, index, target)).error shouldBe null
        game.resolveStack()
    }
    private fun cost(game: TestGame, card: String) = CostCalculator(cardRegistry, services.predicateEvaluator)
        .calculateEffectiveCost(game.state, cardRegistry.getCard(card)!!, game.player1Id)

    init {
        test("Urza starts at seven with a blue-white indicator and activates twice but not three times") {
            val game = board()
            val id = game.findPermanent(name)!!
            game.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 7
            game.state.projectedState.getColors(id) shouldBe setOf("WHITE", "BLUE")
            activate(game, 0); activate(game, 0)
            game.getLifeTotal(1) shouldBe 24
            game.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 11
            game.getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == id } shouldBe false
            game.execute(action(game, 0)).error shouldNotBe null
        }
        test("plus two discounts only artifacts instants and sorceries and expires at end of turn") {
            val game = board()
            activate(game, 0)
            cost(game, "Millstone") shouldBe ManaCost.ZERO
            cost(game, "Divination") shouldBe ManaCost.parse("{U}")
            cost(game, "Cancel") shouldBe ManaCost.parse("{U}{U}")
            cost(game, "Grizzly Bears") shouldBe ManaCost.parse("{1}{G}")
            cost(game, "Saheeli, Filigree Master") shouldBe ManaCost.parse("{2}{U}{R}")
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            cost(game, "Millstone") shouldBe ManaCost.parse("{2}")
        }
        test("two plus two resolutions stack discounts without reducing colored mana") {
            val game = board()
            activate(game, 0); activate(game, 0)
            cost(game, "Divination") shouldBe ManaCost.parse("{U}")
            cost(game, "Cancel") shouldBe ManaCost.parse("{U}{U}")
            cost(game, "The Mightstone and Weakstone") shouldBe ManaCost.parse("{1}")
        }
        test("plus one draws two before asking which card to discard") {
            val game = board()
            activate(game, 1)
            game.state.getHand(game.player1Id).size shouldBe 2
            (game.state.pendingDecision is SelectCardsDecision) shouldBe true
            val discarded = game.state.getHand(game.player1Id).first()
            game.selectCards(listOf(discarded)).error shouldBe null
            game.state.getHand(game.player1Id).size shouldBe 1
            game.state.getGraveyard(game.player1Id).contains(discarded) shouldBe true
            activate(game, 2)
        }
        test("zero creates two colorless one-one Soldier artifact creatures") {
            val game = board()
            activate(game, 2)
            val tokens = game.state.getBattlefield().filter { game.state.projectedState.hasSubtype(it, "Soldier") }
            tokens.size shouldBe 2
            for (id in tokens) {
                game.state.projectedState.getPower(id) shouldBe 1
                game.state.projectedState.getToughness(id) shouldBe 1
                game.state.projectedState.getColors(id) shouldBe emptySet()
                game.state.projectedState.hasType(id, "ARTIFACT") shouldBe true
                game.state.projectedState.isCreature(id) shouldBe true
            }
        }
        test("minus three exiles a nonland permanent and rejects a land") {
            val game = board()
            game.execute(action(game, 3, game.findPermanent("Forest")!!)).error shouldNotBe null
            val enemy = game.findPermanents("Grizzly Bears").first { game.state.projectedState.getController(it) == game.player2Id }
            activate(game, 3, enemy)
            game.isInExile(2, "Grizzly Bears") shouldBe true
            game.state.getEntity(game.findPermanent(name)!!)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 4
        }
        test("ultimate protects your artifacts and planeswalkers before destroying nonlands") {
            val game = board()
            val urza = game.findPermanent(name)!!
            game.state = game.state.updateEntity(urza) { it.with(CountersComponent(mapOf(CounterType.LOYALTY to 11))) }
            activate(game, 4)
            val survivors = game.state.getBattlefield(game.player1Id)
            survivors.size shouldBe 4 // Urza, Saheeli, Ornithopter, Island
            game.findPermanent("Grizzly Bears") shouldBe null
            game.state.getBattlefield(game.player2Id).size shouldBe 1
            game.state.projectedState.hasKeyword(urza, Keyword.INDESTRUCTIBLE) shouldBe true
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.hasKeyword(urza, Keyword.INDESTRUCTIBLE) shouldBe false
        }
        test("ultimate still resolves when paying its cost removes Urza") {
            val game = board()
            game.state = game.state.updateEntity(game.findPermanent(name)!!) {
                it.with(CountersComponent(mapOf(CounterType.LOYALTY to 10)))
            }
            activate(game, 4)
            game.findPermanent(name) shouldBe null
            game.findPermanent("Saheeli, Filigree Master") shouldNotBe null
            game.state.getBattlefield(game.player1Id).size shouldBe 3
            game.state.getBattlefield(game.player2Id).size shouldBe 1
        }
    }
}
