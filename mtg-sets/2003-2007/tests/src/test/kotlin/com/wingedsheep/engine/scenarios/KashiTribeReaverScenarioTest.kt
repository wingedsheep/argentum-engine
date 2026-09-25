package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.KashiTribeReaver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kashi-Tribe Reaver (CHK) — "Whenever this creature deals combat damage to a creature, tap that creature and
 * it doesn't untap during its controller's next untap step."
 *
 * The blocker is untapped when damage is dealt (blocking doesn't tap), so its being tapped proves
 * the trigger resolved against the damaged creature; its staying tapped through the opponent's
 * untap step proves the lock is keyed to the *affected* creature's controller. The 3/2 Reaver dies to
 * the 5/5 blocker unless it regenerates, which the last test covers.
 */
class KashiTribeReaverScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + KashiTribeReaver)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("the blocker it damages is tapped and skips its controller's next untap step") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val snake = d.putCreatureOnBattlefield(me, "Kashi-Tribe Reaver")
        d.removeSummoningSickness(snake)
        val blocker = d.putCreatureOnBattlefield(opp, "Force of Nature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(snake), defendingPlayer = opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(snake))).error shouldBe null
        d.isTapped(blocker) shouldBe false

        d.passPriorityUntil(Step.END)
        withClue("the damaged blocker survived and was tapped by the trigger") {
            d.findPermanent(opp, "Force of Nature").shouldNotBeNull()
            d.isTapped(blocker) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opp
        withClue("the blocker's controller's untap step skipped it") {
            d.isTapped(blocker) shouldBe true
        }
    }

    test("combat damage to a player doesn't trigger it") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val snake = d.putCreatureOnBattlefield(me, "Kashi-Tribe Reaver")
        d.removeSummoningSickness(snake)
        val bystander = d.putCreatureOnBattlefield(opp, "Force of Nature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(snake), defendingPlayer = opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, emptyMap()).error shouldBe null
        d.passPriorityUntil(Step.END)

        d.isTapped(bystander) shouldBe false
    }
    test("{1}{G} regenerates it through a lethal block, and the blocker is still locked") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val reaver = d.putCreatureOnBattlefield(me, "Kashi-Tribe Reaver")
        d.removeSummoningSickness(reaver)
        val blocker = d.putCreatureOnBattlefield(opp, "Force of Nature")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(reaver), defendingPlayer = opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(reaver))).error shouldBe null
        if (d.priorityPlayer != me) d.passPriority(opp).error shouldBe null

        d.giveMana(me, Color.GREEN, 1)
        d.giveColorlessMana(me, 1)
        d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = reaver,
                abilityId = KashiTribeReaver.activatedAbilities.first().id,
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.passPriorityUntil(Step.END)
        withClue("the regeneration shield replaced the Reaver's destruction") {
            d.findPermanent(me, "Kashi-Tribe Reaver").shouldNotBeNull()
        }
        d.isTapped(blocker) shouldBe true
    }
})
