package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.fem.cards.EbonPraetor
import com.wingedsheep.mtg.sets.definitions.mom.cards.RealmbreakerTheInvasionTree
import com.wingedsheep.mtg.sets.definitions.usg.cards.LayWaste
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Realmbreaker, the Invasion Tree (MOM #263).
 *
 * {2}, {T}: Target opponent mills three cards. Put a land card from their graveyard onto the
 * battlefield tapped under your control. It gains "If this land would leave the battlefield,
 * exile it instead of putting it anywhere else."
 * {10}, {T}, Sacrifice Realmbreaker: Search your library for any number of Praetor cards, put
 * them onto the battlefield, then shuffle.
 */
class RealmbreakerTheInvasionTreeScenarioTest : FunSpec({

    val millAbilityId = RealmbreakerTheInvasionTree.activatedAbilities[0].id
    val searchAbilityId = RealmbreakerTheInvasionTree.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(RealmbreakerTheInvasionTree, EbonPraetor, LayWaste))
        // Nonland libraries, so every land card in a graveyard was placed there by the test.
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        return driver
    }

    fun resolveAll(driver: GameTestDriver, pick: (SelectCardsDecision) -> List<EntityId>) {
        var safety = 0
        while (safety++ < 40) {
            when (val pending = driver.state.pendingDecision) {
                is SelectCardsDecision -> driver.submitCardSelection(pending.playerId, pick(pending))
                null -> if (driver.stackSize > 0) driver.bothPass() else return
                else -> error("unexpected decision $pending")
            }
        }
    }

    test("mills three, steals a land already in their graveyard tapped, and exiles it when it would die") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tree = driver.putPermanentOnBattlefield(you, "Realmbreaker, the Invasion Tree")
        val forest = driver.putCardInGraveyard(opponent, "Forest")
        val gyBefore = driver.getGraveyard(opponent).size

        driver.giveColorlessMana(you, 2)
        driver.submit(
            ActivateAbility(you, tree, millAbilityId, targets = listOf(ChosenTarget.Player(opponent)))
        ).outcome shouldBe Outcome.Done
        resolveAll(driver) { d -> d.options.filter { it == forest } }

        // Three Grizzly Bears milled; the pre-existing Forest left for the battlefield.
        driver.getGraveyard(opponent).size shouldBe gyBefore + 3 - 1
        driver.getController(forest) shouldBe you
        driver.findPermanent(you, "Forest") shouldBe forest
        driver.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true

        // Destroying the stolen land exiles it instead of putting it into its owner's graveyard.
        val waste = driver.putCardInHand(opponent, "Lay Waste")
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe opponent
        driver.giveMana(opponent, Color.RED, 1)
        driver.giveColorlessMana(opponent, 3)
        driver.castSpellWithTargets(opponent, waste, listOf(ChosenTarget.Permanent(forest)))
        driver.bothPass()

        driver.getExile(opponent) shouldContain forest
        driver.getGraveyardCardNames(opponent).contains("Forest") shouldBe false
    }

    test("sacrifice ability puts any number of Praetor cards onto the battlefield") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tree = driver.putPermanentOnBattlefield(you, "Realmbreaker, the Invasion Tree")
        val praetorA = driver.putCardOnTopOfLibrary(you, "Ebon Praetor")
        val praetorB = driver.putCardOnTopOfLibrary(you, "Ebon Praetor")

        driver.giveColorlessMana(you, 10)
        driver.submit(ActivateAbility(you, tree, searchAbilityId)).outcome shouldBe Outcome.Done
        driver.getGraveyardCardNames(you) shouldContain "Realmbreaker, the Invasion Tree"

        var offered: List<EntityId> = emptyList()
        resolveAll(driver) { d -> offered = d.options; d.options }

        // Only the Praetors were offered — never the Grizzly Bears in the library.
        offered.toSet() shouldBe setOf(praetorA, praetorB)
        listOf(driver.getController(praetorA), driver.getController(praetorB)) shouldContainAll listOf(you)
        driver.state.getBattlefield().containsAll(listOf(praetorA, praetorB)) shouldBe true
    }
})
