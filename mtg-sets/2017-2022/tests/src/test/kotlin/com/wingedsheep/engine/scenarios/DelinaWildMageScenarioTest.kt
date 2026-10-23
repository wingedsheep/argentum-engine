package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.DelinaWildMage
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DelinaWildMageScenarioTest : FunSpec({

    /** First seed whose d20 comes up [natural]. */
    fun seedFor(natural: Int): Long = (0L..10_000L).first { GameRng.seeded(it).nextInt(20).first + 1 == natural }

    fun GameTestDriver.forceNextD20(natural: Int) = replaceState(state.copy(rng = GameRng.seeded(seedFor(natural))))

    val walker = card("Test Delina Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + DelinaWildMage + walker)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tokens(name: String): List<EntityId> =
        getPermanents(player1).filter {
            state.getEntity(it)?.get<CardComponent>()?.name == name && state.getEntity(it)!!.has<TokenComponent>()
        }

    /** Attack with Delina, target [copied] with her trigger, and resolve it on a d20 of [natural]. */
    fun GameTestDriver.attackAndRoll(delina: EntityId, copied: EntityId, natural: Int) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, mapOf(delina to getOpponent(player1)))
        pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        submitTargetSelection(player1, listOf(copied)).error shouldBe null
        forceNextD20(natural)
        bothPass()
    }

    fun GameTestDriver.setup(): Pair<EntityId, EntityId> {
        val delina = putCreatureOnBattlefield(player1, DelinaWildMage.name).also(::removeSummoningSickness)
        val bears = putCreatureOnBattlefield(player1, "Grizzly Bears")
        return delina to bears
    }

    test("1-14 makes one tapped, attacking, nonlegendary copy and offers no reroll") {
        val d = driver()
        val (delina, _) = d.setup()
        d.attackAndRoll(delina, copied = delina, natural = 9)

        d.pendingDecision shouldBe null
        val token = d.tokens(DelinaWildMage.name).single()
        withClue("the copy is tapped and attacking, but was never declared as an attacker") {
            d.state.getEntity(token)!!.has<TappedComponent>() shouldBe true
            d.state.getEntity(token)!!.get<AttackingComponent>().shouldNotBeNull()
        }
        withClue("it isn't legendary, so the legend rule leaves both Delinas") {
            d.state.projectedState.isLegendary(token) shouldBe false
            d.state.projectedState.isLegendary(delina) shouldBe true
            d.findPermanent(d.player1, DelinaWildMage.name).shouldNotBeNull()
        }
    }

    test("15-20 makes a token and may roll again; each pass copies the same creature until a low roll") {
        val d = driver()
        val (delina, bears) = d.setup()
        d.attackAndRoll(delina, copied = bears, natural = 17)

        d.tokens("Grizzly Bears") shouldHaveSize 1
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.forceNextD20(20)
        d.submitYesNo(d.player1, true)

        d.tokens("Grizzly Bears") shouldHaveSize 2
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.forceNextD20(3)
        d.submitYesNo(d.player1, true)

        withClue("a 3 makes the third token and ends the process without asking") {
            d.tokens("Grizzly Bears") shouldHaveSize 3
            d.pendingDecision shouldBe null
            d.state.stack.shouldBeEmpty()
        }
    }

    test("rolling again is optional") {
        val d = driver()
        val (delina, bears) = d.setup()
        d.attackAndRoll(delina, copied = bears, natural = 15)

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, false)

        d.pendingDecision shouldBe null
        d.tokens("Grizzly Bears") shouldHaveSize 1
    }

    test("each token's controller picks what it attacks as it enters, and a 15-20 still offers the reroll") {
        val d = driver()
        val (delina, bears) = d.setup()
        val opponent = d.getOpponent(d.player1)
        val theirWalker = d.putPermanentOnBattlefield(opponent, "Test Delina Walker").also { pw ->
            d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        }
        d.attackAndRoll(delina, copied = bears, natural = 17)

        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        choice.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(opponent, theirWalker)
        d.submitTargetSelection(d.player1, listOf(theirWalker)).error shouldBe null

        val token = d.tokens("Grizzly Bears").single()
        d.state.getEntity(token)!!.get<AttackingComponent>()!!.defenderId shouldBe theirWalker
        withClue("the roll survived the defender prompt, so the reroll is still offered") {
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        d.forceNextD20(9)
        d.submitYesNo(d.player1, true)
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, listOf(opponent)).error shouldBe null

        withClue("a 9 makes the second token, attacking the player, and ends the process") {
            d.pendingDecision shouldBe null
            d.tokens("Grizzly Bears").map { d.state.getEntity(it)!!.get<AttackingComponent>()!!.defenderId } shouldContainExactlyInAnyOrder
                listOf(theirWalker, opponent)
        }
    }

    test("the tokens are exiled at end of combat") {
        val d = driver()
        val (delina, bears) = d.setup()
        d.attackAndRoll(delina, copied = bears, natural = 4)
        d.tokens("Grizzly Bears") shouldHaveSize 1

        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.getOpponent(d.player1), emptyMap()).outcome shouldBe Outcome.Done
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.tokens("Grizzly Bears").shouldBeEmpty()
        withClue("the attacking token dealt its damage before leaving") {
            d.getLifeTotal(d.getOpponent(d.player1)) shouldBe 20 - 3 - 2
        }
    }
})
