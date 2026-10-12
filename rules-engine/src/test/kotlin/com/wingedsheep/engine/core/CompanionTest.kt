package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CompanionComponent
import com.wingedsheep.engine.state.components.player.companionOutsideGame
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardEntry
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain as stringShouldContain

/**
 * The companion mechanic in play (CR 702.139, 103.2b, 116.2g).
 *
 * Whether a deck may reveal a companion is deck construction's question (the server's validator
 * asks `CompanionEligibility`); this pins what the engine owes a revealed one:
 *  - it begins the game outside the game — in its owner's sideboard — revealed (CR 103.2b), so
 *    every player and spectator sees it while the rest of that sideboard stays private;
 *  - once during the game, any time its owner has priority during a main phase of their turn with
 *    the stack empty, they may pay {3} to put it into their hand (CR 702.139a, 116.2g) — a special
 *    action, so they keep priority (CR 117.3c);
 *  - once in hand it is an ordinary card that stays in the game (CR 702.139c), and the action is
 *    gone for good, even should the card somehow return outside the game.
 */
class CompanionTest : FunSpec({

    val companion = CardDefinition.creature(
        name = "Test Companion Cat",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Cat")),
        power = 3,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY),
        oracleText = "Companion — Each card in your starting deck is a card.",
    ).copy(keywordAbilities = listOf(KeywordAbility.companion(CompanionCondition.EachCard(GameObjectFilter.Any))))

    val wishTarget = CardDefinition.creature(
        name = "Test Sideboard Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
    )

    val companionDeck = Deck(
        cards = List(20) { "Forest" } + List(20) { "Grizzly Bears" },
        sideboard = listOf(CardEntry(wishTarget.name), CardEntry(companion.name)),
        companion = companion.name,
    )
    val plainDeck = Deck(cards = List(20) { "Forest" } + List(20) { "Grizzly Bears" })

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(companion, wishTarget))
    }

    /** Player 1 revealed [companion]; player 2 has none. Player 1 starts, in their first main phase. */
    fun companionGame(): GameTestDriver = newDriver().apply {
        initGame(companionDeck, plainDeck)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.companionId(): EntityId =
        state.getEntity(player1)!!.get<CompanionComponent>()!!.companionId

    fun GameTestDriver.companionActions(playerId: EntityId) =
        legalActions(playerId).filter { it.actionType == "PutCompanionIntoHand" }

    fun GameTestDriver.giveLands(playerId: EntityId, count: Int) =
        repeat(count) { putLandOnBattlefield(playerId, "Forest") }

    test("the revealed companion begins outside the game, in its owner's sideboard (CR 103.2b)") {
        val driver = companionGame()
        val companionId = driver.companionId()

        val sideboard = driver.state.getZone(ZoneKey(driver.player1, Zone.SIDEBOARD))
        sideboard shouldContain companionId
        driver.state.getEntity(companionId)!!.get<CardComponent>()!!.name shouldBe companion.name
        driver.state.getHand(driver.player1) shouldNotContain companionId
        driver.state.getLibrary(driver.player1) shouldNotContain companionId
        driver.state.getEntity(driver.player2)!!.get<CompanionComponent>().shouldBeNull()
    }

    test("a commander deck may reveal a companion too — it isn't one of the 100 (CR 702.139d)") {
        val driver = newDriver()
        val players = driver.initMultiplayer(
            decks = listOf(
                Deck(
                    cards = List(99) { "Forest" },
                    sideboard = listOf(CardEntry(companion.name)),
                    companion = companion.name,
                ),
                Deck(cards = List(99) { "Forest" }),
            ),
            format = Format.Commander(),
            commanders = listOf("Grizzly Bears", "Grizzly Bears"),
        )
        val companionId = driver.state.getEntity(players[0])!!.get<CompanionComponent>()!!.companionId
        driver.state.getZone(ZoneKey(players[0], Zone.SIDEBOARD)) shouldBe listOf(companionId)
        driver.state.getLibrary(players[0]) shouldHaveSize 99 - 7
    }

    test("setup refuses a companion that isn't on the sideboard or has no companion ability") {
        shouldThrow<IllegalArgumentException> {
            newDriver().initGame(companionDeck.copy(sideboard = listOf(CardEntry(wishTarget.name))), plainDeck)
        }.message!!.stringShouldContain("isn't on their sideboard")
        shouldThrow<IllegalArgumentException> {
            newDriver().initGame(companionDeck.copy(companion = wishTarget.name), plainDeck)
        }.message!!.stringShouldContain("has no companion ability")
    }

    test("the companion is public: opponents and spectators see it, but not the rest of the sideboard") {
        val driver = companionGame()
        val companionId = driver.companionId()
        val view = ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = driver.cardRegistry))

        for ((viewer, spectator) in listOf(driver.player2 to false, driver.player2 to true)) {
            val state = view.transform(driver.state, viewer, isSpectator = spectator)
            state.players.single { it.playerId == driver.player1 }.companionId shouldBe companionId
            state.cards[companionId].shouldNotBeNull().name shouldBe companion.name
            val sideboard = state.zones.single { it.zoneId == ZoneKey(driver.player1, Zone.SIDEBOARD) }
            sideboard.cardIds shouldBe listOf(companionId)
            sideboard.size shouldBe 2
        }
        // The owner still sees their whole sideboard.
        val own = view.transform(driver.state, driver.player1)
        own.zones.single { it.zoneId == ZoneKey(driver.player1, Zone.SIDEBOARD) }.cardIds shouldHaveSize 2
    }

    test("the {3} special action is offered in your main phase with an empty stack, unaffordable without mana") {
        val driver = companionGame()
        val companionId = driver.companionId()

        val unaffordable = driver.companionActions(driver.player1).single()
        unaffordable.affordable shouldBe false
        (unaffordable.action as PutCompanionIntoHand).cardId shouldBe companionId

        driver.giveLands(driver.player1, 3)
        val offered = driver.companionActions(driver.player1).single()
        offered.affordable shouldBe true
        offered.manaCostString shouldBe "{3}"

        // Never to the player without a companion, and never on the other player's priority.
        driver.companionActions(driver.player2).shouldBeEmpty()
    }

    test("not outside your main phase, not on an opponent's turn, not with a spell on the stack (CR 116.2g)") {
        val driver = companionGame()
        driver.giveLands(driver.player1, 3)
        val companionId = driver.companionId()

        // A spell on the stack.
        driver.giveLands(driver.player1, 2)
        val bears = driver.putCardInHand(driver.player1, "Grizzly Bears")
        driver.castSpell(driver.player1, bears)
        driver.state.stack.isEmpty() shouldBe false
        driver.companionActions(driver.player1).shouldBeEmpty()
        driver.submit(PutCompanionIntoHand(driver.player1, companionId)).error.shouldNotBeNull()
        driver.bothPass()

        // Combat isn't a main phase.
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.companionActions(driver.player1).shouldBeEmpty()
        driver.submit(PutCompanionIntoHand(driver.player1, companionId)).error.shouldNotBeNull()

        // The opponent's turn — even in its main phase.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.player2)
        driver.passPriority(driver.player2)
        driver.priorityPlayer shouldBe driver.player1
        driver.companionActions(driver.player1).shouldBeEmpty()
    }

    test("paying {3} puts the companion into hand; the player keeps priority (CR 702.139a, 117.3c)") {
        val driver = companionGame()
        driver.giveLands(driver.player1, 3)
        val companionId = driver.companionId()

        val result = driver.submitSuccess(PutCompanionIntoHand(driver.player1, companionId))

        driver.state.getHand(driver.player1) shouldContain companionId
        driver.state.getZone(ZoneKey(driver.player1, Zone.SIDEBOARD)) shouldNotContain companionId
        driver.getUntappedLands(driver.player1).shouldBeEmpty()
        driver.state.stack.isEmpty() shouldBe true
        driver.priorityPlayer shouldBe driver.player1
        result.events.filterIsInstance<CompanionPutIntoHandEvent>().single().cardName shouldBe companion.name
        result.events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == companionId }.let {
            it.fromZone shouldBe Zone.SIDEBOARD
            it.toZone shouldBe Zone.HAND
        }
        driver.state.getEntity(driver.player1)!!.get<CompanionComponent>()!!.putIntoHand shouldBe true

        // In hand it is an ordinary card (CR 702.139c) — and no longer shown as a companion.
        val view = ClientStateTransformer(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = driver.cardRegistry))
        view.transform(driver.state, driver.player2).players.single { it.playerId == driver.player1 }.companionId.shouldBeNull()
        view.transform(driver.state, driver.player2).cards[companionId].shouldBeNull()
    }

    test("mana already floating pays first") {
        val driver = companionGame()
        driver.giveColorlessMana(driver.player1, 3)
        driver.giveLands(driver.player1, 3)

        driver.submitSuccess(PutCompanionIntoHand(driver.player1, driver.companionId()))

        driver.getUntappedLands(driver.player1) shouldHaveSize 3
    }

    test("once per game: never offered again, even if the card returns outside the game (CR 116.2g)") {
        val driver = companionGame()
        driver.giveLands(driver.player1, 6)
        val companionId = driver.companionId()
        driver.submitSuccess(PutCompanionIntoHand(driver.player1, companionId))

        driver.companionActions(driver.player1).shouldBeEmpty()

        // Back to the sideboard (as if wished away and returned): still no second action.
        driver.replaceState(
            driver.state
                .removeFromZone(ZoneKey(driver.player1, Zone.HAND), companionId)
                .addToZone(ZoneKey(driver.player1, Zone.SIDEBOARD), companionId)
        )
        driver.companionActions(driver.player1).shouldBeEmpty()
        driver.submit(PutCompanionIntoHand(driver.player1, companionId)).error.shouldNotBeNull()
    }

    test("only the revealed companion — not another sideboard card") {
        val driver = companionGame()
        driver.giveLands(driver.player1, 3)
        val otherSideboardCard = driver.state.getZone(ZoneKey(driver.player1, Zone.SIDEBOARD))
            .single { it != driver.companionId() }

        driver.submit(PutCompanionIntoHand(driver.player1, otherSideboardCard)).error.shouldNotBeNull()
        driver.state.getZone(ZoneKey(driver.player1, Zone.SIDEBOARD)) shouldContain otherSideboardCard
    }

    test("a restarted game keeps a waiting companion outside the game, revealed — not one already taken") {
        val waiting = companionGame()
        val companionId = waiting.companionId()
        val restarted = GameRestarter(waiting.cardRegistry).restart(waiting.state, GameRestartRequest(waiting.player1))
        restarted.newState.companionOutsideGame(waiting.player1) shouldBe companionId

        val taken = companionGame()
        taken.giveLands(taken.player1, 3)
        taken.submitSuccess(PutCompanionIntoHand(taken.player1, taken.companionId()))
        val afterTaking = GameRestarter(taken.cardRegistry).restart(taken.state, GameRestartRequest(taken.player1))
        afterTaking.newState.getEntity(taken.player1)!!.get<CompanionComponent>().shouldBeNull()
    }

    test("the action round-trips through serialization (replays record it)") {
        val engineJson = kotlinx.serialization.json.Json { serializersModule = engineSerializersModule }
        val action: GameAction = PutCompanionIntoHand(EntityId("p1"), EntityId("c1"))
        val json = engineJson.encodeToString(GameAction.serializer(), action)
        engineJson.decodeFromString(GameAction.serializer(), json) shouldBe action
    }
})
