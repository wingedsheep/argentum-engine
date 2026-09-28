package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.OmnathLocusOfAll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Omnath, Locus of All (MOM #249).
 *
 * "If you would lose unspent mana, that mana becomes black instead.
 *  At the beginning of your first main phase, look at the top card of your library. You may reveal
 *  that card if it has three or more colored mana symbols in its mana cost. If you do, add three mana
 *  in any combination of its colors and put it into your hand. If you don't reveal it, put it into
 *  your hand."
 */
class OmnathLocusOfAllScenarioTest : FunSpec({

    // Three coloured pips across two colours: {U}{R}{R} — revealable, colours {U, R}.
    val threePips = card("Omnath Test Three Pips") {
        manaCost = "{U}{R}{R}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }
    // Hybrid pips count for their colours: {R/G}{G}{G/W} is three coloured symbols.
    val hybridPips = card("Omnath Test Hybrid Pips") {
        manaCost = "{R/G}{G}{G/W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }
    // Only two coloured pips — can't be revealed.
    val twoPips = card("Omnath Test Two Pips") {
        manaCost = "{3}{U}{R}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OmnathLocusOfAll, threePips, hybridPips, twoPips))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    /** Omnath out, [topCard] on top of the library, trigger on the stack at the first main phase. */
    fun GameTestDriver.reachTrigger(topCard: String): Pair<EntityId, EntityId> {
        val player = activePlayer!!
        putCreatureOnBattlefield(player, "Omnath, Locus of All")
        // The starting player skips their first draw, so the planted card is still on top.
        val top = putCardOnTopOfLibrary(player, topCard)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        stackSize shouldBe 1
        bothPass()
        return player to top
    }

    fun GameTestDriver.chooseColor(playerId: EntityId, color: Color, expectedChoices: Set<Color>) {
        val decision = pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        decision.availableColors shouldBe expectedChoices
        submitDecision(playerId, ColorChosenResponse(decision.id, color))
    }

    test("unspent mana becomes black as a phase ends; the opponent's still empties") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(active, "Omnath, Locus of All")

        driver.giveMana(active, Color.WHITE, 1)
        driver.giveMana(active, Color.RED, 1)
        driver.giveColorlessMana(active, 1)
        driver.giveMana(opponent, Color.GREEN, 2)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        driver.pool(active).black shouldBe 3
        driver.pool(active).total shouldBe 3
        driver.pool(opponent).total shouldBe 0
    }

    test("restricted mana keeps its restriction when it becomes black (ruling)") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(active, "Omnath, Locus of All")

        driver.giveRestrictedMana(active, Color.GREEN, 2, ManaRestriction.InstantOrSorceryOnly)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        val pool = driver.pool(active)
        pool.black shouldBe 0
        pool.restrictedMana.size shouldBe 2
        pool.restrictedMana.map { it.color }.toSet() shouldBe setOf(Color.BLACK)
        pool.restrictedMana.map { it.restriction }.toSet() shouldBe setOf(ManaRestriction.InstantOrSorceryOnly)
    }

    test("once Omnath is gone the mana empties normally") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val omnath = driver.putCreatureOnBattlefield(active, "Omnath, Locus of All")
        driver.giveMana(active, Color.BLUE, 2)
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.pool(active).black shouldBe 2

        driver.moveToGraveyard(omnath)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.pool(active).total shouldBe 0
    }

    test("revealing a three-pip card adds three mana in any combination of its colours, then it goes to hand") {
        val driver = newDriver()
        val (player, top) = driver.reachTrigger("Omnath Test Three Pips")

        val reveal = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        reveal.options shouldContain top
        driver.submitCardSelection(player, listOf(top))

        driver.chooseColor(player, Color.BLUE, setOf(Color.BLUE, Color.RED))
        driver.chooseColor(player, Color.RED, setOf(Color.BLUE, Color.RED))
        driver.chooseColor(player, Color.RED, setOf(Color.BLUE, Color.RED))

        driver.pool(player).blue shouldBe 1
        driver.pool(player).red shouldBe 2
        driver.state.getZone(player, Zone.HAND) shouldContain top

        // The mana outlives the main phase — as black, courtesy of the first ability.
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.pool(player).black shouldBe 3
    }

    test("hybrid pips count: a {R/G}{G}{G/W} card can be revealed and offers red, green and white") {
        val driver = newDriver()
        val (player, top) = driver.reachTrigger("Omnath Test Hybrid Pips")

        driver.submitCardSelection(player, listOf(top))
        driver.chooseColor(player, Color.WHITE, setOf(Color.RED, Color.GREEN, Color.WHITE))
        driver.chooseColor(player, Color.GREEN, setOf(Color.RED, Color.GREEN, Color.WHITE))
        driver.chooseColor(player, Color.RED, setOf(Color.RED, Color.GREEN, Color.WHITE))

        driver.pool(player).white shouldBe 1
        driver.pool(player).green shouldBe 1
        driver.pool(player).red shouldBe 1
        driver.state.getZone(player, Zone.HAND) shouldContain top
    }

    test("declining to reveal adds no mana but still puts the card into your hand") {
        val driver = newDriver()
        val (player, top) = driver.reachTrigger("Omnath Test Three Pips")

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, emptyList())

        driver.pendingDecision shouldBe null
        driver.pool(player).total shouldBe 0
        driver.state.getZone(player, Zone.HAND) shouldContain top
    }

    test("a card with only two coloured pips can't be revealed — no mana, straight to hand") {
        val driver = newDriver()
        val (player, top) = driver.reachTrigger("Omnath Test Two Pips")

        (driver.pendingDecision as? SelectCardsDecision)?.let { decision ->
            decision.options shouldContainExactly emptyList()
            driver.submitCardSelection(player, emptyList())
        }

        driver.pool(player).total shouldBe 0
        driver.state.getZone(player, Zone.HAND) shouldContain top
    }
})
