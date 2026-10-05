package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameRestartedEvent
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Restarting the game (CR 727) — Karn Liberated's −14, here on an artifact so the test needs no
 * loyalty: "{0}: Exile target permanent." and "{0}: Restart the game, leaving in exile all non-Aura
 * permanent cards exiled with this artifact. Then put those cards onto the battlefield under your
 * control."
 */
class RestartGameTest : FunSpec({

    val restarter = card("Test Restarter") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = "{0}: Exile target permanent.\n" +
            "{0}: Restart the game, leaving in exile all non-Aura permanent cards exiled with this " +
            "artifact. Then put those cards onto the battlefield under your control."
        activatedAbility {
            cost = Costs.Free
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.ExileLinkedToSource(permanent)
        }
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Pipeline {
                val exiled = gather(CardSource.FromLinkedExile())
                val kept = filter(exiled, GameObjectFilter.Permanent.notSubtype(Subtype.AURA))
                run(Effects.RestartGame(
                    exempt = kept,
                    afterRestart = Effects.Pipeline {
                        move(kept, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                    }
                ))
            }
        }
    }

    val arrival = card("Test Arrival") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Elf"
        power = 2
        toughness = 2
        oracleText = "When this creature enters, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
    }

    val aura = card("Test Restart Aura") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    val exileAbility = restarter.activatedAbilities[0].id
    val restartAbility = restarter.activatedAbilities[1].id

    val deckSize = 40

    /**
     * The driver sets games up without a mulligan phase; mark it as one that had one, as a game
     * between people does, so the restarted game has mulligans too.
     */
    fun GameTestDriver.withMulliganPhase() {
        for (player in listOf(player1, player2)) {
            addComponent(player, state.getEntity(player)!!.get<MulliganStateComponent>()!!.copy(skipped = false))
        }
    }

    fun driver(playerTwoDeck: Int = deckSize, mulligans: Boolean = true): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(restarter, arrival, aura))
        initGame(Deck.of("Island" to deckSize), Deck.of("Forest" to playerTwoDeck))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (mulligans) withMulliganPhase()
    }

    fun GameTestDriver.exileWith(source: EntityId, controller: EntityId, permanent: EntityId) {
        submit(ActivateAbility(controller, source, exileAbility, targets = listOf(ChosenTarget.Permanent(permanent))))
            .outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.restart(source: EntityId, controller: EntityId) {
        submit(ActivateAbility(controller, source, restartAbility)).outcome shouldBe Outcome.Done
        // Both players pass: the ability resolves and the game restarts.
        passPriority(priorityPlayer!!)
        passPriority(priorityPlayer!!)
    }

    fun GameTestDriver.keepBoth() {
        submit(KeepHand(player1)).outcome shouldBe Outcome.Done
        submit(KeepHand(player2)).outcome shouldBe Outcome.Done
    }

    fun GameTestDriver.cardsOwnedBy(playerId: EntityId) = state.entities.filter { (_, c) ->
        c.get<CardComponent>() != null && c.get<OwnerComponent>()?.playerId == playerId
    }.keys

    fun GameTestDriver.namesIn(playerId: EntityId, zone: Zone) =
        state.getZone(ZoneKey(playerId, zone)).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    test("CR 727.1 / 103: the game ends and a new one begins — fresh life, libraries, seven-card hands, mulligans") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val bears = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.addComponent(bears, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        d.putCardInGraveyard(d.player1, "Lightning Bolt")
        d.setLifeTotal(d.player1, 7)
        d.setLifeTotal(d.player2, 3)

        d.restart(source, d.player1)

        val restarted = d.events.filterIsInstance<GameRestartedEvent>()
        restarted.size shouldBe 1
        restarted.single().startingPlayerId shouldBe d.player1

        withClue("No one won, lost or drew (CR 727.1)") {
            d.state.gameOver shouldBe false
            d.state.winnerId shouldBe null
        }
        withClue("Turn 1 of the new game, still before its first turn") {
            d.state.turnNumber shouldBe 1
            d.state.phase shouldBe Phase.BEGINNING
            d.state.step shouldBe Step.UNTAP
            d.state.stack.shouldBeEmpty()
            d.state.continuationStack.shouldBeEmpty()
        }
        withClue("Starting life totals (CR 103.4)") {
            d.getLifeTotal(d.player1) shouldBe 20
            d.getLifeTotal(d.player2) shouldBe 20
        }
        for (player in listOf(d.player1, d.player2)) {
            withClue("Everything is in library or hand: no battlefield, graveyard or exile carries over") {
                d.state.getBattlefield().shouldBeEmpty()
                d.getGraveyard(player).shouldBeEmpty()
                d.getExile(player).shouldBeEmpty()
                d.getHandSize(player) shouldBe 7
            }
            withClue("Each player decides on a mulligan again (CR 103.5)") {
                d.state.getEntity(player)?.get<MulliganStateComponent>()?.hasKept shouldBe false
            }
        }
        withClue("The cards put into play by the test are involved in the new game too (CR 727.2)") {
            d.getHandSize(d.player1) + d.state.getLibrary(d.player1).size shouldBe deckSize + 2
            d.getHandSize(d.player2) + d.state.getLibrary(d.player2).size shouldBe deckSize + 1
        }
        withClue("No counters survive (ruling: players and permanents start fresh)") {
            d.state.entities.values.none { it.has<CountersComponent>() } shouldBe true
        }
    }

    test("CR 727.1a: the controller of the restarting ability takes the first turn") {
        val d = driver()
        // Player 2 restarts the game during player 1's turn.
        val source = d.putPermanentOnBattlefield(d.player2, "Test Restarter")
        d.passPriority(d.player1)
        d.submit(ActivateAbility(d.player2, source, restartAbility)).outcome shouldBe Outcome.Done
        d.passPriority(d.player2)
        d.passPriority(d.player1)

        d.state.activePlayerId shouldBe d.player2
        d.state.turnOrder.first() shouldBe d.player2

        d.keepBoth()
        d.state.activePlayerId shouldBe d.player2
        d.state.turnNumber shouldBe 1
        d.state.step shouldBe Step.UPKEEP
    }

    test("CR 727.2: ownership doesn't change — a card goes to its owner's library wherever it was") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        // Player 2's creature, controlled by player 1.
        val stolen = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.addComponent(stolen, ControllerComponent(d.player1))

        d.restart(source, d.player1)

        val p2Cards = d.cardsOwnedBy(d.player2).map { d.getCardName(it) }
        p2Cards.count { it == "Centaur Courser" } shouldBe 1
        (d.namesIn(d.player2, Zone.LIBRARY) + d.namesIn(d.player2, Zone.HAND)).count { it == "Centaur Courser" } shouldBe 1
        (d.namesIn(d.player1, Zone.LIBRARY) + d.namesIn(d.player1, Zone.HAND)) shouldNotContain "Centaur Courser"
    }

    test("tokens are not cards and do not carry over; sideboard cards stay outside the game") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val token = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.addComponent(token, TokenComponent)
        val sideboarded = d.putCardInHand(d.player1, "Giant Growth")
        d.replaceState(d.state.removeFromZone(ZoneKey(d.player1, Zone.HAND), sideboarded)
            .addToZone(ZoneKey(d.player1, Zone.SIDEBOARD), sideboarded))

        d.restart(source, d.player1)

        withClue("The deck plus the restarter: the token is gone") {
            d.getHandSize(d.player1) + d.state.getLibrary(d.player1).size shouldBe deckSize + 1
        }
        d.namesIn(d.player1, Zone.SIDEBOARD) shouldBe listOf("Giant Growth")
    }

    test("every card gets a new id, so an id the old game showed can't name a card in a new hand") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val oldCardIds = d.state.entities.filter { (_, c) -> c.has<CardComponent>() }.keys

        d.restart(source, d.player1)

        val newCardIds = d.state.entities.filter { (_, c) -> c.has<CardComponent>() }.keys
        newCardIds.intersect(oldCardIds).shouldBeEmpty()
    }

    test("CR 727.5 / 727.4: exempt cards wait in exile through mulligans, then enter under the controller's control") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Test Arrival")
        val mine = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.tapPermanent(mine)
        d.exileWith(source, d.player1, theirs)
        d.exileWith(source, d.player1, mine)

        d.restart(source, d.player1)

        withClue("Exempt cards stay in their owners' exile and aren't in any deck (CR 727.5)") {
            d.namesIn(d.player2, Zone.EXILE) shouldBe listOf("Test Arrival")
            d.namesIn(d.player1, Zone.EXILE) shouldBe listOf("Centaur Courser")
            d.getHandSize(d.player2) + d.state.getLibrary(d.player2).size shouldBe deckSize
            d.state.getBattlefield().shouldBeEmpty()
        }

        // A mulligan in the new game works as usual.
        d.submit(TakeMulligan(d.player2)).outcome shouldBe Outcome.Done
        d.keepBoth()
        val bottom = d.state.getHand(d.player2).take(1)
        d.submit(com.wingedsheep.engine.core.BottomCards(d.player2, bottom)).outcome shouldBe Outcome.Done

        val entered = d.state.getBattlefield()
        withClue("Both exempt cards entered the battlefield just before the first turn (CR 727.4)") {
            entered.map { d.getCardName(it) }.shouldContainExactlyInAnyOrder("Test Arrival", "Centaur Courser")
            entered.forEach { d.getController(it) shouldBe d.player1 }
        }
        withClue("…under the controller's control, though ownership is unchanged (CR 727.2)") {
            val arrivalId = entered.single { d.getCardName(it) == "Test Arrival" }
            d.state.getEntity(arrivalId)?.get<OwnerComponent>()?.playerId shouldBe d.player2
        }
        withClue("Ruling: they untap in the first untap step and aren't summoning sick") {
            entered.forEach {
                d.state.getEntity(it)?.has<TappedComponent>() shouldBe false
                d.state.getEntity(it)?.has<SummoningSicknessComponent>() shouldBe false
            }
        }
        withClue("Ruling: their enters triggers go on the stack in the first upkeep") {
            d.state.step shouldBe Step.UPKEEP
            d.state.activePlayerId shouldBe d.player1
            d.stackSize shouldBe 1
            d.bothPass()
            d.getLifeTotal(d.player1) shouldBe 23
        }
    }

    test("CR 727.5: Auras and nonpermanent cards exiled with the source rejoin their owners' decks") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val auraCard = d.putCardInExile(d.player1, "Test Restart Aura")
        val bolt = d.putCardInExile(d.player2, "Lightning Bolt")
        d.addComponent(source, LinkedExileComponent(listOf(auraCard, bolt)))

        d.restart(source, d.player1)

        d.getExile(d.player1).shouldBeEmpty()
        d.getExile(d.player2).shouldBeEmpty()
        (d.namesIn(d.player1, Zone.LIBRARY) + d.namesIn(d.player1, Zone.HAND)).count { it == "Test Restart Aura" } shouldBe 1
        (d.namesIn(d.player2, Zone.LIBRARY) + d.namesIn(d.player2, Zone.HAND)).count { it == "Lightning Bolt" } shouldBe 1

        d.keepBoth()
        d.state.getBattlefield().shouldBeEmpty()
        d.state.step shouldBe Step.UPKEEP
    }

    test("CR 727.3: a player with fewer than seven cards loses at the first upkeep, mulligans or not") {
        val d = driver(playerTwoDeck = 5)
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        d.restart(source, d.player1)

        withClue("Nothing happens before the first turn") {
            d.state.gameOver shouldBe false
            d.getHandSize(d.player2) shouldBe 5
        }
        d.keepBoth()
        d.state.gameOver shouldBe true
        d.state.winnerId shouldBe d.player1
    }

    test("a restart with nothing exempt still reaches the first turn once both players keep") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        d.restart(source, d.player1)
        d.keepBoth()
        d.state.turnNumber shouldBe 1
        d.state.activePlayerId shouldBe d.player1
        d.state.step shouldBe Step.UPKEEP
        d.state.restartFollowUp shouldBe null
        d.state.pendingRestart shouldBe null
    }

    test("nobody has priority while the new game's opening hands are decided") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        d.restart(source, d.player1)

        d.submit(PassPriority(d.player1)).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        d.state.step shouldBe Step.UNTAP

        d.keepBoth()
        d.state.step shouldBe Step.UPKEEP
    }

    test("a game set up without mulligans restarts straight into its first turn") {
        val d = driver(mulligans = false)
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val mine = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.tapPermanent(mine)
        d.exileWith(source, d.player1, mine)

        d.restart(source, d.player1)

        d.state.turnNumber shouldBe 1
        d.state.activePlayerId shouldBe d.player1
        d.state.step shouldBe Step.UPKEEP
        for (player in listOf(d.player1, d.player2)) {
            d.state.getEntity(player)?.get<MulliganStateComponent>()?.hasKept shouldBe true
        }
        val entered = d.state.getBattlefield()
        entered.map { d.getCardName(it) } shouldBe listOf("Centaur Courser")
        d.state.getEntity(entered.single())?.has<TappedComponent>() shouldBe false
    }

    test("CR 727.2: a copy of a card outside the stack is not a card and doesn't join a deck") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val copy = d.putCardInExile(d.player1, "Lightning Bolt")
        d.addComponent(copy, CopyOfComponent("Lightning Bolt", "Lightning Bolt"))

        d.restart(source, d.player1)

        withClue("The deck plus the restarter: the copy is gone") {
            d.getHandSize(d.player1) + d.state.getLibrary(d.player1).size shouldBe deckSize + 1
        }
    }

    test("a commander returns to the command zone with no commander tax from the old game (CR 903.8)") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        val commander = d.putCardInGraveyard(d.player1, "Centaur Courser")
        d.addComponent(commander, CommanderComponent(ownerId = d.player1, castsFromCommandZone = 3))

        d.restart(source, d.player1)

        val inCommandZone = d.state.getZone(ZoneKey(d.player1, Zone.COMMAND))
        d.namesIn(d.player1, Zone.COMMAND) shouldBe listOf("Centaur Courser")
        d.state.getEntity(inCommandZone.single())?.get<CommanderComponent>()?.castsFromCommandZone shouldBe 0
    }

    test("hotseat control set up around the game survives the restart") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Restarter")
        d.addComponent(d.player2, HotseatControlComponent(d.player1))

        d.restart(source, d.player1)

        d.state.actorFor(d.player2) shouldBe d.player1
    }
})
