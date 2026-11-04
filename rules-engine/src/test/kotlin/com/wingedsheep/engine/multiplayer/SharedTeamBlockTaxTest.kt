package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.BlockTax
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SharedTeamBlockTaxTest : FunSpec({
    val tax = card("Shared Team Block Tax") {
        typeLine = "Enchantment"
        staticAbility { ability = BlockTax(DynamicAmounts.fixed(1)) }
    }

    data class Fixture(
        val driver: GameTestDriver,
        val players: List<EntityId>,
        val blockers: List<EntityId>,
        val lands: List<EntityId>,
        val declaration: Map<EntityId, List<EntityId>>,
    )

    fun fixture(secondPlayerHasMana: Boolean = true): Fixture {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + tax)
        val players = d.initMultiplayer(
            decks = List(4) { Deck.of("Mountain" to 40) },
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(players[0], tax.name)
        val attackers = List(2) { d.putCreatureOnBattlefield(players[0], "Centaur Courser") }
        val blockers = listOf(
            d.putCreatureOnBattlefield(players[2], "Centaur Courser"),
            d.putCreatureOnBattlefield(players[3], "Centaur Courser"),
        )
        val lands = listOf(
            d.putPermanentOnBattlefield(players[2], "Mountain"),
            d.putPermanentOnBattlefield(if (secondPlayerHasMana) players[3] else players[2], "Mountain"),
        )
        attackers.forEach { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(players[0], attackers, players[2]).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        return Fixture(d, players, blockers, lands, blockers.zip(attackers.map { listOf(it) }).toMap())
    }

    fun Fixture.assertUncommitted() {
        lands.forEach { driver.isTapped(it) shouldBe false }
        blockers.forEach { driver.state.getEntity(it)!!.has<BlockingComponent>() shouldBe false }
        players.drop(2).forEach {
            driver.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe false
        }
    }

    test("each blocker controller confirms and pays only their own share of the combined declaration") {
        val f = fixture()
        val d = f.driver
        d.declareBlockers(f.players[2], f.declaration).error shouldBe null
        val first = d.pendingDecision as SelectManaSourcesDecision
        first.playerId shouldBe f.players[2]
        first.requiredCost shouldBe "{1}"
        first.availableSources.map { it.entityId } shouldBe listOf(f.lands[0])
        f.assertUncommitted()

        d.submitManaAutoPayOrDecline(f.players[2], true).error shouldBe null
        val second = d.pendingDecision as SelectManaSourcesDecision
        second.playerId shouldBe f.players[3]
        second.requiredCost shouldBe "{1}"
        second.availableSources.map { it.entityId } shouldBe listOf(f.lands[1])
        f.assertUncommitted()

        d.submitManaAutoPayOrDecline(f.players[3], true).error shouldBe null
        d.pendingDecision shouldBe null
        f.lands.forEach { d.isTapped(it) shouldBe true }
        f.declaration.forEach { (blocker, attackers) ->
            d.state.getEntity(blocker)!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe attackers
        }
        f.players.drop(2).forEach {
            d.state.getEntity(it)!!.has<BlockersDeclaredThisCombatComponent>() shouldBe true
        }
    }

    test("declining the second controller's tax leaves the first controller's mana and all blockers untouched") {
        val f = fixture()
        val d = f.driver
        d.declareBlockers(f.players[2], f.declaration).error shouldBe null
        d.submitManaAutoPayOrDecline(f.players[2], true).error shouldBe null
        (d.pendingDecision as SelectManaSourcesDecision).playerId shouldBe f.players[3]
        d.submitManaAutoPayOrDecline(f.players[3], false).error shouldBe null
        d.pendingDecision shouldBe null
        f.assertUncommitted()
    }

    test("a defender cannot select their teammate's spare mana source for their own block tax") {
        val f = fixture(secondPlayerHasMana = false)
        val d = f.driver
        d.declareBlockers(f.players[2], f.declaration).error shouldBe null
        d.submitManaAutoPayOrDecline(f.players[2], true).error shouldBe null
        val second = d.pendingDecision as SelectManaSourcesDecision
        second.playerId shouldBe f.players[3]
        second.availableSources shouldBe emptyList()
        d.submitDecision(
            f.players[3], ManaSourcesSelectedResponse(second.id, listOf(f.lands[1]), autoPay = false)
        ).error shouldNotBe null
        f.assertUncommitted()
        d.submitManaAutoPayOrDecline(f.players[3], false).error shouldBe null
        f.assertUncommitted()
    }
})
