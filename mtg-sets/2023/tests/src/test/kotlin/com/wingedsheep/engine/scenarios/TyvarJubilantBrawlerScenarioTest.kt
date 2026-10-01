package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.TyvarJubilantBrawler
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tyvar, Jubilant Brawler (ONE #218, {1}{B}{G}, loyalty 3).
 *
 *   You may activate abilities of creatures you control as though those creatures had haste.
 *   +1: Untap up to one target creature.
 *   −2: Mill three cards, then you may return a creature card with mana value 2 or less from your
 *       graveyard to the battlefield.
 */
class TyvarJubilantBrawlerScenarioTest : ScenarioTestBase() {

    private val dummy = card("Tyvar Test Dummy") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(1)
            description = "{T}: You gain 1 life."
        }
    }

    private val dummyTap = dummy.activatedAbilities.single().id
    private val plusOne = TyvarJubilantBrawler.activatedAbilities[0].id
    private val minusTwo = TyvarJubilantBrawler.activatedAbilities[1].id

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun tapped(game: TestGame, id: EntityId) = game.state.getEntity(id)?.has<TappedComponent>() == true

    init {
        cardRegistry.register(dummy)

        context("Tyvar, Jubilant Brawler — static") {
            test("a summoning-sick creature's {T} ability is activatable only while Tyvar is out") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar, Jubilant Brawler")
                    .withCardOnBattlefield(1, "Tyvar Test Dummy", summoningSickness = true)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val dummyId = game.findPermanent("Tyvar Test Dummy")!!
                val result = game.execute(ActivateAbility(game.player1Id, dummyId, dummyTap))
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()
                game.getLifeTotal(1) shouldBe 21

                val without = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar Test Dummy", summoningSickness = true)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val sickId = without.findPermanent("Tyvar Test Dummy")!!
                without.getLegalActions(1)
                    .mapNotNull { it.action as? ActivateAbility }
                    .filter { it.sourceId == sickId && it.abilityId == dummyTap }
                    .size shouldBe 0
            }

            test("it grants no attack rights") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar, Jubilant Brawler")
                    .withCardOnBattlefield(1, "Tyvar Test Dummy", summoningSickness = true)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val attackers = game.getLegalActions(1)
                    .firstOrNull { it.actionType == "DeclareAttackers" }
                    ?.validAttackers.orEmpty()
                attackers shouldBe emptyList()
            }
        }

        context("Tyvar, Jubilant Brawler — +1") {
            test("untaps the target creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar, Jubilant Brawler")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val tyvar = game.findPermanent("Tyvar, Jubilant Brawler")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(
                    ActivateAbility(game.player1Id, tyvar, plusOne, targets = listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()
                tapped(game, bears) shouldBe false
                loyalty(game, tyvar) shouldBe 4
            }

            test("can be activated with no target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar, Jubilant Brawler")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val tyvar = game.findPermanent("Tyvar, Jubilant Brawler")!!
                game.execute(ActivateAbility(game.player1Id, tyvar, plusOne)).error shouldBe null
                game.resolveStack()
                loyalty(game, tyvar) shouldBe 4
            }
        }

        context("Tyvar, Jubilant Brawler — −2") {
            test("mills three and returns a milled creature card with mana value 2 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar, Jubilant Brawler")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val tyvar = game.findPermanent("Tyvar, Jubilant Brawler")!!
                game.execute(ActivateAbility(game.player1Id, tyvar, minusTwo)).error shouldBe null
                game.resolveStack()

                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.minSelections shouldBe 0
                decision.maxSelections shouldBe 1
                decision.options shouldContain bears
                withClue("mana value 4 is too big") { decision.options shouldNotContain giant }

                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
                game.librarySize(1) shouldBe 0
                loyalty(game, tyvar) shouldBe 1
            }

            test("may return a creature card that was already in the graveyard, or decline") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tyvar, Jubilant Brawler")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val tyvar = game.findPermanent("Tyvar, Jubilant Brawler")!!
                game.execute(ActivateAbility(game.player1Id, tyvar, minusTwo)).error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                decision.options shouldContain bears

                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.findCardsInGraveyard(1, "Forest").size shouldBe 3
            }
        }
    }
}
