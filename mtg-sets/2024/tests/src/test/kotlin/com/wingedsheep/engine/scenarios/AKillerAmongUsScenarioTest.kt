package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.NotedCreatureTypesComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.AKillerAmongUs
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A Killer Among Us — {4}{G} Enchantment.
 *
 * Three suspects, one secret. What has to be true:
 *  - the enters trigger makes exactly the three 1/1 tokens, then offers *only* Human / Merfolk /
 *    Goblin as the choice, and the note it leaves is secret to the chooser;
 *  - a target of the chosen type gets three +1/+1 counters and deathtouch;
 *  - a target of a *different* type is still a legal target and simply gets nothing (the card's
 *    ruling — the type is a condition on the effect, not on the target);
 *  - the chosen type survives the enchantment's own sacrifice, which is paid at the same time;
 *  - a player who gained control of the enchantment can't activate it at all, because they can't
 *    reveal a choice they never made.
 */
class AKillerAmongUsScenarioTest : FunSpec({

    val abilityId = AKillerAmongUs.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AKillerAmongUs)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the enchantment and resolve its enters trigger, stopping on the secret choice. */
    fun GameTestDriver.playKiller(): EntityId {
        val card = putCardInHand(player1, "A Killer Among Us")
        giveMana(player1, Color.GREEN, 5)
        castSpell(player1, card).isSuccess shouldBe true
        bothPass()
        bothPass()
        return card
    }

    /** Answer the pending secret choice with [type]. */
    fun GameTestDriver.chooseType(type: String) {
        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(player1, OptionChosenResponse(decision.id, decision.options.indexOf(type)))
            .error.shouldBeNull()
    }

    fun GameTestDriver.tokenNamed(name: String): EntityId =
        getPermanents(player1).single { getCardName(it) == name }

    /** Advance to this player's next combat and swing with [attackers]. */
    fun GameTestDriver.attackWith(attackers: List<EntityId>) {
        passPriorityUntil(Step.UPKEEP)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.UPKEEP)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, attackers, player2).isSuccess shouldBe true
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("the enters trigger makes the three 1/1 suspects, then offers only their three types") {
        val driver = newDriver()
        driver.playKiller()

        val projected = driver.state.projectedState
        for (name in listOf("Human", "Merfolk", "Goblin")) {
            val token = driver.tokenNamed(name)
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
        }

        // "Then secretly choose Human, Merfolk, or Goblin" — those three and nothing else.
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options.sorted() shouldContainExactly listOf("Goblin", "Human", "Merfolk")
    }

    test("the choice is noted on the enchantment, secret to the player who made it") {
        val driver = newDriver()
        val killer = driver.playKiller()
        driver.chooseType("Merfolk")

        val noted = driver.state.getEntity(killer)?.get<NotedCreatureTypesComponent>()
        noted?.types shouldContainExactly setOf("Merfolk")
        noted?.secretTo shouldBe driver.player1
        noted?.isVisibleTo(driver.player2) shouldBe false
    }

    test("an attacking token of the chosen type gets three +1/+1 counters and deathtouch") {
        val driver = newDriver()
        val killer = driver.playKiller()
        driver.chooseType("Goblin")

        val goblin = driver.tokenNamed("Goblin")
        driver.attackWith(listOf(goblin))

        driver.submitSuccess(
            ActivateAbility(driver.player1, killer, abilityId, listOf(ChosenTarget.Permanent(goblin)))
        )
        driver.bothPass()

        driver.plusOneCounters(goblin) shouldBe 3
        driver.state.projectedState.hasKeyword(goblin, Keyword.DEATHTOUCH) shouldBe true
        // The enchantment paid for itself.
        driver.state.getBattlefield().contains(killer) shouldBe false
    }

    test("an attacking token of the wrong type is a legal target that gets nothing") {
        val driver = newDriver()
        val killer = driver.playKiller()
        driver.chooseType("Goblin")

        val human = driver.tokenNamed("Human")
        driver.attackWith(listOf(human))

        // Targeting it is legal — only the payoff is conditional (the card's own ruling).
        driver.submitSuccess(
            ActivateAbility(driver.player1, killer, abilityId, listOf(ChosenTarget.Permanent(human)))
        )
        driver.bothPass()

        driver.plusOneCounters(human) shouldBe 0
        driver.state.projectedState.hasKeyword(human, Keyword.DEATHTOUCH) shouldBe false
        // The cost was still paid in full — the enchantment is gone either way.
        driver.state.getBattlefield().contains(killer) shouldBe false
    }

    test("paying the reveal cost makes the note public") {
        val driver = newDriver()
        val killer = driver.playKiller()
        driver.chooseType("Merfolk")

        val merfolk = driver.tokenNamed("Merfolk")
        driver.attackWith(listOf(merfolk))
        driver.submitSuccess(
            ActivateAbility(driver.player1, killer, abilityId, listOf(ChosenTarget.Permanent(merfolk)))
        )

        // The reveal happens when the cost is paid — before the ability resolves — and the note
        // is public from that moment. The enchantment is in the graveyard, still carrying it.
        val noted = driver.state.getEntity(killer)?.get<NotedCreatureTypesComponent>()
        noted?.secretTo.shouldBeNull()
        noted?.types shouldContainExactly setOf("Merfolk")
    }

    test("a player who gained control can't activate it — they never saw the choice") {
        val driver = newDriver()
        val killer = driver.playKiller()
        driver.chooseType("Human")

        val human = driver.tokenNamed("Human")
        driver.attackWith(listOf(human))

        // The chooser can activate it right now.
        driver.legalActions(driver.player1)
            .any { (it.action as? ActivateAbility)?.sourceId == killer } shouldBe true

        // Hand the enchantment to the opponent without touching the note (CR 702.106b: the piece
        // of paper stays with the object, but only its writer can read it out).
        driver.replaceState(
            driver.state.updateEntity(killer) { c -> c.with(ControllerComponent(driver.player2)) }
        )

        driver.legalActions(driver.player2)
            .none { (it.action as? ActivateAbility)?.sourceId == killer } shouldBe true
    }
})
