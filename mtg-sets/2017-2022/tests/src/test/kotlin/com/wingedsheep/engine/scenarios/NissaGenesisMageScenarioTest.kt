package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.NissaGenesisMage
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nissa, Genesis Mage (HOU #200, {5}{G}{G}, Loyalty 5).
 *
 * The +2 has two independent "up to two" target groups — creatures, then lands — and any of the
 * four slots may be left empty. The −10 looks at the top ten cards and puts any number of the
 * creature and/or land cards among them onto the battlefield; everything else, including creature
 * cards the player passed on, goes to the bottom of the library.
 */
class NissaGenesisMageScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(NissaGenesisMage))

        fun plusTwoBoard() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Nissa, Genesis Mage")
            .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
            .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
            .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("+2 untaps two target creatures and two target lands, leaving the rest tapped") {
            val game = plusTwoBoard().build()
            val nissa = game.findPermanent("Nissa, Genesis Mage")!!
            setLoyalty(game, nissa, 5)
            val (myBears, theirBears) = game.findPermanents("Grizzly Bears").let { bears ->
                bears.partition { game.state.getEntity(it)!!.get<CardComponent>()!!.ownerId == game.player1Id }
                    .let { (mine, theirs) -> mine.single() to theirs.single() }
            }
            val courser = game.findPermanent("Centaur Courser")!!
            val (forest1, forest2, forest3) = game.findPermanents("Forest")

            activate(
                game, nissa, index = 0,
                targets = listOf(myBears, theirBears, forest1, forest2).map { ChosenTarget.Permanent(it) }
            )
            game.resolveStack()

            withClue("both targeted creatures untap, including the opponent's") {
                tapped(game, myBears) shouldBe false
                tapped(game, theirBears) shouldBe false
            }
            withClue("both targeted lands untap") {
                tapped(game, forest1) shouldBe false
                tapped(game, forest2) shouldBe false
            }
            withClue("untargeted permanents stay tapped") {
                tapped(game, courser) shouldBe true
                tapped(game, forest3) shouldBe true
            }
            withClue("+2 took Nissa to 7") { loyalty(game, nissa) shouldBe 7 }
        }

        test("+2 may leave the creature group empty and target only a land") {
            val game = plusTwoBoard().build()
            val nissa = game.findPermanent("Nissa, Genesis Mage")!!
            setLoyalty(game, nissa, 5)
            val forest = game.findPermanents("Forest").first()

            activate(game, nissa, index = 0, targets = listOf(ChosenTarget.Permanent(forest)))
            game.resolveStack()

            withClue("the lone land target untaps") { tapped(game, forest) shouldBe false }
            withClue("no creature was targeted, so none untap") {
                game.findPermanents("Grizzly Bears").forEach { tapped(game, it) shouldBe true }
            }
            loyalty(game, nissa) shouldBe 7
        }

        test("−3 gives target creature +5/+5") {
            val game = plusTwoBoard().build()
            val nissa = game.findPermanent("Nissa, Genesis Mage")!!
            setLoyalty(game, nissa, 5)
            val courser = game.findPermanent("Centaur Courser")!!

            activate(game, nissa, index = 1, targets = listOf(ChosenTarget.Permanent(courser)))
            game.resolveStack()

            game.state.projectedState.getPower(courser) shouldBe 8
            game.state.projectedState.getToughness(courser) shouldBe 8
            loyalty(game, nissa) shouldBe 2
        }

        test("−10 puts the chosen creature and land cards onto the battlefield and the rest on the bottom") {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nissa, Genesis Mage")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
            repeat(3) { builder.withCardInLibrary(1, "Lightning Bolt") }
            repeat(3) { builder.withCardInLibrary(1, "Giant Growth") }
            val game = builder
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val nissa = game.findPermanent("Nissa, Genesis Mage")!!
            setLoyalty(game, nissa, 10)

            activate(game, nissa, index = 2, targets = emptyList())
            game.resolveStack()

            val decision = game.getPendingDecision()
            withClue("the −10 asks which creature/land cards to put onto the battlefield") {
                (decision is SelectCardsDecision) shouldBe true
            }
            decision as SelectCardsDecision
            val nameOf = { id: EntityId -> game.state.getEntity(id)!!.get<CardComponent>()!!.name }
            withClue("only the creature and land cards are selectable") {
                decision.options.map(nameOf).toSet() shouldBe setOf("Grizzly Bears", "Centaur Courser", "Forest")
                decision.options.size shouldBe 4
            }
            val bears = decision.options.first { nameOf(it) == "Grizzly Bears" }
            val forest = decision.options.first { nameOf(it) == "Forest" }
            game.selectCards(listOf(bears, forest))

            withClue("the chosen creature and land entered the battlefield") {
                game.state.getBattlefield().contains(bears) shouldBe true
                game.state.getBattlefield().contains(forest) shouldBe true
            }
            withClue("the creature card passed on stays in the library") {
                game.isOnBattlefield("Centaur Courser") shouldBe false
                game.findCardsInLibrary(1, "Centaur Courser").size shouldBe 1
            }
            withClue("the other eight cards went back to the library") { game.librarySize(1) shouldBe 8 }
            withClue("Nissa went to 0 loyalty and died") { game.findPermanent("Nissa, Genesis Mage") shouldBe null }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget>) {
        val ability = cardRegistry.getCard("Nissa, Genesis Mage")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets
            )
        ).error shouldBe null
    }

    private fun tapped(game: TestGame, id: EntityId): Boolean = game.state.getEntity(id)!!.has<TappedComponent>()

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
