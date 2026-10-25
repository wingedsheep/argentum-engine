package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.BruenorBattlehammer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bruenor Battlehammer (AFR #219).
 *
 * - "Each creature you control gets +2/+0 for each Equipment attached to it." — per creature,
 *   counting only that creature's own Equipment (Auras don't count; opponents' creatures don't).
 * - "You may pay {0} rather than pay the equip cost of the first equip ability you activate each
 *   turn." — the first equip is free, the second pays its printed cost.
 */
class BruenorBattlehammerScenarioTest : FunSpec({

    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equip {2}"
        equipAbility("{2}")
    }

    val testCharm = card("Test Charm") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    val equipId = testBlade.activatedAbilities.first().id

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BruenorBattlehammer, testBlade, testCharm))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.attach(player: EntityId, host: EntityId, vararg names: String) {
        val attachments = names.map { name ->
            putPermanentOnBattlefield(player, name).also { addComponent(it, AttachedToComponent(host)) }
        }
        addComponent(host, AttachmentsComponent(attachments))
    }

    test("each creature you control gets +2/+0 per Equipment attached to it") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Bruenor Battlehammer")
        val twoBlades = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val bladeAndCharm = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val bare = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.attach(me, twoBlades, "Test Blade", "Test Blade")
        driver.attach(me, bladeAndCharm, "Test Blade", "Test Charm")
        driver.attach(opp, theirs, "Test Blade")

        val projected = driver.state.projectedState
        projected.getPower(twoBlades) shouldBe 7
        projected.getToughness(twoBlades) shouldBe 3
        projected.getPower(bladeAndCharm) shouldBe 5
        projected.getPower(bare) shouldBe 3
        projected.getPower(theirs) shouldBe 3
    }

    test("first equip each turn is free; the second pays its equip cost") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val bruenor = driver.putCreatureOnBattlefield(me, "Bruenor Battlehammer")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val blade = driver.putPermanentOnBattlefield(me, "Test Blade")

        // No mana: the first equip this turn costs {0}.
        driver.submit(
            ActivateAbility(me, blade, equipId, targets = listOf(ChosenTarget.Permanent(bruenor)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe bruenor
        driver.state.projectedState.getPower(bruenor) shouldBe 7

        // The second equip this turn needs the real {2}.
        driver.submitExpectFailure(
            ActivateAbility(me, blade, equipId, targets = listOf(ChosenTarget.Permanent(courser)))
        )
        driver.giveColorlessMana(me, 2)
        driver.submit(
            ActivateAbility(me, blade, equipId, targets = listOf(ChosenTarget.Permanent(courser)))
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe courser
        driver.state.projectedState.getPower(courser) shouldBe 5
        driver.state.projectedState.getPower(bruenor) shouldBe 5
    }
})
