package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Myriad (CR 702.116), the engine-live keyword: "Whenever this creature attacks, for each opponent
 * other than defending player, you may create a token that's a copy of this creature that's tapped
 * and attacking that player or a planeswalker they control. If one or more tokens are created this
 * way, exile the tokens at end of combat." (702.116a) — and each instance triggers separately
 * (702.116b).
 *
 * "That player or a planeswalker they control" is the copy's controller's choice as it enters
 * (CR 508.4), asked only when the visited opponent controls a planeswalker.
 */
class MyriadTest : FunSpec({

    val myriadBear = card("Test Myriad Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.MYRIAD)
    }

    val doubleMyriad = card("Test Double Myriad Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbilities(KeywordAbility.Simple(Keyword.MYRIAD), KeywordAbility.Simple(Keyword.MYRIAD))
    }

    val walker = card("Test Myriad Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    fun driver(players: Int): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(myriadBear, doubleMyriad, walker))
        val ids = d.initMultiplayer(List(players) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to ids
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    /** Pass priority until a decision is pending or the stack is empty. */
    fun GameTestDriver.resolveUntilDecision() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 50) {
            passPriority(priorityPlayer!!)
        }
    }

    /** Answer every myriad yes/no in turn with [answers], resolving the stack between them. */
    fun GameTestDriver.answerMyriad(controller: EntityId, vararg answers: Boolean) {
        for (answer in answers) {
            resolveUntilDecision()
            val decision = pendingDecision
            withClue("expected a myriad yes/no") { (decision is YesNoDecision) shouldBe true }
            withClue("the source's controller decides, not the visited opponent") {
                decision!!.playerId shouldBe controller
            }
            submitYesNo(controller, answer)
        }
        resolveUntilDecision()
    }

    fun GameTestDriver.walkerFor(player: EntityId): EntityId =
        putPermanentOnBattlefield(player, "Test Myriad Walker").also { pw ->
            replaceState(state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        }

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.attackedBy(token: EntityId): EntityId? =
        state.getEntity(token)?.get<AttackingComponent>()?.defenderId

    test("a copy for each opponent other than defending player, tapped and attacking that player") {
        val (d, p) = driver(4)
        val (p1, p2, p3, p4) = p
        val bear = d.ready(p1, "Test Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2).error shouldBe null
        d.answerMyriad(p1, true, true)

        val tokens = d.tokensOf(p1)
        tokens shouldHaveSize 2
        tokens.map { d.attackedBy(it) } shouldContainExactlyInAnyOrder listOf(p3, p4)
        tokens.forEach { token ->
            withClue("the copy enters tapped") { d.state.getEntity(token)!!.has<TappedComponent>() shouldBe true }
            d.getCardName(token) shouldBe "Test Myriad Bear"
        }
        withClue("the original still attacks the defending player") { d.attackedBy(bear) shouldBe p2 }
    }

    test("each copy is optional — declining one opponent skips only that opponent") {
        val (d, p) = driver(4)
        val (p1, p2, _, p4) = p
        val bear = d.ready(p1, "Test Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2)
        // APNAP order visits p3 first, then p4.
        d.answerMyriad(p1, false, true)

        val tokens = d.tokensOf(p1)
        tokens shouldHaveSize 1
        d.attackedBy(tokens.single()) shouldBe p4
    }

    test("the tokens are exiled at end of combat") {
        val (d, p) = driver(3)
        val (p1, p2, _) = p
        val bear = d.ready(p1, "Test Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2)
        d.answerMyriad(p1, true)
        d.tokensOf(p1) shouldHaveSize 1

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.tokensOf(p1).shouldBeEmpty()
        withClue("the original stays") { d.findPermanent(p1, "Test Myriad Bear") shouldNotBe null }
    }

    test("copies enter attacking, so they never 'attacked' and their own myriad doesn't trigger (CR 508.4)") {
        val (d, p) = driver(3)
        val (p1, p2, _) = p
        val bear = d.ready(p1, "Test Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2)
        d.answerMyriad(p1, true)

        d.pendingDecision shouldBe null
        d.stackSize shouldBe 0
        d.tokensOf(p1) shouldHaveSize 1
    }

    test("two-player game: the only opponent is the defending player, so nothing is offered") {
        val (d, p) = driver(2)
        val (p1, p2) = p
        val bear = d.ready(p1, "Test Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2)
        d.resolveUntilDecision()

        d.pendingDecision shouldBe null
        d.tokensOf(p1).shouldBeEmpty()
    }

    test("each instance triggers separately (CR 702.116b)") {
        val (d, p) = driver(3)
        val (p1, p2, p3) = p
        val bear = d.ready(p1, "Test Double Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2)
        d.answerMyriad(p1, true, true)

        val tokens = d.tokensOf(p1)
        tokens shouldHaveSize 2
        tokens.forEach { d.attackedBy(it) shouldBe p3 }
    }

    test("a specified defender who isn't one of the creator's opponents gets no attacker (CR 508.4a)") {
        val (d, p) = driver(3)
        val (p1, _, _) = p
        val bear = d.ready(p1, "Test Myriad Bear")
        val effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.Self,
            tapped = true,
            attacking = true,
            attackedPlayer = EffectTarget.PlayerRef(Player.You),
        )

        val result = d.services.effectExecutorRegistry.execute(
            d.state, effect, EffectContext(sourceId = bear, controllerId = p1)
        )
        d.replaceState(result.state)

        val token = d.tokensOf(p1).single()
        withClue("the copy still enters tapped") { d.state.getEntity(token)!!.has<TappedComponent>() shouldBe true }
        withClue("but never attacking its own controller") { d.attackedBy(token) shouldBe null }
    }

    test("a copy may attack a planeswalker that opponent controls — the attacker picks, per opponent") {
        val (d, p) = driver(4)
        val (p1, p2, p3, p4) = p
        val p3Walker = d.walkerFor(p3)
        val p4Walker = d.walkerFor(p4)
        d.walkerFor(p2)
        val bear = d.ready(p1, "Test Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2).error shouldBe null
        d.resolveUntilDecision()
        d.submitYesNo(p1, true)

        val first = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        first.playerId shouldBe p1
        withClue("APNAP visits p3 first: p3 or p3's planeswalker, nobody else's") {
            first.legalTargets[0]!! shouldContainExactly listOf(p3, p3Walker)
        }
        d.submitTargetSelection(p1, listOf(p3Walker)).error shouldBe null

        withClue("the loop carries on to p4 after the pause") {
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe p1
        }
        d.submitYesNo(p1, true)
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets[0]!! shouldContainExactly listOf(p4, p4Walker)
        d.submitTargetSelection(p1, listOf(p4)).error shouldBe null
        d.resolveUntilDecision()

        d.pendingDecision shouldBe null
        val tokens = d.tokensOf(p1)
        withClue("one copy per other opponent, none created twice across the pauses") {
            tokens.map { d.attackedBy(it) } shouldContainExactlyInAnyOrder listOf(p3Walker, p4)
        }
        val onWalker = tokens.single { d.attackedBy(it) == p3Walker }
        withClue("a planeswalker's controller is the defending player") {
            d.state.getEntity(onWalker)!!.get<AttackingComponent>()!!.defendingPlayerId shouldBe p3
        }
    }

    test("an opponent without a planeswalker is attacked directly, no question asked") {
        val (d, p) = driver(3)
        val (p1, p2, p3) = p
        d.walkerFor(p2)
        val bear = d.ready(p1, "Test Myriad Bear")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bear), p2)
        d.answerMyriad(p1, true)

        d.pendingDecision shouldBe null
        d.attackedBy(d.tokensOf(p1).single()) shouldBe p3
    }
})
