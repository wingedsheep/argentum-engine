package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LibrarySearchedEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `Patterns.Library.searchLibraryAndOrGraveyard` — "[you may] search your library and/or graveyard
 * for a card named X … If you search your library this way, shuffle."
 *
 *  - "And/or" is a choice of zones: library, graveyard, or both, plus "Don't search" when optional.
 *  - Only a search that includes the library shuffles it and counts as searching a library
 *    (CR 701.23); a graveyard-only search does neither.
 *  - A search may fail to find a card only in a hidden zone (CR 701.23b): a library search can come
 *    back empty, a graveyard-only search takes the match that is there.
 */
class SearchLibraryAndOrGraveyardTest : FunSpec({
    val optionalTutor = card("Optional Bears Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibraryAndOrGraveyard(GameObjectFilter.Any.named("Grizzly Bears"), optional = true) } }
    val mandatoryTutor = card("Mandatory Bears Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibraryAndOrGraveyard(GameObjectFilter.Any.named("Grizzly Bears")) } }

    // "Search your library and/or graveyard for a creature card with mana value 3 or less" — the
    // bound is stored by an earlier pipeline step and read only after the zone choice.
    val storedBoundTutor = card("Stored Bound Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                val bound = storeNumber(DynamicAmount.Fixed(3))
                run(Patterns.Library.searchLibraryAndOrGraveyard(GameObjectFilter.Creature.manaValueAtMostDynamic(bound.amount)))
            }
        } }

    // "Search your library and/or graveyard for a creature card with mana value X or less" (Finale
    // of Devastation's shape): X is read only after the zone choice.
    val xTutor = card("X Tutor") { manaCost = "{X}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibraryAndOrGraveyard(GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmount.XValue)) } }

    class Setup(val d: GameTestDriver, val me: EntityId, val inLibrary: EntityId, val inGraveyard: EntityId)

    fun setup(): Setup {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(optionalTutor, mandatoryTutor, storedBoundTutor, xTutor))
            initMirrorMatch(Deck.of("Island" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        val inLibrary = d.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val inGraveyard = d.putCardInGraveyard(me, "Grizzly Bears")
        d.putCardInGraveyard(me, "Hill Giant")
        return Setup(d, me, inLibrary, inGraveyard)
    }

    /** Casts [name], answers the zone choice with [option], and returns every event so far. */
    fun Setup.castAndChoose(name: String, option: String): MutableList<GameEvent> {
        d.castSpell(me, d.putCardInHand(me, name)).error shouldBe null
        val events = d.bothPass().events.toMutableList()
        val choice = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = choice.options.indexOf(option)
        withClue("option \"$option\" offered in ${choice.options}") { (index >= 0) shouldBe true }
        val result = d.submitDecision(me, OptionChosenResponse(choice.id, index))
        result.error shouldBe null
        events += result.events
        return events
    }

    fun Setup.pick(card: EntityId?, events: MutableList<GameEvent>) {
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        events += d.submitCardSelection(me, listOfNotNull(card)).events
    }

    test("optional: the choice offers library, graveyard, both, and don't search") {
        val s = setup()
        s.d.castSpell(s.me, s.d.putCardInHand(s.me, "Optional Bears Tutor")).error shouldBe null
        s.d.bothPass()
        s.d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>().options shouldBe listOf(
            "Search your library", "Search your graveyard", "Search your library and graveyard", "Don't search"
        )
    }

    test("mandatory: no don't-search option") {
        val s = setup()
        s.d.castSpell(s.me, s.d.putCardInHand(s.me, "Mandatory Bears Tutor")).error shouldBe null
        s.d.bothPass()
        s.d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>().options shouldBe listOf(
            "Search your library", "Search your graveyard", "Search your library and graveyard"
        )
    }

    test("library: only library cards are offered, then the library is shuffled and searched") {
        val s = setup()
        val events = s.castAndChoose("Optional Bears Tutor", "Search your library")
        val search = s.d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.options shouldBe listOf(s.inLibrary)
        s.pick(s.inLibrary, events)
        s.d.getHand(s.me).contains(s.inLibrary) shouldBe true
        s.d.getGraveyard(s.me).contains(s.inGraveyard) shouldBe true
        events.any { it is LibraryShuffledEvent } shouldBe true
        events.any { it is LibrarySearchedEvent } shouldBe true
    }

    test("library: finding nothing is allowed, and the library still shuffles") {
        val s = setup()
        val events = s.castAndChoose("Optional Bears Tutor", "Search your library")
        s.pick(null, events)
        s.d.getHand(s.me).contains(s.inLibrary) shouldBe false
        events.any { it is LibraryShuffledEvent } shouldBe true
        events.any { it is LibrarySearchedEvent } shouldBe true
    }

    test("graveyard: the public match is taken without a prompt; no shuffle, no library search") {
        val s = setup()
        val events = s.castAndChoose("Optional Bears Tutor", "Search your graveyard")
        withClue("nothing to decide — the graveyard's only match must be found") {
            s.d.state.pendingDecision shouldBe null
        }
        s.d.getHand(s.me).contains(s.inGraveyard) shouldBe true
        s.d.state.getLibrary(s.me).first() shouldBe s.inLibrary
        events.any { it is LibraryShuffledEvent } shouldBe false
        events.any { it is LibrarySearchedEvent } shouldBe false
    }

    test("both: cards from both zones are offered, and the library shuffles") {
        val s = setup()
        val events = s.castAndChoose("Optional Bears Tutor", "Search your library and graveyard")
        val search = s.d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.options shouldContainExactlyInAnyOrder listOf(s.inLibrary, s.inGraveyard)
        s.pick(s.inGraveyard, events)
        s.d.getHand(s.me).contains(s.inGraveyard) shouldBe true
        s.d.getHand(s.me).contains(s.inLibrary) shouldBe false
        events.any { it is LibraryShuffledEvent } shouldBe true
        events.any { it is LibrarySearchedEvent } shouldBe true
    }

    test("don't search: nothing moves, nothing shuffles") {
        val s = setup()
        val handBefore = s.d.getHandSize(s.me)
        val events = s.castAndChoose("Optional Bears Tutor", "Don't search")
        s.d.state.pendingDecision shouldBe null
        s.d.getHandSize(s.me) shouldBe handBefore
        s.d.state.getLibrary(s.me).first() shouldBe s.inLibrary
        events.any { it is LibraryShuffledEvent } shouldBe false
        events.any { it is LibrarySearchedEvent } shouldBe false
    }

    test("a number stored earlier in the resolution reaches the search chosen after the zone choice") {
        val s = setup()
        val events = s.castAndChoose("Stored Bound Tutor", "Search your library and graveyard")
        val search = s.d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("Grizzly Bears (MV 2) qualify in both zones; Hill Giant (MV 4) doesn't") {
            search.options shouldContainExactlyInAnyOrder listOf(s.inLibrary, s.inGraveyard)
        }
        s.pick(s.inLibrary, events)
        s.d.getHand(s.me).contains(s.inLibrary) shouldBe true
    }

    test("the spell's X reaches the search chosen after the zone choice") {
        val s = setup()
        s.d.giveColorlessMana(s.me, 2)
        s.d.castXSpell(s.me, s.d.putCardInHand(s.me, "X Tutor"), xValue = 2).error shouldBe null
        val events = s.d.bothPass().events.toMutableList()
        val choice = s.d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        events += s.d.submitDecision(s.me, OptionChosenResponse(choice.id, choice.options.indexOf("Search your library"))).events
        withClue("Grizzly Bears (MV 2) fit X = 2") {
            s.d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(s.inLibrary)
        }
    }
})
