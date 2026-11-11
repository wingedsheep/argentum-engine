package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Engine tests for ingest (CR 702.115) as a keyword-derived trigger: "Whenever this creature deals
 * combat damage to a player, that player exiles the top card of their library."
 *
 * | Rule / ruling | Covered by |
 * |---|---|
 * | 702.115a — combat damage to a player → that player exiles their top card, face up in their exile | "combat damage to a player exiles the top card of that player's library" |
 * | 702.115a — "to a player": blocked, no trigger | "a blocked ingest creature exiles nothing" |
 * | 702.115a — "to a player": damage to a planeswalker doesn't trigger | "combat damage to a planeswalker doesn't trigger ingest" |
 * | 702.115a — "that player": the damaged player in multiplayer | "in multiplayer the damaged player exiles, not another opponent" |
 * | 702.115a — each combat damage event triggers (double strike hits twice) | "double strike ingests twice" |
 * | 702.115b — multiple instances trigger separately | "each instance of ingest triggers separately" |
 * | ruling — an empty library exiles nothing and the player doesn't lose | "an empty library exiles nothing" |
 * | lose all abilities strips ingest | "a creature that lost all abilities doesn't ingest" |
 * | a granted ingest works like a printed one | "granted ingest triggers" |
 */
class IngestKeywordScenarioTest : FunSpec({

    val ingestDrone = card("Test Ingest Drone") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Eldrazi Drone"
        power = 2
        toughness = 2
        keywords(Keyword.INGEST)
    }

    val doubleIngest = card("Test Double Ingest Drone") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Eldrazi Drone"
        power = 2
        toughness = 2
        keywordAbilities(KeywordAbility.Simple(Keyword.INGEST), KeywordAbility.Simple(Keyword.INGEST))
    }

    val strikingIngest = card("Test Double Striking Ingest Drone") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Eldrazi Drone"
        power = 1
        toughness = 1
        keywords(Keyword.INGEST, Keyword.DOUBLE_STRIKE)
    }

    val walker = card("Test Ingest Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 5
        oracleText = ""
    }

    val strip = card("Test Ingest Strip") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(t)
        }
    }

    val grant = card("Test Grant Ingest") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(Keyword.INGEST, t)
        }
    }

    val extra = listOf(ingestDrone, doubleIngest, strikingIngest, walker, strip, grant)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extra)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.creature(name: String, player: EntityId = activePlayer!!): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    /** Attack [defender] with [attackers] and play out combat through to the postcombat main phase. */
    fun GameTestDriver.attackThroughCombat(attackers: List<EntityId>, defender: EntityId) {
        val me = activePlayer!!
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, attackers.associateWith { defender }).error shouldBe null
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
    }

    fun GameTestDriver.castOn(spellName: String, target: EntityId) {
        val me = activePlayer!!
        val spell = putCardInHand(me, spellName)
        giveMana(me, Color.BLUE, 1)
        castSpell(me, spell, targets = listOf(target)).error shouldBe null
        bothPass()
    }

    test("combat damage to a player exiles the top card of that player's library") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Ingest Drone")
        val top = driver.putCardOnTopOfLibrary(opponent, "Grizzly Bears")
        val librarySize = driver.state.getLibrary(opponent).size
        val myLibrary = driver.state.getLibrary(me).size

        driver.attackThroughCombat(listOf(drone), opponent)

        driver.getLifeTotal(opponent) shouldBe 18
        withClue("the top card goes to its owner's exile") { driver.getExile(opponent) shouldContainExactly listOf(top) }
        driver.state.getLibrary(opponent).size shouldBe librarySize - 1
        withClue("the attacker's own library is untouched") { driver.state.getLibrary(me).size shouldBe myLibrary }
        driver.getExile(me).shouldBeEmpty()
    }

    test("a blocked ingest creature exiles nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Ingest Drone")
        val blocker = driver.creature("Grizzly Bears", opponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(drone), opponent).error shouldBe null
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opponent, mapOf(blocker to listOf(drone))).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.getLifeTotal(opponent) shouldBe 20
        driver.getExile(opponent).shouldBeEmpty()
    }

    test("combat damage to a planeswalker doesn't trigger ingest") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Ingest Drone")
        val pw = driver.putPermanentOnBattlefield(opponent, "Test Ingest Walker")
        driver.replaceState(driver.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 5)) })

        driver.attackThroughCombat(listOf(drone), pw)

        withClue("the planeswalker took the damage") {
            driver.state.getEntity(pw)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
        }
        driver.getExile(opponent).shouldBeEmpty()
    }

    test("in multiplayer the damaged player exiles, not another opponent") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extra)
        val (p1, p2, p3) = driver.initMultiplayer(List(3) { Deck.of("Island" to 40) })
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Ingest Drone", p1)

        driver.attackThroughCombat(listOf(drone), p3)

        driver.getExile(p3) shouldHaveSize 1
        driver.getExile(p2).shouldBeEmpty()
        driver.getExile(p1).shouldBeEmpty()
    }

    test("double strike ingests twice") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Double Striking Ingest Drone")

        driver.attackThroughCombat(listOf(drone), opponent)

        driver.getLifeTotal(opponent) shouldBe 18
        withClue("one trigger per combat damage step") { driver.getExile(opponent) shouldHaveSize 2 }
    }

    test("each instance of ingest triggers separately") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Double Ingest Drone")

        driver.attackThroughCombat(listOf(drone), opponent)

        driver.getExile(opponent) shouldHaveSize 2
    }

    test("an empty library exiles nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Ingest Drone")
        driver.replaceState(driver.state.copy(zones = driver.state.zones - com.wingedsheep.engine.state.ZoneKey(opponent, com.wingedsheep.sdk.core.Zone.LIBRARY)))
        driver.state.getLibrary(opponent).shouldBeEmpty()

        driver.attackThroughCombat(listOf(drone), opponent)

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getExile(opponent).shouldBeEmpty()
        withClue("nobody drew from the empty library, so nobody loses") { driver.state.gameOver shouldBe false }
    }

    test("a creature that lost all abilities doesn't ingest") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val drone = driver.creature("Test Ingest Drone")
        driver.castOn("Test Ingest Strip", drone)

        driver.attackThroughCombat(listOf(drone), opponent)

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getExile(opponent).shouldBeEmpty()
    }

    test("granted ingest triggers") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.creature("Grizzly Bears")
        driver.castOn("Test Grant Ingest", bears)
        driver.state.projectedState.hasKeyword(bears, Keyword.INGEST) shouldBe true

        driver.attackThroughCombat(listOf(bears), opponent)

        driver.getExile(opponent) shouldHaveSize 1
    }
})
