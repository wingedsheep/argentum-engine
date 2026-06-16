package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ArchmagesNewt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Archmage's Newt (OTJ #39) — {1}{U} 2/2 Creature — Salamander Mount.
 *
 * "Whenever this creature deals combat damage to a player, target instant or sorcery card in your
 *  graveyard gains flashback until end of turn. The flashback cost is equal to its mana cost. That
 *  card gains flashback {0} until end of turn instead if this creature is saddled. Saddle 3"
 *
 * Exercises the runtime "granted flashback" feature end to end: a combat-damage trigger grants
 * flashback to a graveyard instant/sorcery (cost = its mana cost, or {0} when the Newt is
 * saddled), the cast-from-graveyard enumerator and handler honor the grant, and the spell is
 * exiled as it resolves.
 */
class ArchmagesNewtScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ArchmagesNewt)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Attack the opponent with [newt] (unblocked), resolve combat damage, target [graveyardSpell]. */
    fun connectAndTarget(driver: GameTestDriver, newt: EntityId, graveyardSpell: EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(newt), driver.player2)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        // Combat damage may pause for a [CombatResolutionDecision] (edge assignment); confirm if so.
        if (driver.pendingDecision is com.wingedsheep.engine.core.CombatResolutionDecision) {
            driver.confirmCombatDamage()
        }
        // The combat-damage trigger goes on the stack; choose its graveyard target, then resolve.
        driver.submitTargetSelection(driver.player1, listOf(graveyardSpell))
        driver.bothPass()
    }

    test("unsaddled: grants flashback equal to the card's mana cost, then exiles it") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2

        val newt = driver.putCreatureOnBattlefield(player, "Archmage's Newt")
        driver.removeSummoningSickness(newt)
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt") // {R}

        connectAndTarget(driver, newt, bolt)

        // The granted flashback record exists.
        driver.state.grantedKeywordAbilities.any { it.entityId == bolt } shouldBe true

        // Flashback cost equals the card's mana cost: {R}.
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                player, bolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Bolt dealt 3 to the opponent (they took 2 combat + 3 = started 20). The spell exiled.
        driver.state.getZone(ZoneKey(player, Zone.EXILE)).contains(bolt) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(bolt) shouldBe false
    }

    test("saddled: grants flashback {0}, castable for no mana") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2

        val newt = driver.putCreatureOnBattlefield(player, "Archmage's Newt")
        // Saddle 3: two Grizzly Bears (power 2 each) total power 4 >= 3.
        val s1 = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val s2 = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(newt)

        driver.submitSuccess(SaddleMount(player, newt, listOf(s1, s2)))
        driver.bothPass()

        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt") // {R}, but {0} when saddled

        connectAndTarget(driver, newt, bolt)

        driver.state.grantedKeywordAbilities.any { it.entityId == bolt } shouldBe true

        // No mana given: a {0} flashback cost means the cast is affordable.
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val flashbackAction = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
            .firstOrNull { (it.action as? CastSpell)?.cardId == bolt && it.actionType == "CastWithFlashback" }
        (flashbackAction != null) shouldBe true
        flashbackAction!!.affordable shouldBe true

        driver.submit(
            CastSpell(
                player, bolt,
                targets = listOf(ChosenTarget.Player(opponent)),
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.EXILE)).contains(bolt) shouldBe true
    }

    test("only the targeted card gains flashback; the grant expires at end of turn") {
        val driver = newDriver()
        val player = driver.player1

        val newt = driver.putCreatureOnBattlefield(player, "Archmage's Newt")
        driver.removeSummoningSickness(newt)
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        val other = driver.putCardInGraveyard(player, "Careful Study") // not targeted

        connectAndTarget(driver, newt, bolt)

        driver.state.grantedKeywordAbilities.any { it.entityId == bolt } shouldBe true
        driver.state.grantedKeywordAbilities.any { it.entityId == other } shouldBe false

        // The grant expires during cleanup; by the next upkeep it is gone.
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 300)
        driver.state.grantedKeywordAbilities.any { it.entityId == bolt } shouldBe false
    }
})
