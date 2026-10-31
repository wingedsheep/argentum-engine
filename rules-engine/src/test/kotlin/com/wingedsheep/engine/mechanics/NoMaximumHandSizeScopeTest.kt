package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [NoMaximumHandSize]'s player scope — "Players have no maximum hand size" (Folio of Fancies) —
 * resolved relative to the holder's controller, in CR 613.11 timestamp order with the other
 * maximum-hand-size effects, and read off projection so a granted scoped one counts too.
 */
class NoMaximumHandSizeScopeTest : FunSpec({
    val everyone = card("Test Everyone No Max") {
        typeLine = "Artifact"
        staticAbility { ability = NoMaximumHandSize(Player.Each) }
    }
    val tower = card("Test Scope Controller No Max") {
        typeLine = "Artifact"
        staticAbility { ability = NoMaximumHandSize() }
    }
    val opponents = card("Test Opponents No Max") {
        typeLine = "Artifact"
        staticAbility { ability = NoMaximumHandSize(Player.EachOpponent) }
    }
    val twenty = card("Test Scope Everyone Twenty") {
        typeLine = "Enchantment"
        staticAbility { ability = SetMaximumHandSize(Player.Each, DynamicAmount.Fixed(20)) }
    }
    val lord = card("Test Scope No Max Lord") {
        typeLine = "Enchantment"
        staticAbility {
            ability = GrantStaticAbility(
                NoMaximumHandSize(Player.Each),
                filter = GroupFilter(GameObjectFilter.Creature.youControl())
            )
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(tower, everyone, opponents, twenty, lord))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.max(playerId: EntityId): Int? = MaximumHandSize.effective(
        state, playerId, cardRegistry, services.conditionEvaluator, services.dynamicAmountEvaluator
    )

    fun GameTestDriver.put(controller: EntityId, name: String, timestamp: Long): EntityId =
        putPermanentOnBattlefield(controller, name).also {
            addComponent(it, BattlefieldEntryTimestampComponent(timestamp))
        }

    test("the default scope is still only the controller") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, tower.name, 1)
        d.max(me) shouldBe null
        d.max(opp) shouldBe 7
    }

    test("Player.Each frees every player") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, everyone.name, 1)
        d.max(me) shouldBe null
        d.max(opp) shouldBe null
    }

    test("Player.EachOpponent frees the opponents and leaves the controller at seven") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, opponents.name, 1)
        d.max(me) shouldBe 7
        d.max(opp) shouldBe null
    }

    test("a scoped no-maximum joins timestamp order with a set effect") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, everyone.name, 1)
        d.put(opp, twenty.name, 2)
        d.max(opp) shouldBe 20

        val d2 = driver(); val me2 = d2.activePlayer!!; val opp2 = d2.getOpponent(me2)
        d2.put(opp2, twenty.name, 1)
        d2.put(me2, everyone.name, 2)
        d2.max(opp2) shouldBe null
    }

    test("a granted scoped no-maximum counts for every player while the grant holds") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.put(me, lord.name, 1)
        d.max(opp) shouldBe 7
        d.put(me, "Grizzly Bears", 2)
        d.max(opp) shouldBe null
        d.max(me) shouldBe null
    }
})
