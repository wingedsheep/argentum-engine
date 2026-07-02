package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jou.cards.BanishingLight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Banishing Light chain interactions.
 *
 * Banishing Light: {2}{W}
 * Enchantment
 * When this enchantment enters, exile target nonland permanent an opponent controls
 * until this enchantment leaves the battlefield.
 */
class BanishingLightTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BanishingLight)
        return driver
    }

    fun GameTestDriver.findAllPermanentsByName(name: String): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == name
        }
    }

    fun GameTestDriver.isInExile(playerId: EntityId, cardName: String): Boolean {
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    /**
     * Advance to the next player's precombat main phase.
     * First goes past the current step, then advances to PRECOMBAT_MAIN.
     */
    fun GameTestDriver.advanceToNextPrecombatMain() {
        passPriorityUntil(Step.END)
        bothPass() // move past end step
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /**
     * Cast Banishing Light, resolve it, handle ETB target selection, and resolve ETB trigger.
     */
    fun GameTestDriver.castAndResolveBanishingLight(
        caster: EntityId,
        banishingLightCardId: EntityId,
        etbTarget: EntityId
    ) {
        giveMana(caster, Color.WHITE, 3)
        castSpell(caster, banishingLightCardId)
        bothPass() // resolve spell → BL enters battlefield → ETB trigger
        pendingDecision shouldNotBe null
        submitTargetSelection(caster, listOf(etbTarget))
        bothPass() // resolve ETB trigger
    }

    test("basic Banishing Light exiles opponent creature and returns it on LTB") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 30),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(p2, "Glory Seeker")

        // P1 casts Banishing Light targeting P2's creature
        val bl1 = driver.putCardInHand(p1, "Banishing Light")
        driver.castAndResolveBanishingLight(p1, bl1, creature)

        // P2's creature should be exiled
        driver.findPermanent(p2, "Glory Seeker") shouldBe null
        driver.isInExile(p2, "Glory Seeker") shouldBe true

        // Banishing Light should have LinkedExileComponent
        val blId = driver.findPermanent(p1, "Banishing Light")
        blId shouldNotBe null
        val linked = driver.state.getEntity(blId!!)?.get<LinkedExileComponent>()
        linked shouldNotBe null
        linked!!.exiledIds shouldContain creature

        // Destroy Banishing Light with Wipe Clean
        val wipeClean = driver.putCardInHand(p1, "Wipe Clean")
        driver.giveMana(p1, Color.WHITE, 2)
        driver.castSpellWithTargets(p1, wipeClean, listOf(ChosenTarget.Permanent(blId)))
        driver.bothPass() // resolve Wipe Clean
        driver.bothPass() // resolve LTB trigger

        // Creature should be back on P2's battlefield
        driver.findPermanent(p2, "Glory Seeker") shouldNotBe null
        driver.findPermanent(p1, "Banishing Light") shouldBe null
    }

    test("chained Banishing Lights - P2 exiles P1 BL, creature returns") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 30),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(p2, "Glory Seeker")

        // Step 1: P1 casts Banishing Light targeting P2's creature
        val bl1 = driver.putCardInHand(p1, "Banishing Light")
        driver.castAndResolveBanishingLight(p1, bl1, creature)

        driver.findPermanent(p2, "Glory Seeker") shouldBe null
        val p1BlId = driver.findPermanent(p1, "Banishing Light")!!

        // Advance to P2's turn
        driver.advanceToNextPrecombatMain()
        driver.activePlayer shouldBe p2

        // Step 2: P2 casts Banishing Light targeting P1's Banishing Light
        val bl2 = driver.putCardInHand(p2, "Banishing Light")
        driver.giveMana(p2, Color.WHITE, 3)
        driver.castSpell(p2, bl2)
        driver.bothPass() // resolve spell → BL#2 enters → ETB triggers

        // ETB trigger targets P1's BL
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(p2, listOf(p1BlId))
        driver.bothPass() // resolve ETB → P1's BL exiled → LTB triggers

        // LTB trigger of P1's BL should return Glory Seeker
        if (driver.stackSize > 0) {
            driver.bothPass() // resolve LTB trigger
        }

        // P1's Banishing Light should be exiled, P2's creature should return
        driver.findPermanent(p1, "Banishing Light") shouldBe null
        driver.findPermanent(p2, "Glory Seeker") shouldNotBe null
        driver.findPermanent(p2, "Banishing Light") shouldNotBe null
    }

    test("triple chained Banishing Lights - P1 BL returns and gets new ETB target") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 30),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(p2, "Glory Seeker")

        // Step 1: P1 casts Banishing Light #1 targeting P2's creature
        val bl1Card = driver.putCardInHand(p1, "Banishing Light")
        driver.castAndResolveBanishingLight(p1, bl1Card, creature)
        driver.findPermanent(p2, "Glory Seeker") shouldBe null
        val bl1Id = driver.findPermanent(p1, "Banishing Light")!!

        // Advance to P2's turn
        driver.advanceToNextPrecombatMain()
        driver.activePlayer shouldBe p2

        // Step 2: P2 casts Banishing Light #2 targeting P1's BL#1
        val bl2Card = driver.putCardInHand(p2, "Banishing Light")
        driver.giveMana(p2, Color.WHITE, 3)
        driver.castSpell(p2, bl2Card)
        driver.bothPass() // resolve spell
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(p2, listOf(bl1Id))
        driver.bothPass() // resolve ETB → P1's BL#1 exiled → LTB fires → Glory Seeker returns

        // Resolve LTB if on stack
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        driver.findPermanent(p1, "Banishing Light") shouldBe null
        driver.findPermanent(p2, "Glory Seeker") shouldNotBe null
        val bl2Id = driver.findPermanent(p2, "Banishing Light")!!

        // Advance to P1's turn
        driver.advanceToNextPrecombatMain()
        driver.activePlayer shouldBe p1

        // Step 3: P1 casts Banishing Light #3 targeting P2's BL#2
        val bl3Card = driver.putCardInHand(p1, "Banishing Light")
        driver.giveMana(p1, Color.WHITE, 3)
        driver.castSpell(p1, bl3Card)
        driver.bothPass() // resolve spell → BL#3 enters → ETB triggers

        // BL#3 ETB targets P2's BL#2
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(p1, listOf(bl2Id))
        driver.bothPass() // resolve ETB → P2's BL#2 exiled → LTB fires → P1's BL#1 returns

        // P2's BL#2 LTB should fire and return P1's BL#1 to battlefield
        // Then P1's BL#1 re-entering should trigger its ETB again

        // Resolve LTB trigger (returns BL#1 to battlefield)
        if (driver.stackSize > 0 && driver.pendingDecision == null) {
            driver.bothPass()
        }

        // BL#1's ETB fires again - select new target (P2's Glory Seeker)
        driver.pendingDecision shouldNotBe null
        val glorySeekerBack = driver.findPermanent(p2, "Glory Seeker")
        glorySeekerBack shouldNotBe null
        driver.submitTargetSelection(p1, listOf(glorySeekerBack!!))

        // Resolve the re-entry ETB trigger
        driver.bothPass()

        // Final state assertions:
        // P1 should have two Banishing Lights on the battlefield
        val p1BanishingLights = driver.findAllPermanentsByName("Banishing Light").filter { id ->
            driver.state.getEntity(id)?.get<OwnerComponent>()?.playerId == p1
        }
        p1BanishingLights.size shouldBe 2

        // P2's Glory Seeker should be exiled again (by returned BL#1's new ETB)
        driver.findPermanent(p2, "Glory Seeker") shouldBe null

        // P2's BL#2 should be in exile
        driver.isInExile(p2, "Banishing Light") shouldBe true

        // The returned BL#1 should only have ONE linked exile entry (the new Glory Seeker exile),
        // not a stale duplicate from its previous battlefield visit (Rule 400.7)
        val returnedBl1 = p1BanishingLights.first { id ->
            val linked = driver.state.getEntity(id)?.get<LinkedExileComponent>()
            linked != null && linked.exiledIds.contains(creature)
        }
        val returnedLinked = driver.state.getEntity(returnedBl1)?.get<LinkedExileComponent>()!!
        returnedLinked.exiledIds.size shouldBe 1
    }
})
