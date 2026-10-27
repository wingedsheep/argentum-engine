package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LibrarySearchedEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LibrarySearchProhibition
import com.wingedsheep.sdk.scripting.PlayersCantSearchLibraries
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [PlayersCantSearchLibraries] — a static prohibition on searching libraries (CR 701.23).
 *
 *  - `OWN_LIBRARY_BY_OWN_EFFECTS` (Ashiok, Dream Render): an affected player's own spells and
 *    abilities can't make them search their own library; other players' spells still can, and
 *    their own spells may still make them search another library (Ashiok's first ruling).
 *  - `ANY_LIBRARY` (Stranglehold): no search of any library.
 *
 * Either way a forbidden search is no search: no searched event, a "you may search" can't be chosen
 * so it doesn't shuffle, a mandatory "search … then shuffle" still shuffles (Ashiok's rulings). A
 * source that loses its abilities forbids nothing.
 *
 * The active player ("me") casts every probe spell; the lock sits on whichever side the case needs.
 */
class PlayersCantSearchLibrariesTest : FunSpec({
    val ownLock = card("Own Search Lock") { typeLine = "Artifact"
        staticAbility { ability = PlayersCantSearchLibraries(Player.EachOpponent, LibrarySearchProhibition.OWN_LIBRARY_BY_OWN_EFFECTS) } }
    val anyLock = card("Any Search Lock") { typeLine = "Artifact"
        staticAbility { ability = PlayersCantSearchLibraries(Player.EachOpponent) } }
    val mandatoryTutor = card("Mandatory Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibrary(GameObjectFilter.Any) } }
    val optionalTutor = card("Optional Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.May(Patterns.Library.searchLibrary(GameObjectFilter.Any)) } }
    val everyoneTutors = card("Everyone Tutors") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.eachPlayerSearchesLibrary(GameObjectFilter.Any, DynamicAmount.Fixed(1)) } }
    // "Search each opponent's library and exile every Island in it."
    val raidLibraries = card("Raid Libraries") { manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = CompositeEffect(listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.EachOpponent, GameObjectFilter.Any.named("Island")),
                    storeAs = "raided",
                    search = true,
                ),
                MoveCollectionEffect(from = "raided", destination = CardDestination.ToZone(Zone.EXILE)),
                EmitLibrarySearchedEventEffect,
            ))
        } }
    // "You may search your graveyard and/or library for a card, put it into your hand."
    val optionalWideTutor = card("Optional Wide Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.May(Patterns.Library.searchMultipleZones(listOf(Zone.GRAVEYARD, Zone.LIBRARY), destination = SearchDestination.HAND)) } }
    val mandatoryWideTutor = card("Mandatory Wide Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchMultipleZones(listOf(Zone.GRAVEYARD, Zone.LIBRARY), destination = SearchDestination.HAND) } }
    val mute = card("Mute Lock") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) } }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ownLock, anyLock, mandatoryTutor, optionalTutor, everyoneTutors, raidLibraries,
            optionalWideTutor, mandatoryWideTutor, mute))
        initMirrorMatch(Deck.of("Island" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun castSpellAndResolve(d: GameTestDriver, p: EntityId, name: String) {
        d.castSpell(p, d.putCardInHand(p, name)).error shouldBe null
        d.bothPass()
    }
    fun GameTestDriver.cast(p: EntityId, name: String, targets: List<EntityId> = emptyList()): List<GameEvent> {
        castSpell(p, putCardInHand(p, name), targets).error shouldBe null
        return bothPass().events
    }

    test("own scope: an opponent's mandatory search of their own library finds nothing, still shuffles, isn't a search") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Own Search Lock")
        val handBefore = d.getHandSize(me)
        val events = d.cast(me, "Mandatory Tutor")
        withClue("nothing to choose — the library is out of the search") { d.state.pendingDecision shouldBe null }
        d.getHandSize(me) shouldBe handBefore
        withClue("\"search … then shuffle\" still shuffles") { events.any { it is LibraryShuffledEvent } shouldBe true }
        withClue("no search happened") { events.any { it is LibrarySearchedEvent } shouldBe false }
    }

    test("own scope: an opponent's optional search can't be chosen, so it doesn't shuffle") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Own Search Lock")
        val events = d.cast(me, "Optional Tutor")
        withClue("no \"search?\" prompt") { d.state.pendingDecision shouldBe null }
        events.any { it is LibraryShuffledEvent } shouldBe false
    }

    test("own scope: the lock's controller still searches") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Own Search Lock")
        d.cast(me, "Mandatory Tutor")
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("own scope: an affected player's own spell may still make them search another player's library") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Own Search Lock")
        val events = d.cast(me, "Raid Libraries")
        withClue("every Island in the lock controller's library is exiled") {
            d.state.getLibrary(opp).size shouldBe 0
        }
        events.any { it is LibrarySearchedEvent } shouldBe true
    }

    test("own scope: a spell the lock's controller casts may make an opponent search their own library") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Own Search Lock")
        d.cast(me, "Everyone Tutors")
        // Each player searches in turn order: me first, then the opponent — caused by my spell.
        val mine = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        mine.playerId shouldBe me
        d.submitCardSelection(me, emptyList()).error shouldBe null
        val theirs = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the opponent searches, because the spell isn't theirs") { theirs.playerId shouldBe opp }
    }

    test("own scope: an affected player's per-player search of their own library is still forbidden") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Own Search Lock")
        d.cast(me, "Everyone Tutors")
        val only = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("only the lock's controller searches — my own spell can't make me search") { only.playerId shouldBe opp }
        d.submitCardSelection(opp, emptyList()).error shouldBe null
        withClue("and I get no search of my own afterwards") { d.state.pendingDecision shouldBe null }
    }

    for (name in listOf("Optional Wide Tutor", "Mandatory Wide Tutor")) {
        test("own scope: $name still searches the graveyard half of \"graveyard and/or library\"") {
            val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
            d.putPermanentOnBattlefield(opp, "Own Search Lock")
            val inGraveyard = d.putCardInGraveyard(me, "Island")
            castSpellAndResolve(d, me, name)
            if (d.state.pendingDecision is com.wingedsheep.engine.core.YesNoDecision) d.submitYesNo(me, true).error shouldBe null
            val choice = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the graveyard card is findable; the library is out of the search") {
                choice.options shouldBe listOf(inGraveyard)
            }
        }
    }

    test("any scope: an affected player can't search any library") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Any Search Lock")
        val events = d.cast(me, "Raid Libraries")
        d.state.getLibrary(opp).isNotEmpty() shouldBe true
        events.any { it is LibrarySearchedEvent } shouldBe false
        d.cast(me, "Mandatory Tutor")
        d.state.pendingDecision shouldBe null
    }

    test("a lock that has lost its abilities forbids nothing") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val lock = d.putPermanentOnBattlefield(opp, "Own Search Lock")
        d.cast(me, "Mute Lock", listOf(lock))
        d.cast(me, "Mandatory Tutor")
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }
})
