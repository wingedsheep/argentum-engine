package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AssimilationAegis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Assimilation Aegis (OTJ mythic Equipment), {1}{W}{U}.
 *
 * When this Equipment enters, exile up to one target creature until this Equipment leaves the
 * battlefield.
 * Whenever this Equipment becomes attached to a creature, for as long as this Equipment remains
 * attached to it, that creature becomes a copy of a creature card exiled with this Equipment.
 * Equip {2}
 *
 * Exercises the new "becomes attached" trigger (SELF binding) and
 * [com.wingedsheep.sdk.scripting.effects.BecomeCopyOfLinkedExileEffect] — the equipped creature
 * becomes a copy of the exiled card while attached, and reverts when the Equipment leaves.
 */
class AssimilationAegisScenarioTest : FunSpec({

    // Destroy-target-artifact removal used to prove the copy reverts when the Aegis leaves (real LTB).
    val shatter = com.wingedsheep.sdk.dsl.card("Test Shatter") {
        manaCost = "{1}{U}"
        colorIdentity = "U"
        typeLine = "Instant"
        oracleText = "Destroy target artifact."
        spell {
            val t = target(
                "target artifact",
                com.wingedsheep.sdk.scripting.targets.TargetPermanent(
                    filter = com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Artifact
                )
            )
            effect = com.wingedsheep.sdk.dsl.Effects.Destroy(t)
        }
        metadata { rarity = com.wingedsheep.sdk.model.Rarity.COMMON; collectorNumber = "9" }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AssimilationAegis)
        driver.registerCard(shatter)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.equipAbilityId() = AssimilationAegis.activatedAbilities.first().id

    fun GameTestDriver.cardNameOf(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    test("equipping after exiling a creature makes the equipped creature a copy of the exiled card") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2

        // My small creature that will be equipped, and the opponent's big creature to exile/copy.
        val myBears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")     // 2/2
        val bigFish = driver.putCreatureOnBattlefield(opp, "Gurmag Angler")    // 5/5
        driver.removeSummoningSickness(myBears)

        // Cast Assimilation Aegis; its ETB exiles up to one target creature — choose the Angler.
        val aegisCard = driver.putCardInHand(me, "Assimilation Aegis")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.submitSuccess(com.wingedsheep.engine.core.CastSpell(me, aegisCard))
        driver.bothPass() // resolve Aegis -> ETB trigger asks for the exile target
        driver.state.pendingDecision shouldNotBe null
        driver.submitTargetSelection(me, listOf(bigFish))
        driver.bothPass() // resolve ETB exile

        // The Angler is exiled (no longer on the battlefield).
        driver.findPermanent(opp, "Gurmag Angler") shouldBe null

        // Equip the Aegis onto my Bears.
        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = aegisCard,
                abilityId = driver.equipAbilityId(),
                targets = listOf(ChosenTarget.Permanent(myBears))
            )
        )
        // Resolve the equip, then the becomes-attached trigger it puts on the stack.
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        // My Bears is now a copy of Gurmag Angler (5/5).
        driver.cardNameOf(myBears) shouldBe "Gurmag Angler"
        driver.state.projectedState.getPower(myBears) shouldBe 5
        driver.state.projectedState.getToughness(myBears) shouldBe 5
    }

    test("the copy reverts when the Equipment leaves the battlefield") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2

        val myBears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bigFish = driver.putCreatureOnBattlefield(opp, "Gurmag Angler")
        driver.removeSummoningSickness(myBears)

        val aegisCard = driver.putCardInHand(me, "Assimilation Aegis")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.submitSuccess(com.wingedsheep.engine.core.CastSpell(me, aegisCard))
        driver.bothPass()
        driver.submitTargetSelection(me, listOf(bigFish))
        driver.bothPass()

        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(
            ActivateAbility(me, aegisCard, driver.equipAbilityId(), targets = listOf(ChosenTarget.Permanent(myBears)))
        )
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.cardNameOf(myBears) shouldBe "Gurmag Angler"

        // Destroy the Aegis with a removal spell (real LTB path). Its LTB returns the exiled
        // Angler; the equipped Bears, no longer attached to the Aegis, reverts to its own identity
        // (CR 611.2b — the AttachedCopyExpiryCheck state-based action runs as priority passes).
        driver.giveColorlessMana(me, 1)
        driver.giveMana(me, Color.BLUE, 1)
        val shatterCard = driver.putCardInHand(me, "Test Shatter")
        driver.submitSuccess(
            com.wingedsheep.engine.core.CastSpell(me, shatterCard, targets = listOf(ChosenTarget.Permanent(aegisCard)))
        )
        guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 12) driver.bothPass()

        driver.cardNameOf(myBears) shouldBe "Grizzly Bears"
        driver.state.projectedState.getPower(myBears) shouldBe 2
        driver.state.projectedState.getToughness(myBears) shouldBe 2
        // The exiled Angler is returned to the battlefield under its owner's control.
        driver.findPermanent(opp, "Gurmag Angler") shouldNotBe null
    }

    test("declining the exile target leaves the equipped creature unchanged") {
        val driver = createDriver()
        val me = driver.player1

        val myBears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(myBears)

        val aegisCard = driver.putCardInHand(me, "Assimilation Aegis")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.submitSuccess(com.wingedsheep.engine.core.CastSpell(me, aegisCard))
        driver.bothPass()
        // "up to one" — decline by choosing no target.
        if (driver.state.pendingDecision != null) {
            driver.submitTargetSelection(me, emptyList())
        }
        driver.bothPass()

        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(
            ActivateAbility(me, aegisCard, driver.equipAbilityId(), targets = listOf(ChosenTarget.Permanent(myBears)))
        )
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        // Nothing was exiled, so the Bears keeps its own identity.
        driver.cardNameOf(myBears) shouldBe "Grizzly Bears"
        driver.state.projectedState.getPower(myBears) shouldBe 2
    }
})
