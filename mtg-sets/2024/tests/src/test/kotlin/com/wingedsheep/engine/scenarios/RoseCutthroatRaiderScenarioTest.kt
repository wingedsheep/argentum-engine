package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.RoseCutthroatRaider
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RoseCutthroatRaiderScenarioTest : FunSpec({
    val walker = card("Rose Walker Probe") { typeLine = "Planeswalker — Test" }
    val outlet = card("Junk Sacrifice Probe") {
        typeLine = "Artifact — Junk"
        activatedAbility { cost = Costs.SacrificeSelf; effect = Effects.GainLife(1) }
    }
    fun setup(teamGame: Boolean = false): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(RoseCutthroatRaider, PredefinedTokens.Junk, walker, outlet))
        initMultiplayer(
            List(4) { Deck.of("Mountain" to 40) },
            format = if (teamGame) Format.TwoHeadedGiant() else Format.Standard,
            teams = if (teamGame) listOf(listOf(0, 1), listOf(2, 3)) else null,
        )
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 40) d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
        d.state.stack.size shouldBe 0
    }
    fun junk(d: GameTestDriver) = d.getPermanents(d.player1).filter { d.getCardName(it) == "Junk" }
    fun attack(d: GameTestDriver, targets: List<EntityId>) {
        val attackers = targets.map { d.putCreatureOnBattlefield(d.player1, "Grizzly Bears").also(d::removeSummoningSickness) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, attackers.zip(targets).toMap()).error shouldBe null
    }
    fun endCombat(d: GameTestDriver) { d.passPriorityUntil(Step.END_COMBAT); resolve(d) }
    test("Rose creates one Junk for each distinct opponent attacked even when Rose did not attack") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, RoseCutthroatRaider.name)
        attack(d, listOf(d.player2, d.player2, d.state.turnOrder[2]))
        endCombat(d)
        junk(d).size shouldBe 2
    }
    test("Rose entering after attacks still sees the whole turn") {
        val d = setup()
        attack(d, listOf(d.player2))
        d.putCreatureOnBattlefield(d.player1, RoseCutthroatRaider.name)
        endCombat(d)
        junk(d).size shouldBe 1
    }
    test("no attack means no Raid trigger") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, RoseCutthroatRaider.name)
        d.passPriorityUntil(Step.END_COMBAT)
        d.state.stack.size shouldBe 0
        junk(d).size shouldBe 0
    }
    test("attacking only a planeswalker satisfies Raid but creates zero Junk") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, RoseCutthroatRaider.name)
        val pw = d.putPermanentOnBattlefield(d.player2, walker.name)
        d.addComponent(pw, CountersComponent().withAdded(CounterType.LOYALTY, 20))
        attack(d, listOf(pw))
        d.passPriorityUntil(Step.END_COMBAT)
        d.state.stack.size shouldBe 1
        resolve(d)
        junk(d).size shouldBe 0
    }
    test("a queued Raid trigger survives Rose leaving") {
        val d = setup()
        val rose = d.putCreatureOnBattlefield(d.player1, RoseCutthroatRaider.name)
        attack(d, listOf(d.player2))
        d.passPriorityUntil(Step.END_COMBAT)
        d.replaceState(d.state.removeEntity(rose))
        resolve(d)
        junk(d).size shouldBe 1
    }
    test("Junk activation uses the stack, triggers red mana, and exiles a playable land") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, RoseCutthroatRaider.name)
        val token = d.putPermanentOnBattlefield(d.player1, "Junk")
        val land = d.putCardOnTopOfLibrary(d.player1, "Mountain")
        d.submit(ActivateAbility(d.player1, token, PredefinedTokens.Junk.activatedAbilities[0].id)).error shouldBe null
        d.state.stack.size shouldBe 2
        resolve(d)
        d.state.getEntity(d.player1)?.get<ManaPoolComponent>()?.red shouldBe 1
        (land in d.state.getZone(d.player1, Zone.EXILE)) shouldBe true
        d.legalActions(d.player1).any { (it.action as? PlayLand)?.cardId == land } shouldBe true
        d.submit(PlayLand(d.player1, land)).error shouldBe null
    }
    test("sacrificing a nontoken Junk for another ability also triggers Rose") {
        val d = setup()
        d.putCreatureOnBattlefield(d.player1, RoseCutthroatRaider.name)
        val source = d.putPermanentOnBattlefield(d.player1, outlet.name)
        d.submit(ActivateAbility(d.player1, source, outlet.activatedAbilities[0].id)).error shouldBe null
        resolve(d)
        d.state.getEntity(d.player1)?.get<ManaPoolComponent>()?.red shouldBe 1
    }
    test("Junk cannot activate outside sorcery timing") {
        val d = setup()
        val token = d.putPermanentOnBattlefield(d.player1, "Junk")
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.legalActions(d.player1).any { (it.action as? ActivateAbility)?.sourceId == token } shouldBe false
    }
    for (ownAttack in listOf(false, true)) test("2HG teammate attacks ${if (ownAttack) "do not increase your Junk count" else "do not enable your Raid"}") {
        val d = setup(teamGame = true)
        val p = d.state.turnOrder
        d.putCreatureOnBattlefield(p[0], RoseCutthroatRaider.name)
        val teammate = d.putCreatureOnBattlefield(p[1], "Grizzly Bears").also(d::removeSummoningSickness)
        val assignments = mutableMapOf(teammate to p[3])
        if (ownAttack) assignments[d.putCreatureOnBattlefield(p[0], "Grizzly Bears").also(d::removeSummoningSickness)] = p[2]
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], assignments).error shouldBe null
        endCombat(d)
        junk(d).size shouldBe if (ownAttack) 1 else 0
    }

})
