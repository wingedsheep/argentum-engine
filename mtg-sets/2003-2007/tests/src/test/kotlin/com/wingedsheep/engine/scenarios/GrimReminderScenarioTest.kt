package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.GrimReminder
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Grim Reminder (MRD #66) — "Search your library for a nonland card and reveal it. Each opponent
 * who cast a spell this turn with the same name as that card loses 6 life. Then shuffle."
 *
 * The card reads *cast history*, which is what made it engine-blocked: the name it looks for is
 * captured at resolution time, and `CardPredicate.NameEqualsChosen` was hardcoded to `false` when
 * matched against a `CastSpellRecord`. Every test here therefore pivots on the name, not on the
 * search: the same board, the same six life, decided by whether the *right player* cast the *right
 * name*. A regression to the old fail-closed behaviour makes the first test fail while the three
 * negative tests keep passing, which is exactly the wrong way round for a hardcoded `false`.
 */
class GrimReminderScenarioTest : FunSpec({

    val returnAbility = GrimReminder.activatedAbilities.single().id

    fun driver(stopAt: Step = Step.PRECOMBAT_MAIN): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + GrimReminder)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(stopAt)
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    /** [caster] casts Dark Ritual — a no-target instant, so it works from either seat. */
    fun GameTestDriver.castDarkRitual(caster: EntityId) {
        if (caster != activePlayer) passPriority(activePlayer!!)
        val ritual = putCardInHand(caster, "Dark Ritual")
        giveMana(caster, Color.BLACK, 1)
        castSpell(caster, ritual).isSuccess shouldBe true
        bothPass()
        // Hand priority back to player 1 so the Grim Reminder cast below is legal — passing it to
        // the opponent to let them cast leaves it there once the stack empties.
        if (priorityPlayer != player1) passPriority(priorityPlayer!!)
    }

    /**
     * Plant a Dark Ritual and a Giant Growth in player 1's library, cast Grim Reminder, and answer
     * its search with [pick] (an empty list = fail to find). Returns the two planted card ids.
     */
    fun GameTestDriver.reminderSearch(
        pick: (ritual: EntityId, decoy: EntityId) -> List<EntityId>
    ): Pair<EntityId, EntityId> {
        val ritual = putCardOnTopOfLibrary(player1, "Dark Ritual")
        val decoy = putCardOnTopOfLibrary(player1, "Giant Growth")
        val reminder = putCardInHand(player1, "Grim Reminder")
        giveMana(player1, Color.BLACK, 3)
        castSpell(player1, reminder).isSuccess shouldBe true
        bothPass()

        val decision = pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("a search sees every nonland card in the library, and only those") {
            decision.options.toSet() shouldBe setOf(ritual, decoy)
        }
        submitDecision(player1, CardsSelectedResponse(decision.id, pick(ritual, decoy)))
        resolveStack(this)
        return ritual to decoy
    }

    test("an opponent who cast that name this turn loses 6") {
        val d = driver()
        d.castDarkRitual(d.player2)

        val (ritual, _) = d.reminderSearch { r, _ -> listOf(r) }

        d.getLifeTotal(d.player2) shouldBe 14
        withClue("the caster is untouched — only opponents pay") {
            d.getLifeTotal(d.player1) shouldBe 20
        }
        withClue("the found card is revealed, never moved: it stays in the library") {
            d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).contains(ritual) shouldBe true
        }
    }

    test("naming a card the opponent did not cast costs them nothing") {
        // Same board, same search, different name — this is the assertion that a permissive
        // "any spell this turn" reading would fail.
        val d = driver()
        d.castDarkRitual(d.player2)

        d.reminderSearch { _, decoy -> listOf(decoy) }

        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("your own cast of that name does not hit anyone") {
        // "Each opponent who cast a spell this turn" — the history consulted is per player, not
        // the game-wide one.
        val d = driver()
        d.castDarkRitual(d.player1)

        d.reminderSearch { ritual, _ -> listOf(ritual) }

        d.getLifeTotal(d.player2) shouldBe 20
        d.getLifeTotal(d.player1) shouldBe 20
    }

    test("failing to find names nothing, so nobody loses life") {
        // A library is a hidden zone (CR 701.19c), so declining is legal — and with no name
        // captured the cast-history predicate must match nothing rather than everything.
        val d = driver()
        d.castDarkRitual(d.player2)

        d.reminderSearch { _, _ -> emptyList() }

        d.getLifeTotal(d.player2) shouldBe 20
    }

    test("the graveyard ability is upkeep-only, and returns the card when it is legal") {
        val main = driver()
        val buried = main.putCardInGraveyard(main.player1, "Grim Reminder")
        main.giveMana(main.player1, Color.BLACK, 2)
        withClue("a main phase is not an upkeep") {
            main.submitExpectFailure(ActivateAbility(main.player1, buried, returnAbility))
        }

        val upkeep = driver(stopAt = Step.UPKEEP)
        upkeep.assertStep(Step.UPKEEP)
        val card = upkeep.putCardInGraveyard(upkeep.player1, "Grim Reminder")
        upkeep.giveMana(upkeep.player1, Color.BLACK, 2)
        upkeep.submit(ActivateAbility(upkeep.player1, card, returnAbility)).isSuccess shouldBe true
        resolveStack(upkeep)

        withClue("it comes back to hand from the graveyard") {
            upkeep.getHand(upkeep.player1).contains(card) shouldBe true
            upkeep.getGraveyard(upkeep.player1).contains(card) shouldBe false
        }
    }
})
