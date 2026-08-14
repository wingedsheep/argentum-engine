package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.state.components.player.PlayerEnduringStoryComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * **Storied** / the **enduring story** designation (The Hobbit, CR 702.195).
 *
 * A mechanic-level test rather than a card-level one: what is pinned here is the state-based action
 * and the count it reads, which is shared by all nine printed storied cards. The inline cards are
 * deliberately vanilla so nothing but the type line and the keyword is in play.
 *
 * The rules this asserts, one test each:
 *
 * - 702.195a — three or more permanents that are artifacts, Sagas, and/or legendary, *and* a storied
 *   permanent, is what grants the designation. Two is not enough; three without a storied permanent
 *   is not enough.
 * - 702.195a — the three categories are a union over permanents, not a sum over categories. A
 *   legendary artifact is one qualifying permanent.
 * - 702.195a — "for the rest of the game": the designation survives dropping back below three and
 *   survives losing the storied permanent entirely.
 * - 702.195b — any number of players may hold it at once, and one player crossing the threshold does
 *   not hand it to the other.
 * - The check is continuous, not an enters-the-battlefield trigger: a storied permanent that lands
 *   while the board is short still turns on later, when the third qualifying permanent arrives.
 */
class StoriedEnduringStoryTest : FunSpec({

    // A storied 2/2 with no payoff half — the keyword and nothing else, so what these tests observe
    // is the state-based action rather than any card's own text. Legendary because every printed
    // storied card is, which also makes it count toward its own threshold.
    val storiedBear = CardDefinition.creature(
        name = "Storied Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY),
        keywords = setOf(Keyword.STORIED),
        oracleText = "Storied"
    )

    // Nonlegendary, no keyword: a body that never counts toward the threshold and never grants it.
    val plainBear = CardDefinition.creature(
        name = "Plain Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    val plainLegend = CardDefinition.creature(
        name = "Plain Legend",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype.HUMAN),
        power = 1,
        toughness = 1,
        supertypes = setOf(Supertype.LEGENDARY)
    )

    val plainRock = CardDefinition.artifact(
        name = "Plain Rock",
        manaCost = ManaCost.parse("{2}")
    )

    // Both an artifact and legendary — the card that proves the count is a union over permanents.
    val legendaryRock = CardDefinition.artifact(
        name = "Legendary Rock",
        manaCost = ManaCost.parse("{2}"),
        supertypes = setOf(Supertype.LEGENDARY)
    )

    val plainSaga = CardDefinition.enchantment(
        name = "Plain Saga",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype.SAGA)
    )

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(
            listOf(storiedBear, plainBear, plainLegend, plainRock, legendaryRock, plainSaga)
        )
        return d
    }

    /** Walk to a state-based-action poll so the marker component gets written. */
    fun pollSbas(d: GameTestDriver) {
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun hasMarker(d: GameTestDriver, playerId: EntityId): Boolean =
        d.state.getEntity(playerId)?.has<PlayerEnduringStoryComponent>() == true

    test("three qualifying permanents alongside a storied permanent grants the enduring story") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Storied Bear is itself legendary, so it is one of the three.
        d.putCreatureOnBattlefield(active, "Storied Bear")
        d.putCreatureOnBattlefield(active, "Plain Legend")
        d.putPermanentOnBattlefield(active, "Plain Rock")

        EnduringStoryService.has(d.state, active) shouldBe true

        // The live read is true immediately; the marker is written at the next SBA poll, and that is
        // what makes the designation outlive the board (see the persistence tests below).
        pollSbas(d)
        hasMarker(d, active) shouldBe true
    }

    test("two qualifying permanents is one short — no enduring story") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Storied Bear")
        d.putCreatureOnBattlefield(active, "Plain Legend")
        // A nonlegendary, nonartifact body and a land pad the battlefield past three permanents
        // without adding a third *qualifying* one — the threshold is not a permanent count.
        d.putCreatureOnBattlefield(active, "Plain Bear")
        d.putLandOnBattlefield(active, "Forest")

        EnduringStoryService.has(d.state, active) shouldBe false
        pollSbas(d)
        hasMarker(d, active) shouldBe false
    }

    test("three qualifying permanents without a storied permanent grants nothing") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Plain Legend")
        d.putPermanentOnBattlefield(active, "Plain Rock")
        d.putPermanentOnBattlefield(active, "Plain Saga")

        EnduringStoryService.has(d.state, active) shouldBe false
        pollSbas(d)
        hasMarker(d, active) shouldBe false
    }

    test("a legendary artifact counts once, not once per category") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Storied Bear (legendary) + Legendary Rock (legendary *and* an artifact) = two qualifying
        // permanents. Summing the categories instead of the permanents would read three and wrongly
        // grant the designation.
        d.putCreatureOnBattlefield(active, "Storied Bear")
        d.putPermanentOnBattlefield(active, "Legendary Rock")

        EnduringStoryService.has(d.state, active) shouldBe false

        // One more qualifying permanent of any kind tips it over.
        d.putPermanentOnBattlefield(active, "Plain Rock")
        EnduringStoryService.has(d.state, active) shouldBe true
    }

    test("a Saga counts toward the threshold") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Storied Bear")
        d.putCreatureOnBattlefield(active, "Plain Legend")
        EnduringStoryService.has(d.state, active) shouldBe false

        d.putPermanentOnBattlefield(active, "Plain Saga")
        EnduringStoryService.has(d.state, active) shouldBe true
    }

    test("the designation survives dropping back below three qualifying permanents") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Storied Bear")
        val legend = d.putCreatureOnBattlefield(active, "Plain Legend")
        val rock = d.putPermanentOnBattlefield(active, "Plain Rock")

        pollSbas(d)
        hasMarker(d, active) shouldBe true

        // CR 702.195a — "for the rest of the game". Losing two of the three, so that the live count
        // is back to one, must not take the designation away.
        d.moveToGraveyard(legend)
        d.moveToGraveyard(rock)
        EnduringStoryService.qualifiesViaStoried(d.state, active) shouldBe false
        EnduringStoryService.has(d.state, active) shouldBe true
    }

    test("the designation survives losing the storied permanent itself") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = d.putCreatureOnBattlefield(active, "Storied Bear")
        d.putCreatureOnBattlefield(active, "Plain Legend")
        d.putPermanentOnBattlefield(active, "Plain Rock")

        pollSbas(d)
        hasMarker(d, active) shouldBe true

        d.moveToGraveyard(bear)
        EnduringStoryService.qualifiesViaStoried(d.state, active) shouldBe false
        EnduringStoryService.has(d.state, active) shouldBe true
    }

    test("the storied permanent turns on later, when the third qualifying permanent arrives") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The permanent lands on an empty board — an enters-the-battlefield trigger would sample the
        // count here, find one, and never look again.
        d.putCreatureOnBattlefield(active, "Storied Bear")
        pollSbas(d)
        hasMarker(d, active) shouldBe false

        d.putCreatureOnBattlefield(active, "Plain Legend")
        d.putPermanentOnBattlefield(active, "Plain Rock")
        pollSbas(d)
        hasMarker(d, active) shouldBe true
    }

    test("one player's enduring story is their own — the opponent gets nothing from it") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Storied Bear")
        d.putCreatureOnBattlefield(active, "Plain Legend")
        d.putPermanentOnBattlefield(active, "Plain Rock")

        // The opponent has three qualifying permanents of their own but no storied permanent, which
        // is the half of CR 702.195a that is easy to drop when the count is read per-player.
        d.putCreatureOnBattlefield(opponent, "Plain Legend")
        d.putPermanentOnBattlefield(opponent, "Plain Rock")
        d.putPermanentOnBattlefield(opponent, "Plain Saga")

        pollSbas(d)
        hasMarker(d, active) shouldBe true
        hasMarker(d, opponent) shouldBe false
    }
})
