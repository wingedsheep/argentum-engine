package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Which untap restrictions apply to "untap … during each other player's untap step" statics
 * (Seedborn Muse, Unwinding Clock, Bender's Waterskin).
 *
 * `DOESNT_UNTAP` reads "doesn't untap during its controller's untap step", so it does not stop
 * those untaps (Seedborn Muse / Unwinding Clock rulings). `CANT_BECOME_UNTAPPED` blocks every untap,
 * and a stun counter still replaces the untap.
 */
class OtherPlayersUntapStepRestrictionsTest : FunSpec({

    val AllMuse = card("Restriction Test All Muse") {
        manaCost = "{3}{G}{G}"
        typeLine = "Creature — Spirit"
        power = 2
        toughness = 4
        staticAbility { ability = UntapDuringOtherUntapSteps() }
    }
    val ArtifactClock = card("Restriction Test Artifact Clock") {
        manaCost = "{4}"
        typeLine = "Artifact"
        staticAbility { ability = UntapFilteredDuringOtherUntapSteps(filter = GameObjectFilter.Artifact) }
    }
    val SelfSkin = card("Restriction Test Self Skin") {
        manaCost = "{3}"
        typeLine = "Artifact"
        flags(AbilityFlag.DOESNT_UNTAP)
        staticAbility { ability = UntapSelfDuringOtherUntapSteps }
    }
    val DoesntUntap = card("Restriction Test Doesnt Untap") {
        manaCost = "{2}"
        typeLine = "Artifact"
        flags(AbilityFlag.DOESNT_UNTAP)
    }
    val CantUntap = card("Restriction Test Cant Untap") {
        manaCost = "{2}"
        typeLine = "Artifact"
        flags(AbilityFlag.CANT_BECOME_UNTAPPED)
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AllMuse, ArtifactClock, SelfSkin, DoesntUntap, CantUntap))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    /** Put [granter] and the two restricted artifacts down for the active player, tap them, pass to the opponent. */
    fun runOtherUntapStep(granter: String?): Triple<GameTestDriver, EntityId, EntityId> {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (granter != null) d.putPermanentOnBattlefield(you, granter)
        val doesnt = d.putPermanentOnBattlefield(you, "Restriction Test Doesnt Untap")
        val cant = d.putPermanentOnBattlefield(you, "Restriction Test Cant Untap")
        d.tapPermanent(doesnt)
        d.tapPermanent(cant)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.getOpponent(you)
        return Triple(d, doesnt, cant)
    }

    test("untap-all (Seedborn Muse) untaps a DOESNT_UNTAP permanent but not a CANT_BECOME_UNTAPPED one") {
        val (d, doesnt, cant) = runOtherUntapStep("Restriction Test All Muse")
        d.isTapped(doesnt) shouldBe false
        d.isTapped(cant) shouldBe true
    }

    test("filtered untap (Unwinding Clock) untaps a DOESNT_UNTAP permanent but not a CANT_BECOME_UNTAPPED one") {
        val (d, doesnt, cant) = runOtherUntapStep("Restriction Test Artifact Clock")
        d.isTapped(doesnt) shouldBe false
        d.isTapped(cant) shouldBe true
    }

    test("without a granter both stay tapped through another player's untap step") {
        val (d, doesnt, cant) = runOtherUntapStep(null)
        d.isTapped(doesnt) shouldBe true
        d.isTapped(cant) shouldBe true
    }

    test("self untap untaps its own source even when it has DOESNT_UNTAP") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val skin = d.putPermanentOnBattlefield(you, "Restriction Test Self Skin")
        d.tapPermanent(skin)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.getOpponent(you)
        d.isTapped(skin) shouldBe false
    }

    test("a stun counter still replaces the other-player's-step untap") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(you, "Restriction Test Artifact Clock")
        val doesnt = d.putPermanentOnBattlefield(you, "Restriction Test Doesnt Untap")
        d.tapPermanent(doesnt)
        d.replaceState(d.state.updateEntity(doesnt) { it.with(CountersComponent().withAdded(CounterType.STUN, 1)) })
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.getOpponent(you)
        d.isTapped(doesnt) shouldBe true
        d.state.getEntity(doesnt)?.get<CountersComponent>()?.getCount(CounterType.STUN) shouldBe 0
    }
})
