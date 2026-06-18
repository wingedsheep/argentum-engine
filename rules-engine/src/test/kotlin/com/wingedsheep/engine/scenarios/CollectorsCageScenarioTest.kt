package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.big.cards.CollectorsCage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Collector's Cage (BIG #1) — {1}{W} Artifact.
 *
 * "Hideaway 5 (When this artifact enters, look at the top five cards of your library, exile one
 *  face down, then put the rest on the bottom in a random order.)
 *  {1}, {T}: Put a +1/+1 counter on target creature you control. Then if you control three or more
 *  creatures with different powers, you may play the exiled card without paying its mana cost."
 *
 * Covers: Hideaway exiles one card on ETB; the activated ability always adds a +1/+1 counter, and
 * gates the "play the exiled card for free" offer on controlling 3+ creatures with different powers.
 */
class CollectorsCageScenarioTest : FunSpec({

    val cageAbilityId = CollectorsCage.activatedAbilities.first().id

    /** Cast Collector's Cage from hand so Hideaway runs, exiling one card face down. */
    fun setUpCageWithHideaway(driver: GameTestDriver, player: EntityId): EntityId {
        val cage = driver.findCardInHand(player, "Collector's Cage")!!
        driver.giveMana(player, Color.WHITE, 2)
        driver.castSpell(player, cage)
        driver.bothPass() // resolve the spell → it enters → Hideaway trigger goes on the stack
        driver.bothPass() // resolve Hideaway → pauses for "exile one of top five"
        // Choose the first option to exile face down.
        val decision = driver.pendingDecision
        require(decision is SelectCardsDecision) { "expected hideaway selection, got $decision" }
        driver.submitCardSelection(player, listOf(decision.options.first()))
        return driver.findPermanent(player, "Collector's Cage")!!
    }

    test("Hideaway exiles exactly one card linked to the Cage") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardInHand(player, "Collector's Cage")

        val exiledBefore = driver.getExile(player).size
        setUpCageWithHideaway(driver, player)
        val exiledAfter = driver.getExile(player).size

        (exiledAfter - exiledBefore) shouldBe 1
    }

    test("ability adds a +1/+1 counter; with 3+ distinct powers it offers the free play") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardInHand(player, "Collector's Cage")
        val cage = setUpCageWithHideaway(driver, player)

        // Three creatures with distinct powers: Birds of Paradise (0), Grizzly Bears (2), Hill Giant (3).
        val birds = driver.putCreatureOnBattlefield(player, "Birds of Paradise")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Hill Giant")
        driver.giveMana(player, Color.WHITE, 1)

        driver.submit(
            ActivateAbility(
                playerId = player, sourceId = cage, abilityId = cageAbilityId,
                // Counter the Birds (0 → 1) so powers become {1, 2, 3} = 3 distinct → free-play offered.
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(birds))
            )
        ).error shouldBe null
        driver.bothPass() // resolve the ability

        // The counter landed.
        driver.state.getEntity(birds)!!
            .get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

        // After the counter, powers are {1, 2, 3} = 3 distinct → "you may play the exiled card" offer.
        val d = driver.pendingDecision
        (d is YesNoDecision) shouldBe true
        // Decline the free play; nothing further required.
        driver.submitYesNo(player, false)
    }

    test("with fewer than 3 distinct powers, no free-play offer is made") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardInHand(player, "Collector's Cage")
        val cage = setUpCageWithHideaway(driver, player)

        // Two creatures, same power (Grizzly Bears 2/2 x2) → at most 1 distinct power even after a counter.
        val a = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.giveMana(player, Color.WHITE, 1)

        driver.submit(
            ActivateAbility(
                playerId = player, sourceId = cage, abilityId = cageAbilityId,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(a))
            )
        ).error shouldBe null
        driver.bothPass()

        // Counter applied, but no play-exiled-card offer.
        driver.state.getEntity(a)!!
            .get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        val d = driver.pendingDecision
        (d == null || d !is YesNoDecision) shouldBe true
    }
})
