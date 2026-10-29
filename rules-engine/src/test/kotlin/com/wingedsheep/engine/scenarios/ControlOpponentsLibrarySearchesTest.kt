package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LibrarySearchedEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ControlOpponentsLibrarySearches
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [ControlOpponentsLibrarySearches] (Opposition Agent): while an opponent of the source's controller
 * searches their own library, the controller makes the search's pick, each card found is exiled
 * instead of going where the instruction says, and the controller may play it for as long as it
 * stays exiled, spending mana as though it were mana of any color. The rest of the instruction (the
 * shuffle, the searched event) still happens. Rulings covered: searching multiple zones exiles what
 * is found in any of them; the latest-entered source wins when several apply; a source without its
 * abilities does nothing; the source's controller's own searches, and searches of someone else's
 * library, are untouched.
 */
class ControlOpponentsLibrarySearchesTest : FunSpec({
    val agent = card("Test Agent") { typeLine = "Artifact"
        staticAbility { ability = ControlOpponentsLibrarySearches } }
    val tutor = card("Test Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibrary(GameObjectFilter.Any) } }
    val landTutor = card("Test Land Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchLibrary(GameObjectFilter.Land, destination = SearchDestination.BATTLEFIELD) } }
    val wideTutor = card("Test Wide Tutor") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.searchMultipleZones(listOf(Zone.GRAVEYARD, Zone.LIBRARY), destination = SearchDestination.HAND) } }
    val everyoneTutors = card("Test Everyone Tutors") { manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Patterns.Library.eachPlayerSearchesLibrary(GameObjectFilter.Any, DynamicAmount.Fixed(1)) } }
    // "Search target opponent's library for a card and put that card into your hand."
    val pilfer = card("Test Pilfer") { manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            target(Targets.Player)
            effect = CompositeEffect(listOf(
                GatherCardsEffect(CardSource.FromZone(Zone.LIBRARY, Player.TargetPlayer, GameObjectFilter.Any), storeAs = "searchable", search = true),
                SelectFromCollectionEffect(from = "searchable", selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)), storeSelected = "found"),
                MoveCollectionEffect(from = "found", destination = CardDestination.ToZone(Zone.HAND)),
                EmitLibrarySearchedEventEffect,
            ))
        } }
    // Doomsday's shape: "Search your library for a card and exile the rest. Put that card on top."
    val split = card("Test Split") { manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = CompositeEffect(listOf(
                GatherCardsEffect(CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any), storeAs = "searchable", search = true),
                SelectFromCollectionEffect(from = "searchable", selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "found", storeRemainder = "rest"),
                MoveCollectionEffect(from = "rest", destination = CardDestination.ToZone(Zone.EXILE)),
                MoveCollectionEffect(from = "found", destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)),
                EmitLibrarySearchedEventEffect,
            ))
        } }
    // Long-Term Plans' shape: the found card is moved twice, and other library cards are moved after it.
    val plans = card("Test Plans") { manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = CompositeEffect(listOf(
                GatherCardsEffect(CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any), storeAs = "searchable", search = true),
                SelectFromCollectionEffect(from = "searchable", selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)), storeSelected = "found"),
                MoveCollectionEffect(from = "found", destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)),
                GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(2)), storeAs = "above"),
                MoveCollectionEffect(from = "found", destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)),
                MoveCollectionEffect(from = "above", destination = CardDestination.ToZone(Zone.GRAVEYARD)),
                EmitLibrarySearchedEventEffect,
            ))
        } }
    val mute = card("Test Mute") { manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t) } }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(agent, tutor, landTutor, wideTutor, everyoneTutors, pilfer, split, plans, mute))
        initMirrorMatch(Deck.of("Island" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.cast(p: EntityId, name: String, targets: List<EntityId> = emptyList()) =
        castSpell(p, putCardInHand(p, name), targets).also { it.error shouldBe null }.let { bothPass().events }

    test("the source's controller picks an opponent's search, and the found card is exiled, not put into hand") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        val bolt = d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val handBefore = d.getHandSize(me)
        d.cast(me, "Test Tutor")

        val pick = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the agent's controller makes the search's pick") { pick.playerId shouldBe opp }
        pick.prompt shouldContain "their library"
        val events = d.submitCardSelection(opp, listOf(bolt)).also { it.error shouldBe null }.events

        d.getHandSize(me) shouldBe handBefore
        d.getExile(me) shouldContain bolt
        withClue("the rest of the instruction still happens") {
            events.any { it is LibraryShuffledEvent } shouldBe true
            events.any { it is LibrarySearchedEvent && it.playerId == me } shouldBe true
        }
    }

    test("the source's controller may cast the exiled card, spending mana as though it were any color") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        val bolt = d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        d.cast(me, "Test Tutor")
        d.submitCardSelection(opp, listOf(bolt)).error shouldBe null

        withClue("the searcher may not play it") {
            d.legalActions(me).mapNotNull { (it.action as? CastSpell)?.cardId } shouldNotContain bolt
        }
        d.passPriority(me)
        d.giveMana(opp, Color.BLACK, 1)
        d.submit(CastSpell(opp, bolt, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(me)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(me) shouldBe 17
        d.getGraveyard(me) shouldContain bolt
    }

    test("a land found for the battlefield is exiled instead, and the source's controller may play it") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        d.cast(me, "Test Land Tutor")
        val pick = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe opp
        val island = pick.options.first()
        val battlefieldBefore = d.state.getBattlefield().size
        d.submitCardSelection(opp, listOf(island)).error shouldBe null
        d.state.getBattlefield().size shouldBe battlefieldBefore
        d.getExile(me) shouldContain island
        d.state.mayPlayPermissions.any { it.controllerId == opp && island in it.cardIds && it.withAnyManaColor } shouldBe true
    }

    test("the source's controller may have a search for a quality find nothing") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        d.cast(me, "Test Land Tutor")
        d.submitCardSelection(opp, emptyList()).error shouldBe null
        d.getExile(me).size shouldBe 0
    }

    test("a search of several zones exiles what is found in any of them") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        val inGraveyard = d.putCardInGraveyard(me, "Lightning Bolt")
        d.cast(me, "Test Wide Tutor")
        val pick = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe opp
        d.submitCardSelection(opp, listOf(inGraveyard)).error shouldBe null
        d.getExile(me) shouldContain inGraveyard
    }

    test("only the picked card is found: the unpicked rest moves as written, with no permission") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        val bolt = d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val libraryBefore = d.state.getLibrary(me).size
        d.cast(me, "Test Split")
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe opp
        d.submitCardSelection(opp, listOf(bolt)).error shouldBe null

        d.state.getLibrary(me).size shouldBe 0
        d.getExile(me).size shouldBe libraryBefore
        withClue("only the found card may be played by the agent's controller") {
            d.state.mayPlayPermissions.flatMap { it.cardIds }.toSet() shouldBe setOf(bolt)
        }
    }

    test("a found card stays in exile when a later step moves it again, and other library cards aren't found") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        val bolt = d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val libraryBefore = d.state.getLibrary(me).size
        d.cast(me, "Test Plans")
        d.submitCardSelection(opp, listOf(bolt)).error shouldBe null

        d.getExile(me) shouldBe listOf(bolt)
        withClue("the two cards from the top went to the graveyard as written") {
            d.getGraveyard(me).size shouldBe 2 + 1 // + the resolved sorcery
            d.state.getLibrary(me).size shouldBe libraryBefore - 3
        }
        d.state.mayPlayPermissions.flatMap { it.cardIds }.toSet() shouldBe setOf(bolt)
    }

    test("the source's controller's own search is untouched") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, "Test Agent")
        val bolt = d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        d.cast(me, "Test Tutor")
        val pick = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe me
        d.submitCardSelection(me, listOf(bolt)).error shouldBe null
        d.getHand(me) shouldContain bolt
    }

    test("an opponent searching someone else's library isn't controlled") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Test Agent")
        val theirBolt = d.putCardOnTopOfLibrary(opp, "Lightning Bolt")
        d.cast(me, "Test Pilfer", listOf(opp))
        val pick = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe me
        d.submitCardSelection(me, listOf(theirBolt)).error shouldBe null
        withClue("found normally, not exiled") { d.state.logicalZone(theirBolt)?.zoneType shouldBe Zone.HAND }
    }

    test("the source's controller's spell making an opponent search is controlled too") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Test Agent")
        val theirBolt = d.putCardOnTopOfLibrary(opp, "Lightning Bolt")
        d.cast(me, "Test Everyone Tutors")
        // Each player searches in turn: my own search first, uncontrolled.
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe me
        d.submitCardSelection(me, emptyList()).error shouldBe null
        val theirs = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("I pick for the opponent") { theirs.playerId shouldBe me }
        d.submitCardSelection(me, listOf(theirBolt)).error shouldBe null
        d.getExile(opp) shouldContain theirBolt
        d.getHand(opp) shouldNotContain theirBolt
    }

    test("a source that has lost its abilities controls nothing") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val agentId = d.putPermanentOnBattlefield(opp, "Test Agent")
        d.cast(me, "Test Mute", listOf(agentId))
        val bolt = d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        d.cast(me, "Test Tutor")
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe me
        d.submitCardSelection(me, listOf(bolt)).error shouldBe null
        d.getHand(me) shouldContain bolt
    }

    test("with several sources, the controller of the one that entered most recently takes the search") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(agent, tutor))
            initMultiplayer(List(3) { Deck.of("Island" to 40) }); passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val (me, second, third) = d.state.turnOrder
        d.putPermanentOnBattlefield(second, "Test Agent")
        d.putPermanentOnBattlefield(third, "Test Agent")
        d.castSpell(me, d.putCardInHand(me, "Test Tutor")).error shouldBe null
        repeat(3) { if (d.state.pendingDecision == null) d.passPriority(d.state.priorityPlayerId!!) }
        d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe third
    }
})
