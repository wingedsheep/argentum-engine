package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ninjutsu (CR 702.49), as the hand-activated ability `ninjutsu(cost)` builds:
 *
 * - 702.49a: "[Cost], Reveal this card from your hand, Return an unblocked attacking creature you
 *   control to its owner's hand: Put this card onto the battlefield from your hand tapped and
 *   attacking." — an activated ability, so the card is never cast.
 * - 702.49c: it enters attacking the same player, planeswalker, or battle as the returned creature.
 * - 506.3c: if that defender is gone, it enters but is never attacking.
 * - 506.3d: put onto the battlefield attacking after blockers are declared, it is unblocked.
 */
class NinjutsuTest : FunSpec({

    val shinobi = card("Test Shinobi") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Human Ninja"
        power = 3
        toughness = 3
        ninjutsu("{1}{G}")
    }

    val brawlerCard = card("Plain Brawler") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val walkerCard = card("Test Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Tester"
        startingLoyalty = 3
        loyaltyAbility(1) { effect = Effects.GainLife(1) }
    }

    val ninjutsuId = ninjutsuAbility("{1}{G}").id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(shinobi, brawlerCard, walkerCard))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.priorityTo(player: EntityId, step: Step) {
        var guard = 0
        while (state.priorityPlayerId != null && state.priorityPlayerId != player &&
            state.step == step && guard++ < 4
        ) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    /** Attack [defenderTarget] with [creature], declare no blocks, and give [attacker] priority. */
    fun GameTestDriver.attackUnblocked(attacker: EntityId, defender: EntityId, creature: EntityId, defenderTarget: EntityId = defender) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, mapOf(creature to defenderTarget)).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareBlockers(defender, emptyMap()).outcome shouldBe Outcome.Done
        priorityTo(attacker, Step.DECLARE_BLOCKERS)
    }

    fun ninjutsu(player: EntityId, ninja: EntityId, returned: EntityId) = ActivateAbility(
        playerId = player,
        sourceId = ninja,
        abilityId = ninjutsuId,
        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(returned)),
        paymentStrategy = PaymentStrategy.FromPool,
    )

    fun ninjutsuOffers(driver: GameTestDriver, player: EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .mapNotNull { it.action as? ActivateAbility }
            .filter { it.abilityId == ninjutsuId }

    test("702.49a/c — an activated ability from hand: return the attacker, enter tapped and attacking its defender") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val brawler = driver.putCreatureOnBattlefield(attacker, "Plain Brawler")
        driver.removeSummoningSickness(brawler)
        val ninja = driver.putCardInHand(attacker, "Test Shinobi")

        driver.attackUnblocked(attacker, defender, brawler)
        driver.giveMana(attacker, Color.GREEN, 2)
        val eventsBefore = driver.events.size
        driver.submit(ninjutsu(attacker, ninja, brawler)).outcome shouldBe Outcome.Done

        withClue("the cost returned the attacker at once; the ninja waits in hand") {
            driver.getHand(attacker) shouldContain brawler
            driver.getHand(attacker) shouldContain ninja
        }
        withClue("an ability is on the stack, not a spell — ninjutsu is never a cast") {
            driver.state.getEntity(driver.state.stack.last())?.has<ActivatedAbilityOnStackComponent>() shouldBe true
            val newEvents = driver.events.drop(eventsBefore)
            newEvents.filterIsInstance<SpellCastEvent>().shouldBeEmpty()
            newEvents.filterIsInstance<AbilityActivatedEvent>().single().keyword shouldBe Keyword.NINJUTSU
        }

        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        val ninjaPerm = driver.findPermanent(attacker, "Test Shinobi").shouldNotBeNull()
        driver.state.getEntity(ninjaPerm)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(ninjaPerm)?.get<AttackingComponent>()?.defenderId shouldBe defender

        withClue("506.3d — it entered unblocked and deals its combat damage") {
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
            driver.assertLifeTotal(defender, 17)
        }
    }

    test("offered only once an unblocked attacker exists — and still after combat damage") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val brawler = driver.putCreatureOnBattlefield(attacker, "Plain Brawler")
        driver.removeSummoningSickness(brawler)
        driver.putCardInHand(attacker, "Test Shinobi")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(attacker, Color.GREEN, 2)
        ninjutsuOffers(driver, attacker).shouldBeEmpty()

        driver.attackUnblocked(attacker, defender, brawler)
        driver.giveMana(attacker, Color.GREEN, 2)
        ninjutsuOffers(driver, attacker).shouldNotBeEmpty()

        driver.passPriorityUntil(Step.END_COMBAT)
        driver.priorityTo(attacker, Step.END_COMBAT)
        driver.giveMana(attacker, Color.GREEN, 2)
        withClue("the Brawler is still an unblocked attacker in the end of combat step") {
            ninjutsuOffers(driver, attacker).shouldNotBeEmpty()
        }
    }

    test("a blocked attacker can't be returned to pay for ninjutsu") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val brawler = driver.putCreatureOnBattlefield(attacker, "Plain Brawler")
        driver.removeSummoningSickness(brawler)
        val blocker = driver.putCreatureOnBattlefield(defender, "Plain Brawler")
        val ninja = driver.putCardInHand(attacker, "Test Shinobi")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, mapOf(brawler to defender)).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(blocker to listOf(brawler))).outcome shouldBe Outcome.Done
        driver.priorityTo(attacker, Step.DECLARE_BLOCKERS)
        driver.giveMana(attacker, Color.GREEN, 2)

        ninjutsuOffers(driver, attacker).shouldBeEmpty()
        driver.submitExpectFailure(ninjutsu(attacker, ninja, brawler))
    }

    test("506.3c — the returned creature's planeswalker is gone: the ninja enters tapped, never attacking") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val walker = driver.putPermanentOnBattlefield(defender, "Test Walker")
        driver.replaceState(
            driver.state.updateEntity(walker) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.LOYALTY, 3))
            }
        )
        val brawler = driver.putCreatureOnBattlefield(attacker, "Plain Brawler")
        driver.removeSummoningSickness(brawler)
        val ninja = driver.putCardInHand(attacker, "Test Shinobi")

        driver.attackUnblocked(attacker, defender, brawler, defenderTarget = walker)
        driver.giveMana(attacker, Color.GREEN, 2)
        driver.submit(ninjutsu(attacker, ninja, brawler)).outcome shouldBe Outcome.Done
        driver.replaceState(driver.state.removeFromZone(ZoneKey(defender, Zone.BATTLEFIELD), walker))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val ninjaPerm = driver.findPermanent(attacker, "Test Shinobi").shouldNotBeNull()
        driver.state.getEntity(ninjaPerm)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(ninjaPerm)?.get<AttackingComponent>().shouldBeNull()
    }

    test("the ninja left the hand before resolution: nothing enters, the returned creature stays in hand") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val brawler = driver.putCreatureOnBattlefield(attacker, "Plain Brawler")
        driver.removeSummoningSickness(brawler)
        val ninja = driver.putCardInHand(attacker, "Test Shinobi")

        driver.attackUnblocked(attacker, defender, brawler)
        driver.giveMana(attacker, Color.GREEN, 2)
        driver.submit(ninjutsu(attacker, ninja, brawler)).outcome shouldBe Outcome.Done
        // Discarded in response.
        driver.replaceState(
            driver.state.removeFromZone(ZoneKey(attacker, Zone.HAND), ninja)
                .addToZone(ZoneKey(attacker, Zone.GRAVEYARD), ninja)
        )
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(attacker, "Test Shinobi").shouldBeNull()
        driver.getHand(attacker) shouldContain brawler
    }
})
