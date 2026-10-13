package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.EDELonesomeEyebot
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * ED-E, Lonesome Eyebot — the intervening-if compares *all* attacking creatures with ED-E's quest
 * counters (ED-E needn't attack), and the sacrifice ability counts the quest counters ED-E had
 * when it was sacrificed to pay the cost.
 */
class EDELonesomeEyebotScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + EDELonesomeEyebot)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 40)
            d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
    }
    fun quest(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.QUEST) ?: 0
    fun ready(d: GameTestDriver, name: String) =
        d.putCreatureOnBattlefield(d.player1, name).also(d::removeSummoningSickness)

    test("attacking with more creatures than ED-E has quest counters adds one, even when ED-E stays home") {
        val d = setup()
        val ede = ready(d, EDELonesomeEyebot.name)
        val bears = ready(d, "Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(bears), d.player2).error shouldBe null
        resolve(d)
        quest(d, ede) shouldBe 1
    }

    test("no counter when the attackers don't outnumber the quest counters") {
        val d = setup()
        val ede = ready(d, EDELonesomeEyebot.name)
        d.addComponent(ede, CountersComponent().withAdded(CounterType.QUEST, 2))
        val bears = ready(d, "Grizzly Bears")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(ede, bears), d.player2).error shouldBe null
        d.state.stack.size shouldBe 0
        resolve(d)
        quest(d, ede) shouldBe 2
    }

    test("sacrificing ED-E draws one card plus one per quest counter it had") {
        val d = setup()
        val ede = d.putCreatureOnBattlefield(d.player1, EDELonesomeEyebot.name)
        d.addComponent(ede, CountersComponent().withAdded(CounterType.QUEST, 2))
        d.giveColorlessMana(d.player1, 2)
        val handBefore = d.getHandSize(d.player1)
        d.submit(ActivateAbility(d.player1, ede, EDELonesomeEyebot.activatedAbilities.single().id))
            .error shouldBe null
        (ede in d.state.getBattlefield()) shouldBe false
        resolve(d)
        d.getHandSize(d.player1) shouldBe handBefore + 3
    }
})
