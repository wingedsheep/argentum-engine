package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.ArcadeGannon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Arcade Gannon — {2}{W}{U} Legendary Creature — Human Doctor 2/3
 * "{T}: Draw a card, then discard a card. Put a quest counter on Arcade Gannon."
 * "For Auld Lang Syne — Once during each of your turns, you may cast an artifact or Human spell
 * from your graveyard with mana value less than or equal to the number of quest counters on
 * Arcade Gannon."
 *
 * The graveyard permission's mana-value cap is source-relative: it reads the quest counters on the
 * Arcade Gannon that grants it, live, when the cast is offered and when it is made.
 */
class ArcadeGannonScenarioTest : FunSpec({

    val bauble = card("Zero Bauble") {
        manaCost = "{0}"
        typeLine = "Artifact"
    }
    val gizmo = card("Two Gizmo") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }
    val otherGizmo = card("Other Two Gizmo") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }
    val wanderer = card("Wasteland Wanderer") {
        manaCost = "{1}{W}"
        colorIdentity = "W"
        typeLine = "Creature — Human Scout"
        power = 2
        toughness = 2
    }
    val ghoul = card("Wasteland Ghoul") {
        manaCost = "{1}{B}"
        colorIdentity = "B"
        typeLine = "Creature — Zombie Mutant"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArcadeGannon, bauble, gizmo, otherGizmo, wanderer, ghoul))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun setQuestCounters(driver: GameTestDriver, gannon: EntityId, n: Int) {
        driver.replaceState(driver.state.updateEntity(gannon) {
            it.with(CountersComponent(mapOf(CounterType.QUEST to n)))
        })
    }

    fun graveyardCasts(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player)
            .filter { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId }

    fun endTurn(driver: GameTestDriver) {
        val startTurn = driver.state.turnNumber
        var guard = 0
        while (driver.state.turnNumber == startTurn && guard++ < 300) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("the mana-value cap is the number of quest counters on Arcade Gannon") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val gannon = driver.putPermanentOnBattlefield(player, "Arcade Gannon")
        val zero = driver.putCardInGraveyard(player, "Zero Bauble")
        val two = driver.putCardInGraveyard(player, "Two Gizmo")
        driver.giveColorlessMana(player, 2)

        withClue("no quest counters: only a mana value 0 artifact fits under the cap") {
            graveyardCasts(driver, player, zero) shouldHaveSize 1
            graveyardCasts(driver, player, two) shouldHaveSize 0
        }

        setQuestCounters(driver, gannon, 1)
        withClue("one quest counter is still below mana value 2") {
            graveyardCasts(driver, player, two) shouldHaveSize 0
        }

        setQuestCounters(driver, gannon, 2)
        graveyardCasts(driver, player, two) shouldHaveSize 1
        driver.submit(CastSpell(playerId = player, cardId = two)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(player, "Two Gizmo") shouldNotBe null
    }

    test("covers artifact or Human spells only") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val gannon = driver.putPermanentOnBattlefield(player, "Arcade Gannon")
        setQuestCounters(driver, gannon, 5)
        val human = driver.putCardInGraveyard(player, "Wasteland Wanderer")
        val zombie = driver.putCardInGraveyard(player, "Wasteland Ghoul")
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)

        graveyardCasts(driver, player, human) shouldHaveSize 1
        withClue("a Zombie Mutant creature is neither an artifact nor a Human") {
            graveyardCasts(driver, player, zombie) shouldHaveSize 0
        }
    }

    test("the tap ability loots and adds the quest counter that opens the cap") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val gannon = driver.putPermanentOnBattlefield(player, "Arcade Gannon")
        driver.removeSummoningSickness(gannon)
        val gizmoCard = driver.putCardInGraveyard(player, "Two Gizmo")
        setQuestCounters(driver, gannon, 1)
        val handBefore = driver.getHand(player).size

        val abilityId = ArcadeGannon.activatedAbilities.single().id
        driver.submit(ActivateAbility(playerId = player, sourceId = gannon, abilityId = abilityId)).outcome shouldBe Outcome.Done
        driver.bothPass()
        // Discard decision: pick any card.
        while (driver.isPaused) driver.autoResolveDecision()

        driver.getHand(player).size shouldBe handBefore
        driver.state.getEntity(gannon)!!.get<CountersComponent>()!!.getCount(CounterType.QUEST) shouldBe 2
        driver.giveColorlessMana(player, 2)
        graveyardCasts(driver, player, gizmoCard) shouldHaveSize 1
    }

    test("once during each of your turns, refreshed on your next turn") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val gannon = driver.putPermanentOnBattlefield(player, "Arcade Gannon")
        setQuestCounters(driver, gannon, 3)
        val first = driver.putCardInGraveyard(player, "Two Gizmo")
        val second = driver.putCardInGraveyard(player, "Other Two Gizmo")
        driver.giveColorlessMana(player, 4)

        driver.submit(CastSpell(playerId = player, cardId = first)).outcome shouldBe Outcome.Done
        driver.bothPass()
        withClue("the permission is spent for this turn") {
            graveyardCasts(driver, player, second) shouldHaveSize 0
        }

        endTurn(driver)
        endTurn(driver)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe player
        driver.giveColorlessMana(player, 2)
        graveyardCasts(driver, player, second) shouldHaveSize 1
    }

    test("a new Arcade Gannon brings a new cast the same turn") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val first = driver.putPermanentOnBattlefield(player, "Arcade Gannon")
        setQuestCounters(driver, first, 2)
        val castFirst = driver.putCardInGraveyard(player, "Two Gizmo")
        val castSecond = driver.putCardInGraveyard(player, "Other Two Gizmo")
        driver.giveColorlessMana(player, 4)

        driver.submit(CastSpell(playerId = player, cardId = castFirst)).outcome shouldBe Outcome.Done
        driver.bothPass()
        graveyardCasts(driver, player, castSecond) shouldHaveSize 0

        driver.moveToGraveyard(first)
        val second = driver.putPermanentOnBattlefield(player, "Arcade Gannon")
        withClue("the new Gannon has no quest counters yet, so its cap is 0") {
            graveyardCasts(driver, player, castSecond) shouldHaveSize 0
        }
        setQuestCounters(driver, second, 2)
        graveyardCasts(driver, player, castSecond) shouldHaveSize 1
    }
})
