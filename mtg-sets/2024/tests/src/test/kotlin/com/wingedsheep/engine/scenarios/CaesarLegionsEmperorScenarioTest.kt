package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.CaesarLegionsEmperor
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Caesar, Legion's Emperor — Fallout #1.
 *
 * Whenever you attack, you may sacrifice another creature. When you do, choose two —
 * • Create two 1/1 red and white Soldier creature tokens with haste that are tapped and attacking.
 * • You draw a card and you lose 1 life.
 * • Caesar deals damage equal to the number of creature tokens you control to target opponent.
 *
 * The Soldiers' defender is chosen per token as it enters (CR 508.4) — the engine-level rules of
 * that choice are pinned in `EnteringAttackDefenderChoiceTest`; these cover the card.
 */
class CaesarLegionsEmperorScenarioTest : FunSpec({

    val walker = card("Caesar Test Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun setup(opponentWalker: Boolean = false): Triple<GameTestDriver, EntityId, EntityId?> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(CaesarLegionsEmperor, walker))
        d.initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caesar = d.putCreatureOnBattlefield(d.player1, CaesarLegionsEmperor.name).also { d.removeSummoningSickness(it) }
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val pw = if (opponentWalker) d.putPermanentOnBattlefield(d.player2, walker.name).also { pw ->
            d.addComponent(pw, CountersComponent().withAdded(CounterType.LOYALTY, 3))
        } else null
        return Triple(d, caesar, pw)
    }

    fun GameTestDriver.resolveUntilDecision() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 50) passPriority(priorityPlayer!!).error shouldBe null
    }

    fun GameTestDriver.chooseMode(label: String) {
        val decision = pendingDecision as? ChooseOptionDecision ?: error("expected a mode question; got $pendingDecision")
        val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
        check(index >= 0) { "$label not offered; options=${decision.options}" }
        submitDecision(player1, OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
    }

    /** Attack with Caesar, accept the sacrifice of the Bears, and choose [modes]. */
    fun GameTestDriver.attackSacrificeAndChoose(caesar: EntityId, vararg modes: String) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(caesar), player2).error shouldBe null
        resolveUntilDecision()
        pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        submitYesNo(player1, true).error shouldBe null
        // The Bears are the only other creature, so the sacrifice choice may be made for us.
        val bears = findPermanent(player1, "Grizzly Bears")
        if (bears != null && pendingDecision is ChooseTargetsDecision) {
            submitTargetSelection(player1, listOf(bears)).error shouldBe null
        }
        modes.forEach { chooseMode(it) }
        (pendingDecision as? ChooseTargetsDecision)?.let { submitTargetSelection(player1, listOf(player2)).error shouldBe null }
        resolveUntilDecision()
    }

    fun GameTestDriver.soldiers(): List<EntityId> =
        getCreatures(player1).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.attackedBy(id: EntityId): EntityId? = state.getEntity(id)?.get<AttackingComponent>()?.defenderId

    test("tokens + damage: each Soldier's defender is chosen, then the damage counts both tokens") {
        val (d, caesar, pw) = setup(opponentWalker = true)
        d.attackSacrificeAndChoose(caesar, "Create two", "deals damage")

        withClue("the sacrificed creature is gone") { d.getGraveyardCardNames(d.player1) shouldContain "Grizzly Bears" }
        val first = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        first.legalTargets[0]!! shouldContainExactly listOf(d.player2, pw!!)
        d.soldiers() shouldHaveSize 0
        d.submitTargetSelection(d.player1, listOf(pw)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null
        d.resolveUntilDecision()

        val soldiers = d.soldiers()
        soldiers shouldHaveSize 2
        soldiers.map { d.attackedBy(it) } shouldContainExactly listOf(pw, d.player2)
        soldiers.forEach {
            d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true
            d.state.projectedState.hasKeyword(it, Keyword.HASTE) shouldBe true
            d.state.projectedState.getPower(it) shouldBe 1
        }
        withClue("damage equal to the two creature tokens, dealt after they were created") {
            d.getLifeTotal(d.player2) shouldBe 18
        }
        withClue("tokens put onto the battlefield attacking never 'attacked': Caesar doesn't retrigger") {
            d.pendingDecision shouldBe null
            d.stackSize shouldBe 0
        }

        d.passPriorityUntil(Step.END_COMBAT)
        withClue("one Soldier dealt combat damage to the planeswalker") {
            d.state.getEntity(pw)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 2
        }
        withClue("Caesar (4) and the other Soldier (1) hit the player") { d.getLifeTotal(d.player2) shouldBe 13 }
    }

    test("tokens with only the defending player to attack ask nothing; draw a card and lose 1 life") {
        val (d, caesar, _) = setup()
        val handBefore = d.getHandSize(d.player1)
        d.attackSacrificeAndChoose(caesar, "Create two", "draw a card")

        d.pendingDecision shouldBe null
        d.soldiers().map { d.attackedBy(it) } shouldContainExactly listOf(d.player2, d.player2)
        d.getHandSize(d.player1) shouldBe handBefore + 1
        d.getLifeTotal(d.player1) shouldBe 19
    }

    test("damage without the token mode counts only creature tokens already there") {
        val (d, caesar, _) = setup()
        val existingToken = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        d.addComponent(existingToken, TokenComponent)
        d.attackSacrificeAndChoose(caesar, "draw a card", "deals damage")

        withClue("no Soldiers were created; only the token already there remains") {
            d.soldiers() shouldContainExactly listOf(existingToken)
        }
        withClue("one creature token you control: 1 damage") { d.getLifeTotal(d.player2) shouldBe 19 }
        d.getLifeTotal(d.player1) shouldBe 19
    }

    test("declining the sacrifice: nothing is sacrificed and no modes are chosen") {
        val (d, caesar, _) = setup()
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(caesar), d.player2).error shouldBe null
        d.resolveUntilDecision()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.resolveUntilDecision()

        d.pendingDecision shouldBe null
        d.findPermanent(d.player1, "Grizzly Bears") shouldNotBe null
        d.soldiers() shouldHaveSize 0
    }
})
