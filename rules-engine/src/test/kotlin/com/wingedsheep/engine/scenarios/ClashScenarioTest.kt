package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ClashedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine-level tests for the clash keyword action (CR 701.30).
 *
 * Every clause of the rule has a test here:
 *
 *  - **701.30a** — each clashing player reveals their top card and *may* put it on the bottom.
 *    Both halves are covered: declining leaves the card on top, accepting moves it to the bottom,
 *    and each player decides only for their own library.
 *  - **701.30b** — "clash with an opponent" is a *choice* of opponent, forced and promptless with
 *    a single opponent (every test below is two-player, and none of them answers such a prompt).
 *  - **701.30c** — the reveal is simultaneous and the decisions are in APNAP order. Covered by
 *    asserting the active player is asked first on their own turn, and the *opponent* first when
 *    the clash happens during the opponent's turn.
 *  - **701.30d** — a player wins only with a *strictly* greater mana value than every other card
 *    revealed. Win, loss, tie and both empty-library cases are separate tests.
 *
 * Plus the two printed-ruling facts that make clash a two-player event rather than the clasher's
 * private business: the `ClashedEvent` fires for **both** participants, and an opponent can win —
 * and collect their own "whenever you clash and win" payoff from — a clash you initiated.
 */
class ClashScenarioTest : FunSpec({

    // ---------------------------------------------------------------------
    // Substrate cards
    // ---------------------------------------------------------------------

    // Filler with unambiguous, well-separated mana values, so a rigged library top decides the
    // clash without depending on whatever the deck's real cards cost.
    fun filler(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Artifact"
        oracleText = ""
    }
    val Pebble = filler("Clash Pebble", "{0}")
    val Stone = filler("Clash Stone", "{2}")
    val Boulder = filler("Clash Boulder", "{5}")

    // "Clash with an opponent. If you win, you gain 5 life." — the plain gate.
    val ClashForLife = card("Clash For Life") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Clash with an opponent. If you win, you gain 5 life."
        spell { effect = Patterns.Mechanic.clash(Effects.GainLife(5)) }
    }

    // Captivating Glance's shape: "If you win, … Otherwise, …" — both branches exercised.
    val ClashOrElse = card("Clash Or Else") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Clash with an opponent. If you win, you gain 5 life. Otherwise, you lose 3 life."
        spell {
            effect = Patterns.Mechanic.clash(
                ifYouWin = Effects.GainLife(5),
                otherwise = Effects.LoseLife(3, EffectTarget.Controller)
            )
        }
    }

    // "Whenever you clash, put a +1/+1 counter on this." — fires win or lose (Entangling Trap's
    // trigger shape).
    val ClashWatcher = card("Clash Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.clashes()
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // "Whenever you clash and win, put a +1/+1 counter on this." — Sylvan Echoes' trigger shape.
    val ClashWinWatcher = card("Clash Win Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.clashes(true)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }

    // The instant twin of ClashForLife, so a clash can happen on the *opponent's* turn and put the
    // APNAP ordering under real strain (Lash Out is the printed case).
    val ClashInstant = card("Clash Instant") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Clash with an opponent. If you win, you gain 5 life."
        spell { effect = Patterns.Mechanic.clash(Effects.GainLife(5)) }
    }

    // Entangling Trap's shape: the trigger fires either way and only *part* of the effect is gated
    // on the outcome. One counter always, a second one only on a win — so the two failure modes
    // (rider never runs / rider always runs) are told apart by the same number.
    val ClashRiderWatcher = card("Clash Rider Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.clashes()
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
                Effects.If(
                    Conditions.YouWonTheClash,
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                )
        }
    }

    // The same rider on a trigger no clash ever fires, to pin down that "if you won" is false
    // rather than vacuously true when there is no clash outcome to read.
    val NonClashRiderWatcher = card("Non Clash Rider Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Bird"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.If(
                Conditions.YouWonTheClash,
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
        }
    }

    val substrate = listOf(
        Pebble, Stone, Boulder, ClashForLife, ClashOrElse, ClashInstant,
        ClashWatcher, ClashWinWatcher, ClashRiderWatcher, NonClashRiderWatcher
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + substrate)
        return driver
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.libraryTopName(player: EntityId): String? =
        state.getZone(ZoneKey(player, Zone.LIBRARY)).firstOrNull()?.let { getCardName(it) }

    fun GameTestDriver.librarySize(player: EntityId): Int =
        state.getZone(ZoneKey(player, Zone.LIBRARY)).size

    fun GameTestDriver.emptyLibrary(player: EntityId) {
        val key = ZoneKey(player, Zone.LIBRARY)
        replaceState(state.copy(zones = state.zones + (key to emptyList())))
    }

    /**
     * Answer every clash top-or-bottom prompt, keeping each card on top (select nothing), and
     * record the order the prompts arrived in so the APNAP tests can assert on it.
     */
    fun GameTestDriver.answerClashKeepingAll(): List<EntityId> {
        val askedInOrder = mutableListOf<EntityId>()
        repeat(6) {
            val decision = pendingDecision as? SelectCardsDecision ?: return askedInOrder
            askedInOrder += decision.playerId
            submitDecision(decision.playerId, CardsSelectedResponse(decision.id, emptyList()))
        }
        return askedInOrder
    }

    /** Answer every clash prompt by putting the revealed card on the *bottom*. */
    fun GameTestDriver.answerClashBottomingAll() {
        repeat(6) {
            val decision = pendingDecision as? SelectCardsDecision ?: return
            submitDecision(decision.playerId, CardsSelectedResponse(decision.id, decision.options))
        }
    }

    /**
     * Cast a clash spell from [player]'s hand and pass so it starts resolving.
     *
     * The priority nudge is a harness accommodation, not part of the mechanic: `SubmitDecisionHandler`
     * hands priority to whoever answered the *last* decision of a resolution, and a clash always ends
     * on the opponent's top-or-bottom choice — so after one clash resolves the opponent is holding
     * priority and [player] cannot cast anything. Passing it back (the stack is empty, so a single
     * pass is just the baton moving on) puts a second clash in the same test on the same footing as
     * the first.
     */
    fun GameTestDriver.castClash(player: EntityId, cardName: String) {
        if (stackSize == 0 && priorityPlayer != null && priorityPlayer != player) {
            passPriority(priorityPlayer!!)
        }
        val cardId = putCardInHand(player, cardName)
        castSpell(player, cardId)
        bothPass()
    }

    /** Resolve everything left on the stack (clash triggers stack on top of the spell). */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }

    // ---------------------------------------------------------------------
    // CR 701.30d — who wins
    // ---------------------------------------------------------------------

    test("the clasher wins with a strictly greater mana value and the gate's payoff runs") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Boulder")   // MV 5
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")  // MV 0
        val lifeBefore = driver.getLifeTotal(active)

        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        driver.getLifeTotal(active) shouldBe lifeBefore + 5
    }

    test("the clasher loses to a greater mana value and the payoff does not run") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Pebble")    // MV 0
        driver.putCardOnTopOfLibrary(opponent, "Clash Boulder") // MV 5
        val lifeBefore = driver.getLifeTotal(active)

        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        driver.getLifeTotal(active) shouldBe lifeBefore
    }

    test("a tie wins for nobody — CR 701.30d needs a strictly greater mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Same mana value on both sides: "greater than all other cards revealed" fails both ways.
        driver.putCardOnTopOfLibrary(active, "Clash Stone")
        driver.putCardOnTopOfLibrary(opponent, "Clash Stone")

        val before = driver.events.size
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        val clashed = driver.events.drop(before).filterIsInstance<ClashedEvent>()
        clashed.size shouldBe 2
        clashed.none { it.won } shouldBe true
    }

    test("the else branch runs when the clash is lost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Pebble")
        driver.putCardOnTopOfLibrary(opponent, "Clash Boulder")
        val lifeBefore = driver.getLifeTotal(active)

        driver.castClash(active, "Clash Or Else")
        driver.answerClashKeepingAll()

        driver.getLifeTotal(active) shouldBe lifeBefore - 3
    }

    test("an empty library reveals nothing, so that player cannot win but the other still can") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The clasher has nothing to reveal; the opponent reveals a {0}. Even a zero-mana-value
        // card beats revealing nothing at all, so the clasher loses rather than ties.
        driver.emptyLibrary(active)
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")
        val lifeBefore = driver.getLifeTotal(active)

        val before = driver.events.size
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        driver.getLifeTotal(active) shouldBe lifeBefore
        val clashed = driver.events.drop(before).filterIsInstance<ClashedEvent>()
        clashed.single { it.playerId == active }.won shouldBe false
        clashed.single { it.playerId == opponent }.won shouldBe true
    }

    test("the clasher wins against an empty opposing library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Pebble") // MV 0 still beats nothing
        driver.emptyLibrary(opponent)
        val lifeBefore = driver.getLifeTotal(active)

        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        driver.getLifeTotal(active) shouldBe lifeBefore + 5
    }

    // ---------------------------------------------------------------------
    // CR 701.30a — the top-or-bottom choice
    // ---------------------------------------------------------------------

    test("each clashing player may put their own revealed card on the bottom") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")
        val activeLibrarySize = driver.librarySize(active)
        val opponentLibrarySize = driver.librarySize(opponent)

        driver.castClash(active, "Clash For Life")
        driver.answerClashBottomingAll()

        // Both revealed cards left the top; neither left the library.
        driver.libraryTopName(active) shouldBe "Mountain"
        driver.libraryTopName(opponent) shouldBe "Mountain"
        driver.librarySize(active) shouldBe activeLibrarySize
        driver.librarySize(opponent) shouldBe opponentLibrarySize
    }

    test("declining the choice leaves each revealed card on top") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")

        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        driver.libraryTopName(active) shouldBe "Clash Boulder"
        driver.libraryTopName(opponent) shouldBe "Clash Pebble"
    }

    // ---------------------------------------------------------------------
    // CR 701.30c — APNAP decision order
    // ---------------------------------------------------------------------

    test("the two top-or-bottom decisions go to both players, active player first on their turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")

        driver.castClash(active, "Clash For Life")
        val asked = driver.answerClashKeepingAll()

        asked shouldBe listOf(active, opponent)
    }

    test("clashing on the opponent's turn asks the opponent first — APNAP, not clasher-first") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val firstPlayer = driver.activePlayer!!
        val clasher = driver.getOpponent(firstPlayer)

        // The non-active player clashes at instant speed on the active player's turn.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardOnTopOfLibrary(clasher, "Clash Boulder")
        driver.putCardOnTopOfLibrary(firstPlayer, "Clash Pebble")

        val cardId = driver.putCardInHand(clasher, "Clash Instant")
        driver.passPriority(firstPlayer)
        driver.castSpell(clasher, cardId)
        driver.bothPass()
        val asked = driver.answerClashKeepingAll()

        // CR 701.30c defers to APNAP order (CR 101.4), so the *active* player decides first even
        // though the other player is the one clashing.
        asked shouldBe listOf(firstPlayer, clasher)
    }

    // ---------------------------------------------------------------------
    // "Whenever you clash" — both participants clash
    // ---------------------------------------------------------------------

    test("a ClashedEvent fires for both participants, carrying who won") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")

        val before = driver.events.size
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        val clashed = driver.events.drop(before).filterIsInstance<ClashedEvent>()
        clashed.size shouldBe 2
        clashed.single { it.playerId == active }.won shouldBe true
        clashed.single { it.playerId == active }.opponentId shouldBe opponent
        clashed.single { it.playerId == opponent }.won shouldBe false
        clashed.single { it.playerId == opponent }.opponentId shouldBe active
    }

    test("the reveal attributes the opponent's card to its owner, so multiplayer can name them") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")

        val before = driver.events.size
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()

        // Both reveals are the clasher's (the pipeline's controller); only the per-card owner
        // tells the client whose library the second card came from.
        val reveals = driver.events.drop(before).filterIsInstance<CardsRevealedEvent>()
        reveals.size shouldBe 2
        reveals.single { driver.getCardName(it.cardIds.single()) == "Clash Boulder" }
            .cardOwnerIds shouldBe emptyList()
        reveals.single { driver.getCardName(it.cardIds.single()) == "Clash Pebble" }
            .cardOwnerIds shouldBe listOf(opponent)
    }

    test("Whenever-you-clash fires for the clasher whether they win or lose") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Clash Watcher")

        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()
        driver.plusOneCounters(watcher) shouldBe 1

        driver.putCardOnTopOfLibrary(active, "Clash Pebble")
        driver.putCardOnTopOfLibrary(opponent, "Clash Boulder")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()
        driver.plusOneCounters(watcher) shouldBe 2
    }

    test("Whenever-you-clash-and-win fires only on a win") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Clash Win Watcher")

        // Lose: nothing.
        driver.putCardOnTopOfLibrary(active, "Clash Pebble")
        driver.putCardOnTopOfLibrary(opponent, "Clash Boulder")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()
        driver.plusOneCounters(watcher) shouldBe 0

        // Win: one counter.
        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()
        driver.plusOneCounters(watcher) shouldBe 1
    }

    test("the opponent's own clash payoff fires off a clash you initiated, and can win it") {
        // Entangling Trap / Sylvan Echoes ruling: "if you clash because of a spell or ability an
        // opponent controls, the ability will still trigger. Likewise, you can still win the clash
        // even if you weren't the player to initiate it."
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val theirWatcher = driver.putCreatureOnBattlefield(opponent, "Clash Win Watcher")

        // The active player clashes and *loses*; the opponent, who never initiated anything, wins.
        driver.putCardOnTopOfLibrary(active, "Clash Pebble")
        driver.putCardOnTopOfLibrary(opponent, "Clash Boulder")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()

        driver.plusOneCounters(theirWatcher) shouldBe 1
    }

    // ---------------------------------------------------------------------
    // "If you won" as a rider *inside* a "Whenever you clash" trigger
    // ---------------------------------------------------------------------

    test("the if-you-won rider runs on a win and is skipped on a loss") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Clash Rider Watcher")

        // Lose: the ungated half runs, the rider does not.
        driver.putCardOnTopOfLibrary(active, "Clash Pebble")
        driver.putCardOnTopOfLibrary(opponent, "Clash Boulder")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()
        driver.plusOneCounters(watcher) shouldBe 1

        // Win: both halves run, so this clash is worth two.
        driver.putCardOnTopOfLibrary(active, "Clash Boulder")
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()
        driver.plusOneCounters(watcher) shouldBe 3
    }

    test("a tie is not a win for the rider — CR 701.30d wins for nobody") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Clash Rider Watcher")

        driver.putCardOnTopOfLibrary(active, "Clash Stone")
        driver.putCardOnTopOfLibrary(opponent, "Clash Stone")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()

        driver.plusOneCounters(watcher) shouldBe 1
    }

    test("revealing nothing from an empty library is not a win for the rider") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val watcher = driver.putCreatureOnBattlefield(active, "Clash Rider Watcher")

        // The rider's controller has nothing to reveal, so they cannot have the greatest mana
        // value; the trigger still fires, because they still clashed.
        driver.emptyLibrary(active)
        driver.putCardOnTopOfLibrary(opponent, "Clash Pebble")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()

        driver.plusOneCounters(watcher) shouldBe 1
    }

    test("the rider reads the reader's own outcome, not the clasher's") {
        // The clash the *opponent* started, which the rider's controller wins without lifting a
        // finger — the Entangling Trap / Sylvan Echoes ruling applied to the rider rather than the
        // trigger. The clasher's own ClashedEvent says `won = false`; the rider must not see that.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val theirWatcher = driver.putCreatureOnBattlefield(opponent, "Clash Rider Watcher")

        driver.putCardOnTopOfLibrary(active, "Clash Pebble")
        driver.putCardOnTopOfLibrary(opponent, "Clash Boulder")
        driver.castClash(active, "Clash For Life")
        driver.answerClashKeepingAll()
        driver.resolveStack()

        driver.plusOneCounters(theirWatcher) shouldBe 2
    }

    test("the rider is false on a trigger no clash fired") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cardId = driver.putCardInHand(active, "Non Clash Rider Watcher")
        driver.castSpell(active, cardId)
        driver.bothPass()
        driver.resolveStack()

        val watcher = checkNotNull(driver.findPermanent(active, "Non Clash Rider Watcher"))
        driver.plusOneCounters(watcher) shouldBe 0
    }
})
