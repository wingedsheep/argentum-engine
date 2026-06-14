package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.WitchKingOfAngmar
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Witch-king of Angmar (LTR #114) — {3}{B}{B} Legendary Creature — Wraith Noble, 5/3.
 *
 *   Flying
 *   Whenever one or more creatures deal combat damage to you, each opponent sacrifices a creature
 *   of their choice that dealt combat damage to you this turn. The Ring tempts you.
 *   Discard a card: Witch-king of Angmar gains indestructible until end of turn. Tap it.
 *
 * Verifies the defensive combat-damage batch trigger + edict restricted to creatures that dealt
 * combat damage to the Witch-king's controller this turn (a creature that did NOT deal you damage
 * is not a legal sacrifice), the Ring temptation, and the discard-for-indestructible ability.
 */
class WitchKingOfAngmarScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WitchKingOfAngmar))
        return driver
    }

    test("opponent sacrifices the creature that dealt you combat damage, and the Ring tempts you") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!          // the opponent (each opponent sacrifices)
        val defender = driver.getOpponent(attacker)   // controls Witch-king

        // Defender controls the Witch-king (the trigger's controller).
        driver.putCreatureOnBattlefield(defender, "Witch-king of Angmar")

        // Attacker has two creatures: the Bears attack and connect; the Giant stays home and so
        // never deals combat damage to the defender this turn.
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        val giant = driver.putCreatureOnBattlefield(attacker, "Hill Giant")
        driver.removeSummoningSickness(bears)
        driver.removeSummoningSickness(giant)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(bears), defender).isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(defender).isSuccess shouldBe true

        // Advance through the combat-damage step: defender takes 2, the Witch-king trigger goes on
        // the stack and resolves. passPriorityUntil auto-resolves the combat-damage assignment, the
        // edict sacrifice (only the connecting Bears qualifies), and the Ring temptation.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.assertLifeTotal(defender, 18)

        // The Bears dealt combat damage to the defender this turn -> sacrificed.
        driver.getGraveyardCardNames(attacker) shouldContain "Grizzly Bears"
        // The Hill Giant never dealt the defender combat damage -> not a legal sacrifice, survives.
        driver.findPermanent(attacker, "Hill Giant") shouldNotBe null
        driver.getGraveyardCardNames(attacker) shouldNotContain "Hill Giant"

        // The Ring tempted the Witch-king's controller.
        driver.state.getEntity(defender)?.get<TheRingComponent>()?.temptCount shouldBe 1
    }

    test("discard a card: Witch-king gains indestructible until end of turn and taps itself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val witchKing = driver.putCreatureOnBattlefield(player, "Witch-king of Angmar")
        val toDiscard = driver.putCardInHand(player, "Grizzly Bears")
        val abilityId = WitchKingOfAngmar.activatedAbilities[0].id

        driver.state.projectedState.hasKeyword(witchKing, Keyword.INDESTRUCTIBLE) shouldBe false
        driver.isTapped(witchKing) shouldBe false

        val result = driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = witchKing,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Card was discarded as the cost.
        driver.getHand(player).contains(toDiscard) shouldBe false
        driver.getGraveyard(player).contains(toDiscard) shouldBe true

        // Witch-king gained indestructible (projected) and tapped itself.
        driver.state.projectedState.hasKeyword(witchKing, Keyword.INDESTRUCTIBLE) shouldBe true
        driver.isTapped(witchKing) shouldBe true
    }
})
