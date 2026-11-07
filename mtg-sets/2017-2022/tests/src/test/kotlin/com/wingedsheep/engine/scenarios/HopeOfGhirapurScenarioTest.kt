package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.HopeOfGhirapur
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hope of Ghirapur (AER #154) — "Sacrifice Hope of Ghirapur: Until your next turn, target player
 * who was dealt combat damage by Hope of Ghirapur this turn can't cast noncreature spells."
 *
 * Only a player the Hope dealt *combat* damage to this turn is a legal target; the sacrifice cost
 * moves the Hope to the graveyard before the ability resolves, so the target re-check reads its
 * last-known damage record. The ban stops noncreature spells only, and lifts as your next turn begins.
 */
class HopeOfGhirapurScenarioTest : FunSpec({

    val sacrifice = HopeOfGhirapur.activatedAbilities.single().id

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(HopeOfGhirapur))
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.sacrificeTargeting(controller: EntityId, hope: EntityId, victim: EntityId) =
        submit(ActivateAbility(controller, hope, sacrifice, targets = listOf(ChosenTarget.Player(victim))))

    fun GameTestDriver.connect(me: EntityId, opp: EntityId, hope: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(me, listOf(hope), opp).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("Hope connected") { getLifeTotal(opp) shouldBe 19 }
    }

    fun GameTestDriver.canCast(player: EntityId, card: EntityId, color: Color): Boolean {
        giveMana(player, color, 1)
        return legalActions(player).any { (it.action as? CastSpell)?.cardId == card }
    }

    test("the player it dealt combat damage to can't cast noncreature spells until your next turn") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val hope = d.putCreatureOnBattlefield(me, "Hope of Ghirapur")
        d.removeSummoningSickness(hope)
        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        val bears = d.putCardInHand(opp, "Grizzly Bears")

        d.connect(me, opp, hope)
        d.sacrificeTargeting(me, hope, opp).error shouldBe null
        withClue("sacrificed as the cost") { d.state.getBattlefield().contains(hope) shouldBe false }
        d.bothPass()

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        withClue("noncreature spells are banned on their turn") { d.canCast(opp, bolt, Color.RED) shouldBe false }
        d.giveMana(opp, Color.GREEN, 1)
        withClue("creature spells are not") { d.canCast(opp, bears, Color.GREEN) shouldBe true }

        d.passPriorityUntil(Step.UPKEEP, me)
        d.submit(PassPriority(me)).error shouldBe null
        withClue("the ban lifts on your next turn") { d.canCast(opp, bolt, Color.RED) shouldBe true }
    }

    test("can't target a player it hasn't dealt combat damage to this turn") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val hope = d.putCreatureOnBattlefield(me, "Hope of Ghirapur")

        d.sacrificeTargeting(me, hope, opp).error shouldNotBe null
        d.sacrificeTargeting(me, hope, me).error shouldNotBe null
        withClue("a rejected activation doesn't sacrifice it") { d.state.getBattlefield().contains(hope) shouldBe true }
    }

    test("only the player it dealt combat damage to is a legal target") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val hope = d.putCreatureOnBattlefield(me, "Hope of Ghirapur")
        d.removeSummoningSickness(hope)

        d.connect(me, opp, hope)
        withClue("the player it never hit is still not a legal target") {
            d.sacrificeTargeting(me, hope, me).error shouldNotBe null
        }
    }

    test("last turn's combat damage doesn't count") {
        val d = setup()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val hope = d.putCreatureOnBattlefield(me, "Hope of Ghirapur")
        d.removeSummoningSickness(hope)

        d.connect(me, opp, hope)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.submit(PassPriority(opp)).error shouldBe null
        withClue("I hold priority in their main phase") { d.state.priorityPlayerId shouldBe me }
        d.sacrificeTargeting(me, hope, opp).error shouldNotBe null
    }
})
