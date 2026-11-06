package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.NissaStewardOfElements
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nissa, Steward of Elements (AKH #204, {X}{G}{U}, Loyalty X).
 *
 * Cast for X she enters with X loyalty. Her 0 puts the top card of the library onto the battlefield
 * only when it is a land, or a creature whose mana value is at most her loyalty; otherwise it stays
 * on top. Her −6 untaps up to two target lands and makes them 5/5 flying, hasty Elementals that are
 * still lands.
 */
class NissaStewardOfElementsScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(NissaStewardOfElements))

        fun board(top: String?) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Nissa, Steward of Elements")
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .apply { if (top != null) withCardInLibrary(1, top) }
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("cast with X = 3, Nissa enters with three loyalty counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nissa, Steward of Elements")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Nissa, Steward of Elements", xValue = 3).error shouldBe null
            game.resolveStack()

            val nissa = game.findPermanent("Nissa, Steward of Elements")
            withClue("Nissa is on the battlefield") { (nissa != null) shouldBe true }
            loyalty(game, nissa!!) shouldBe 3
        }

        test("0 puts a creature card with mana value at most her loyalty onto the battlefield") {
            val game = board("Centaur Courser").build()
            val nissa = game.findPermanent("Nissa, Steward of Elements")!!
            setLoyalty(game, nissa, 3)

            activate(game, nissa, index = 1)
            game.resolveStack()

            val decision = game.getPendingDecision()
            withClue("the 0 offers the three-mana creature at loyalty 3") { (decision is SelectCardsDecision) shouldBe true }
            decision as SelectCardsDecision
            decision.options.size shouldBe 1
            game.selectCards(decision.options)

            game.isOnBattlefield("Centaur Courser") shouldBe true
            withClue("a 0 ability leaves her loyalty unchanged") { loyalty(game, nissa) shouldBe 3 }
        }

        test("0 leaves a creature card whose mana value exceeds her loyalty on top of the library") {
            val game = board("Centaur Courser").build()
            val nissa = game.findPermanent("Nissa, Steward of Elements")!!
            setLoyalty(game, nissa, 2)

            activate(game, nissa, index = 1)
            game.resolveStack()

            val decision = game.getPendingDecision()
            if (decision is SelectCardsDecision) {
                withClue("nothing is eligible") { decision.options.size shouldBe 0 }
                game.selectCards(emptyList())
            }
            game.isOnBattlefield("Centaur Courser") shouldBe false
            withClue("the card stays on top of the library") {
                game.findCardsInLibrary(1, "Centaur Courser").size shouldBe 1
                game.librarySize(1) shouldBe 1
            }
        }

        test("0 puts a land card onto the battlefield regardless of its mana value") {
            val game = board("Island").build()
            val nissa = game.findPermanent("Nissa, Steward of Elements")!!
            setLoyalty(game, nissa, 1)

            activate(game, nissa, index = 1)
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            decision.options.size shouldBe 1
            game.selectCards(decision.options)

            game.isOnBattlefield("Island") shouldBe true
            game.librarySize(1) shouldBe 0
        }

        test("−6 untaps two target lands and makes them 5/5 flying, hasty Elemental lands") {
            val game = board(null).build()
            val nissa = game.findPermanent("Nissa, Steward of Elements")!!
            setLoyalty(game, nissa, 6)
            val (forest1, forest2, forest3) = game.findPermanents("Forest")

            activate(game, nissa, index = 2, targets = listOf(forest1, forest2).map { ChosenTarget.Permanent(it) })
            game.resolveStack()

            val projected = game.state.projectedState
            listOf(forest1, forest2).forEach { land ->
                withClue("a targeted land untaps") { game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false }
                projected.isCreature(land) shouldBe true
                withClue("it's still a land") { projected.hasType(land, "LAND") shouldBe true }
                projected.getPower(land) shouldBe 5
                projected.getToughness(land) shouldBe 5
                projected.hasKeyword(land, Keyword.FLYING) shouldBe true
                projected.hasKeyword(land, Keyword.HASTE) shouldBe true
                projected.hasSubtype(land, "Elemental") shouldBe true
            }
            withClue("the untargeted land is untouched") {
                game.state.getEntity(forest3)!!.has<TappedComponent>() shouldBe true
                projected.isCreature(forest3) shouldBe false
            }
            withClue("−6 from 6 loyalty puts Nissa into the graveyard") {
                game.findPermanent("Nissa, Steward of Elements") shouldBe null
            }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard("Nissa, Steward of Elements")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = source, abilityId = ability.id, targets = targets)
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
