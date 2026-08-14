package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.TinybonesBaubleBurglar
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Tinybones, Bauble Burglar (FDN #72) — {1}{B} Legendary Creature — Skeleton Rogue 1/3.
 *
 * "Whenever an opponent discards a card, exile it from their graveyard with a stash counter on it.
 *  During your turn, you may play cards you don't own with stash counters on them from exile, and
 *  mana of any type can be spent to cast those spells.
 *  {3}{B}, {T}: Each opponent discards a card. Activate only as a sorcery."
 *
 * Covers both new engine capabilities and the card's two rulings:
 *  - the discard trigger binds *the discarded card* (one firing per card, CR 400.7e), and the exile
 *    is gated on the card still being in the graveyard;
 *  - the play permission is a live filter over exile, so it covers cards stashed by a *previous*
 *    Tinybones (ruling 1), is limited to your turn and to cards you don't own, and waives nothing
 *    but the colored mana requirements (ruling 2 + CR 118.14).
 */
class TinybonesBaubleBurglarScenarioTest : FunSpec({

    val discardAbilityId = TinybonesBaubleBurglar.activatedAbilities[0].id

    // {G}{G} so that paying it from Swamps proves "mana of any type can be spent".
    val greenBear = card("Stash Test Bear") {
        manaCost = "{G}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val stashTestInstant = card("Stash Test Shock") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Stash Test Shock deals 2 damage to any target."
        spell {
            effect = Effects.GainLife(1)
        }
    }
    // One spell, two discarded cards — proves each firing of the discard trigger binds its own card.
    val doubleDiscard = card("Stash Test Duress") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Each opponent discards two cards."
        spell {
            effect = Effects.EachOpponentDiscards(2)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TinybonesBaubleBurglar)
        driver.registerCard(greenBear)
        driver.registerCard(stashTestInstant)
        driver.registerCard(doubleDiscard)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.legalActionsFor(playerId: EntityId) =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, playerId)

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        legalActionsFor(playerId).mapNotNull { it.action as? CastSpell }.filter { it.cardId == cardId }

    fun GameTestDriver.playLandActionsFor(playerId: EntityId, cardId: EntityId): List<PlayLand> =
        legalActionsFor(playerId).mapNotNull { it.action as? PlayLand }.filter { it.cardId == cardId }

    fun GameTestDriver.stashCounters(cardId: EntityId): Int =
        state.getEntity(cardId)?.get<CountersComponent>()?.getCount(CounterType.STASH) ?: 0

    /** Put a card in [ownerId]'s exile with a stash counter, as Tinybones' first ability would. */
    fun GameTestDriver.putStashedCardInExile(ownerId: EntityId, cardName: String): EntityId {
        val cardId = putCardInExile(ownerId, cardName)
        addComponent(cardId, CountersComponent().withAdded(CounterType.STASH, 1))
        return cardId
    }

    /**
     * Resolve everything on the stack, answering any card-selection decision (the discard pick) with
     * [discards] so the test controls *which* cards are discarded, and auto-answering anything else.
     */
    fun GameTestDriver.settle(discards: List<EntityId> = emptyList(), maxSteps: Int = 40) {
        repeat(maxSteps) {
            val decision = pendingDecision
            when {
                decision is SelectCardsDecision && discards.isNotEmpty() ->
                    submitCardSelection(decision.playerId, discards.take(decision.maxSelections))
                decision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    test("activating the ability makes each opponent discard, and the discard is exiled with a stash counter") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val tinybones = driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        driver.removeSummoningSickness(tinybones)
        val victim = driver.putCardInHand(opponent, "Stash Test Bear")
        driver.giveMana(me, Color.BLACK, 4)

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = tinybones, abilityId = discardAbilityId)
        )
        driver.settle(discards = listOf(victim))

        // Discarded (CR 701.9a: hand → graveyard), then exiled from the graveyard by the trigger.
        driver.state.getZone(opponent, Zone.HAND).contains(victim).shouldBeFalse()
        driver.state.getZone(opponent, Zone.GRAVEYARD).contains(victim).shouldBeFalse()
        driver.state.getZone(opponent, Zone.EXILE).contains(victim).shouldBeTrue()
        driver.stashCounters(victim) shouldBe 1
    }

    test("a two-card discard fires the trigger once per card, exiling both with their own stash counter") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        val first = driver.putCardInHand(opponent, "Stash Test Bear")
        val second = driver.putCardInHand(opponent, "Stash Test Shock")
        val duress = driver.putCardInHand(me, "Stash Test Duress")
        driver.giveMana(me, Color.BLACK, 1)

        driver.castSpell(me, duress).isSuccess shouldBe true
        driver.settle(discards = listOf(first, second))

        driver.state.getZone(opponent, Zone.EXILE).containsAll(listOf(first, second)).shouldBeTrue()
        driver.stashCounters(first) shouldBe 1
        driver.stashCounters(second) shouldBe 1
    }

    test("the exile is skipped when the discarded card left the graveyard before the trigger resolved") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val tinybones = driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        driver.removeSummoningSickness(tinybones)
        val victim = driver.putCardInHand(opponent, "Stash Test Bear")
        driver.giveMana(me, Color.BLACK, 4)

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = tinybones, abilityId = discardAbilityId)
        )
        // Resolve the ability itself (the discard), stopping while the discard trigger is still on
        // the stack.
        run {
            repeat(20) {
                val decision = driver.pendingDecision
                when {
                    decision is SelectCardsDecision -> driver.submitCardSelection(opponent, listOf(victim))
                    decision != null -> driver.autoResolveDecision()
                    else -> driver.bothPass()
                }
                if (driver.state.getZone(opponent, Zone.GRAVEYARD).contains(victim)) return@run
            }
        }
        driver.state.getZone(opponent, Zone.GRAVEYARD).contains(victim).shouldBeTrue()

        // Someone returns the discarded card to its owner's hand before the trigger resolves — by
        // then it is a different object (CR 400.7), so "exile it from their graveyard" does nothing.
        val graveyard = ZoneKey(opponent, Zone.GRAVEYARD)
        val hand = ZoneKey(opponent, Zone.HAND)
        driver.replaceState(
            driver.state.copy(
                zones = driver.state.zones +
                    (graveyard to driver.state.getZone(graveyard) - victim) +
                    (hand to driver.state.getZone(hand) + victim)
            )
        )
        driver.settle()

        driver.state.getZone(opponent, Zone.HAND).contains(victim).shouldBeTrue()
        driver.state.getZone(opponent, Zone.EXILE).contains(victim).shouldBeFalse()
        driver.stashCounters(victim) shouldBe 0
    }

    test("during your turn you may cast a stash-countered card you don't own, paying its colored cost with any mana") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        val stashed = driver.putStashedCardInExile(opponent, "Stash Test Bear")
        // Black sources only: paying the {G}{G} cost proves the "mana of any type" relaxation.
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        driver.castActionsFor(me, stashed).isNotEmpty().shouldBeTrue()

        driver.castSpell(me, stashed).isSuccess shouldBe true
        driver.settle()

        driver.state.getZone(me, Zone.BATTLEFIELD).contains(stashed).shouldBeTrue()
    }

    test("a stash-countered land you don't own can be played from exile during your main phase") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        val stashedLand = driver.putStashedCardInExile(opponent, "Swamp")

        driver.playLandActionsFor(me, stashedLand).isNotEmpty().shouldBeTrue()
        driver.playLand(me, stashedLand).isSuccess shouldBe true
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(stashedLand).shouldBeTrue()
    }

    test("the permission covers cards stashed by a Tinybones that has since left the battlefield") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        // The stash outlives its Tinybones: with none on the battlefield there is no permission…
        val stashed = driver.putStashedCardInExile(opponent, "Stash Test Bear")
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }
        driver.castActionsFor(me, stashed).isEmpty().shouldBeTrue()

        // …and a freshly cast Tinybones grants access to it again (ruling: "regardless of whether
        // they were put there by the Tinybones you currently control or a Tinybones that was
        // previously on the battlefield").
        driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        driver.castActionsFor(me, stashed).isNotEmpty().shouldBeTrue()
    }

    test("cards you own with stash counters are not playable from exile") {
        val driver = newDriver()
        val me = driver.player1

        driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        val myOwnStashedCard = driver.putStashedCardInExile(me, "Stash Test Bear")
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        driver.castActionsFor(me, myOwnStashedCard).isEmpty().shouldBeTrue()
    }

    test("an exiled card without a stash counter is not playable") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        val plainExiledCard = driver.putCardInExile(opponent, "Stash Test Bear")
        repeat(2) { driver.putLandOnBattlefield(me, "Swamp") }

        driver.castActionsFor(me, plainExiledCard).isEmpty().shouldBeTrue()
    }

    test("the permission is closed during the opponent's turn, even for an instant") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        driver.putCreatureOnBattlefield(me, "Tinybones, Bauble Burglar")
        val stashedInstant = driver.putStashedCardInExile(opponent, "Stash Test Shock")
        driver.putLandOnBattlefield(me, "Swamp")

        // On my turn the instant is castable from exile…
        driver.castActionsFor(me, stashedInstant).isNotEmpty().shouldBeTrue()

        // …but "During your turn" closes the permission on the opponent's turn, so even an instant
        // (which timing alone would allow) offers no cast action.
        repeat(60) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
            if (driver.state.activePlayerId == opponent && driver.state.step == Step.PRECOMBAT_MAIN) return@repeat
        }
        driver.state.activePlayerId shouldBe opponent
        driver.castActionsFor(me, stashedInstant).isEmpty().shouldBeTrue()
    }
})
