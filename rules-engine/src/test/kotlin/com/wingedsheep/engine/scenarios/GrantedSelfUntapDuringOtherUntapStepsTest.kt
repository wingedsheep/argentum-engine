package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [UntapSelfDuringOtherUntapSteps] is read off each permanent's *projected* statics by the untap
 * step, so a permanent granted it by a [GrantStaticAbility] (Urban Burgeoning's enchanted land)
 * untaps during other players' untap steps exactly like one printed with it (Endbringer), and a
 * permanent that loses all its abilities — or whose granter does — stops untapping.
 */
class GrantedSelfUntapDuringOtherUntapStepsTest : FunSpec({
    val printed = card("Printed Self Untapper") {
        typeLine = "Artifact"
        staticAbility { ability = UntapSelfDuringOtherUntapSteps }
    }
    val granter = card("Land Untap Granter") {
        typeLine = "Artifact"
        staticAbility {
            ability = GrantStaticAbility(UntapSelfDuringOtherUntapSteps, GroupFilter(GameObjectFilter.Land.youControl()))
        }
    }
    val mute = card("Self Untap Mute") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val t = target(TargetFilter.Permanent); effect = Effects.RemoveAllAbilities(t, Duration.Permanent) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(printed, granter, mute))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.toOpponentsUpkeep() {
        val me = activePlayer!!
        passPriorityUntil(Step.UPKEEP)
        (activePlayer == getOpponent(me)) shouldBe true
    }

    test("a granted self-untap untaps the holder during another player's untap step") {
        val d = driver(); val me = d.activePlayer!!
        d.putPermanentOnBattlefield(me, granter.name)
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.tapPermanent(forest); d.tapPermanent(bears)
        d.toOpponentsUpkeep()
        d.isTapped(forest) shouldBe false
        d.isTapped(bears) shouldBe true // not a land, so not granted the ability
    }

    test("the grant ends when the granter loses its abilities") {
        val d = driver(); val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, granter.name)
        val forest = d.putPermanentOnBattlefield(me, "Forest")
        d.castSpell(me, d.putCardInHand(me, mute.name), listOf(source)).error shouldBe null
        d.bothPass()
        d.tapPermanent(forest)
        d.toOpponentsUpkeep()
        d.isTapped(forest) shouldBe true
    }

    test("a printed self-untap stops working once its holder loses all abilities") {
        val d = driver(); val me = d.activePlayer!!
        val untapper = d.putPermanentOnBattlefield(me, printed.name)
        d.castSpell(me, d.putCardInHand(me, mute.name), listOf(untapper)).error shouldBe null
        d.bothPass()
        d.tapPermanent(untapper)
        d.toOpponentsUpkeep()
        d.isTapped(untapper) shouldBe true
    }

    test("a printed self-untap still untaps its holder") {
        val d = driver(); val me = d.activePlayer!!
        val untapper = d.putPermanentOnBattlefield(me, printed.name)
        d.tapPermanent(untapper)
        d.toOpponentsUpkeep()
        d.isTapped(untapper) shouldBe false
    }
})
