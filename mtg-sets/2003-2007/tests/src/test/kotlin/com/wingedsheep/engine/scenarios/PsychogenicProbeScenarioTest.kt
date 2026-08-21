package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.ShuffleCause
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Fabricate
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MyrMindservant
import com.wingedsheep.mtg.sets.definitions.mrd.cards.PsychogenicProbe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Psychogenic Probe (MRD #231) — "Whenever a spell or ability causes a player to shuffle their
 * library, this artifact deals 2 damage to that player."
 *
 * The card is a one-line trigger over a new event shape, so the tests are all about *what counts
 * as a shuffle*. Three of them come straight out of CR 701.24 and would each pass a naive
 * implementation that fired only on a bare "shuffle your library" effect:
 *
 * - **701.24b** — a tutor's search-then-shuffle leg still shuffles, even though the found cards are
 *   held out of the randomization. Fabricate covers it.
 * - **701.24e** — an empty library is still shuffled, and the 2009 ruling on the card says so
 *   explicitly. This is the one an "only shuffle if there's something to shuffle" short-circuit
 *   in the executor would quietly break.
 * - **701.24f** — the trigger is per *shuffle*, and each Probe is its own ability, so two Probes
 *   watching one shuffle deal 4.
 *
 * The other half is the word **"causes"**: the game rules also shuffle every library while setting
 * the game up (CR 103.2) and whenever a player mulligans (CR 103.5), and neither is a spell or an
 * ability. Those two never reach a battlefield trigger in a real game because nothing is on the
 * battlefield yet — which is exactly why the exclusion has to be asserted on the event's own
 * `cause` tag rather than on a life total.
 */
class PsychogenicProbeScenarioTest : FunSpec({

    val mindservantAbility = MyrMindservant.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PsychogenicProbe + MyrMindservant + Fabricate)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Put a Myr Mindservant under [playerId] that can pay `{2}, {T}` right now. */
    fun GameTestDriver.readyMindservant(playerId: EntityId): EntityId {
        val myr = putCreatureOnBattlefield(playerId, "Myr Mindservant")
        removeSummoningSickness(myr)
        giveColorlessMana(playerId, 2)
        return myr
    }

    /**
     * Activate a ready Mindservant and let the shuffle — and any Probe trigger — resolve. The
     * board is set up in player 1's main phase, so hand priority over first when the non-active
     * player is the one activating.
     */
    fun GameTestDriver.shuffleWith(playerId: EntityId, myr: EntityId) {
        while (priorityPlayer != playerId) passPriority(priorityPlayer!!)
        submit(ActivateAbility(playerId, myr, mindservantAbility)).isSuccess shouldBe true
        bothPass()
        while (stackSize > 0) bothPass()
    }

    test("an ability that shuffles a player's library deals 2 to that player") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        d.putPermanentOnBattlefield(d.player1, "Psychogenic Probe")
        val myr = d.readyMindservant(opponent)

        d.shuffleWith(opponent, myr)

        withClue("the shuffler takes the damage, not the Probe's controller") {
            d.getLifeTotal(opponent) shouldBe 18
            d.getLifeTotal(d.player1) shouldBe 20
        }
    }

    test("the Probe is symmetric — its own controller shuffling takes 2 as well") {
        // "a player", not "an opponent". A trigger scoped to Player.EachOpponent passes the test
        // above and fails this one.
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        d.putPermanentOnBattlefield(d.player1, "Psychogenic Probe")
        val myr = d.readyMindservant(d.player1)

        d.shuffleWith(d.player1, myr)

        withClue("the Probe punishes its controller too") {
            d.getLifeTotal(d.player1) shouldBe 18
            d.getLifeTotal(opponent) shouldBe 20
        }
    }

    test("an empty library is still shuffled, and still deals 2 (CR 701.24e)") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Psychogenic Probe")
        val myr = d.readyMindservant(d.player1)
        val library = ZoneKey(d.player1, Zone.LIBRARY)
        d.replaceState(d.state.copy(zones = d.state.zones + (library to emptyList())))

        d.shuffleWith(d.player1, myr)

        withClue("the 2009 ruling: it triggers even with nothing to shuffle") {
            d.state.getZone(library).shouldBeInstanceOf<List<EntityId>>().size shouldBe 0
            d.getLifeTotal(d.player1) shouldBe 18
        }
    }

    test("a tutor's search-then-shuffle deals 2 (CR 701.24b)") {
        // Fabricate: "Search your library for an artifact card, reveal it, put it into your hand,
        // then shuffle." The found card sits out of the randomization; the library is shuffled
        // regardless, so the trigger fires.
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Psychogenic Probe")
        d.putCardOnTopOfLibrary(d.player1, "Myr Mindservant")
        val fabricate = d.putCardInHand(d.player1, "Fabricate")
        d.giveColorlessMana(d.player1, 2)
        d.giveMana(d.player1, Color.BLUE, 1)

        d.castSpell(d.player1, fabricate).isSuccess shouldBe true
        d.bothPass()

        val search = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, search.options.take(1))
        while (d.stackSize > 0 || d.state.pendingDecision != null) {
            if (d.state.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        withClue("the search's shuffle leg fires the Probe") {
            d.getLifeTotal(d.player1) shouldBe 18
        }
    }

    test("two Probes see one shuffle and deal 4 (CR 701.24f)") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        d.putPermanentOnBattlefield(d.player1, "Psychogenic Probe")
        d.putPermanentOnBattlefield(d.player1, "Psychogenic Probe")
        val myr = d.readyMindservant(opponent)

        d.shuffleWith(opponent, myr)

        withClue("each Probe is its own triggered ability") {
            d.getLifeTotal(opponent) shouldBe 16
        }
    }

    test("shuffling to set the game up is not caused by a spell or ability") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PsychogenicProbe)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0)

        val shuffles = d.events.filterIsInstance<LibraryShuffledEvent>()
        withClue("every library is shuffled while the game is set up (CR 103.2)") {
            shuffles.shouldNotBeEmpty()
        }
        withClue("…and none of them may fire a shuffle trigger") {
            shuffles.all { it.cause == ShuffleCause.GAME_SETUP } shouldBe true
        }
    }

    test("shuffling to take a mulligan is not caused by a spell or ability") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PsychogenicProbe)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = false, startingPlayer = 0)

        d.submit(TakeMulligan(d.player1)).isSuccess shouldBe true

        val mulliganShuffles = d.events.filterIsInstance<LibraryShuffledEvent>()
            .filter { it.playerId == d.player1 && it.cause != ShuffleCause.GAME_SETUP }
        withClue("the mulligan shuffled player 1's library (CR 103.5)") {
            mulliganShuffles.shouldNotBeEmpty()
        }
        withClue("…tagged as a mulligan, so no shuffle trigger can see it") {
            mulliganShuffles.all { it.cause == ShuffleCause.MULLIGAN } shouldBe true
        }
    }
})
