package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.TeferiWhoSlowsTheSunset
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Teferi, Who Slows the Sunset (MID #245, {2}{W}{U}, Loyalty 4).
 *
 * The +1 splits its chosen permanents on who controls them — yours untap, the rest tap — and any of
 * its three targets may be skipped. The −7 emblem untaps your permanents in the opponent's untap
 * step and draws you a card in their draw step, right after their own draw.
 */
class TeferiWhoSlowsTheSunsetScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(TeferiWhoSlowsTheSunset))

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Teferi, Who Slows the Sunset")
            .withCardOnBattlefield(1, "Island", tapped = true)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("+1 untaps the chosen land you control, taps the opponent's creature, and gains 2 life") {
            val game = board().build()
            val teferi = game.findPermanent("Teferi, Who Slows the Sunset")!!
            setLoyalty(game, teferi, 4)
            val island = game.findPermanent("Island")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            activate(game, teferi, index = 0, targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(island)))
            game.resolveStack()

            withClue("your chosen land untaps") { game.state.getEntity(island)!!.has<TappedComponent>() shouldBe false }
            withClue("the opponent's chosen creature taps") { game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true }
            withClue("you gain 2 life") { game.getLifeTotal(1) shouldBe 22 }
            withClue("+1 took Teferi to 5") { loyalty(game, teferi) shouldBe 5 }
        }

        test("+1 with no targets chosen still gains 2 life") {
            val game = board().build()
            val teferi = game.findPermanent("Teferi, Who Slows the Sunset")!!
            setLoyalty(game, teferi, 4)

            activate(game, teferi, index = 0, targets = emptyList())
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 22
        }

        test("−7 emblem: untap during the opponent's untap step, draw after their draw") {
            val game = board().build()
            val teferi = game.findPermanent("Teferi, Who Slows the Sunset")!!
            setLoyalty(game, teferi, 7)
            val island = game.findPermanent("Island")!!

            activate(game, teferi, index = 2, targets = emptyList())
            game.resolveStack()
            withClue("Teferi went to 0 loyalty and died") { game.findPermanent("Teferi, Who Slows the Sunset") shouldBe null }

            val myHand = game.handSize(1)
            val oppHand = game.handSize(2)
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)

            withClue("it is the opponent's draw step") { game.state.activePlayerId shouldBe game.player2Id }
            withClue("your tapped Island untapped during their untap step") {
                game.state.getEntity(island)!!.has<TappedComponent>() shouldBe false
            }
            withClue("the opponent drew their card") { game.handSize(2) shouldBe oppHand + 1 }
            withClue("and you drew one too") { game.handSize(1) shouldBe myHand + 1 }
            withClue("the opponent holds priority") { game.state.priorityPlayerId shouldBe game.player2Id }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget>) {
        val ability = cardRegistry.getCard("Teferi, Who Slows the Sunset")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
