package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vivien, Monsters' Advocate (IKO #175) — {3}{G}{G} planeswalker, loyalty 3.
 *
 *   You may look at the top card of your library any time.
 *   You may cast creature spells from the top of your library.
 *   +1: Create a 3/3 green Beast creature token. Put your choice of a reach counter, a vigilance
 *       counter, or a trample counter on it.
 *   −2: When you next cast a creature spell this turn, search your library for a creature card
 *       with lesser mana value, put it onto the battlefield, then shuffle.
 *
 * Pins down the +1's counter choice, the cast-from-top static being creature-only, and the −2's
 * one-shot delayed trigger: it fires on a creature cast from the top of the library, its search is
 * capped strictly below the cast spell's mana value, and it doesn't fire a second time.
 */
class VivienMonstersAdvocateScenarioTest : ScenarioTestBase() {

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Seed a planeswalker's starting loyalty (withCardOnBattlefield doesn't stamp it). */
    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun ability(index: Int) =
        cardRegistry.getCard("Vivien, Monsters' Advocate")!!.script.activatedAbilities[index]

    init {
        fun setup(vararg library: String): TestGame = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Vivien, Monsters' Advocate")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withLandsOnBattlefield(1, "Forest", 5)
            .withCardInHand(1, "Grizzly Bears")
            .apply { library.forEach { withCardInLibrary(1, it) } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.vivien(): EntityId = findPermanent("Vivien, Monsters' Advocate")!!

        fun TestGame.castFromTop(): CastSpell =
            CastSpell(player1Id, state.getLibrary(player1Id).first(), emptyList())

        test("+1 creates a 3/3 Beast with the chosen keyword counter") {
            val game = setup("Forest")
            val vivien = game.vivien()
            seedLoyalty(game, vivien, 3)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = vivien, abilityId = ability(0).id)
            ).error shouldBe null
            game.resolveStack()

            val choice = game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            choice.options.size shouldBe 3
            game.submitDecision(OptionChosenResponse(choice.id, 2)).error shouldBe null // trample

            val beast = game.findPermanents("Beast Token").single()
            game.state.projectedState.getPower(beast) shouldBe 3
            game.state.projectedState.getToughness(beast) shouldBe 3
            game.state.getEntity(beast)!!.get<CountersComponent>()!!.getCount(CounterType.TRAMPLE) shouldBe 1
            game.state.projectedState.hasKeyword(beast, Keyword.TRAMPLE) shouldBe true
            game.state.projectedState.hasKeyword(beast, Keyword.REACH) shouldBe false
            withClue("Vivien goes up to 4 loyalty") { loyalty(game, vivien) shouldBe 4 }
        }

        test("a creature on top of the library is castable; a noncreature one is not") {
            val creatureTop = setup("Hill Giant")
            withClue("Hill Giant on top is offered as a cast") {
                creatureTop.getLegalActions(1).any {
                    it.actionType == "CastSpell" && it.description.contains("Hill Giant")
                } shouldBe true
            }

            val sorceryTop = setup("Lava Axe")
            withClue("Vivien only lets you cast creature spells from the top") {
                sorceryTop.getLegalActions(1).any {
                    it.actionType == "CastSpell" && it.description.contains("Lava Axe")
                } shouldBe false
            }
        }

        test("-2: the next creature spell finds a creature card of lesser mana value, once") {
            val game = setup("Hill Giant", "Serra Angel", "Llanowar Elves", "Shock", "Hill Giant", "Grizzly Bears")
            val vivien = game.vivien()
            seedLoyalty(game, vivien, 3)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = vivien, abilityId = ability(1).id)
            ).error shouldBe null
            game.resolveStack()
            withClue("Vivien drops to 1 loyalty") { loyalty(game, vivien) shouldBe 1 }

            // Cast Hill Giant (mana value 4) from the top of the library.
            game.execute(game.castFromTop()).error shouldBe null
            game.resolveStack()

            val search = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only creature cards with mana value less than 4 are offered — not the other Hill Giant, Serra Angel, or Shock") {
                search.options shouldContainExactlyInAnyOrder
                    game.findCardsInLibrary(1, "Llanowar Elves") + game.findCardsInLibrary(1, "Grizzly Bears")
            }
            game.selectCards(game.findCardsInLibrary(1, "Llanowar Elves")).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Llanowar Elves") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true

            // The delayed trigger is one-shot: a second creature spell searches nothing.
            val librarySize = game.state.getLibrary(game.player1Id).size
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("no second search") {
                game.state.pendingDecision shouldBe null
                game.state.getLibrary(game.player1Id).size shouldBe librarySize
            }
        }
    }
}
