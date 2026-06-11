package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.ShagratLootBearer
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shagrat, Loot Bearer — "Whenever Shagrat attacks, attach up to one target Equipment to it. Then
 * amass Orcs X, where X is the number of Equipment attached to Shagrat."
 *
 * The amass X uses the new `DynamicAmounts.equipmentAttachedToSelf()` (Equipment-only attachment
 * count), so the filter test proves Auras attached to Shagrat do NOT inflate X.
 */
class ShagratLootBearerScenarioTest : FunSpec({

    // Minimal Aura used only to verify the Equipment filter excludes non-Equipment attachments.
    val filterAura = card("Filter Test Aura") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        oracleText = "test"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ShagratLootBearer + Bonesplitter + filterAura)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("equipmentAttachedToSelf counts only Equipment, not Auras") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val shagrat = driver.putCreatureOnBattlefield(you, "Shagrat, Loot Bearer")
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        val aura = driver.putPermanentOnBattlefield(you, "Filter Test Aura")

        // Manually attach both the Equipment and the Aura to Shagrat.
        driver.addComponent(sword, AttachedToComponent(shagrat))
        driver.addComponent(aura, AttachedToComponent(shagrat))
        driver.addComponent(shagrat, AttachmentsComponent(listOf(sword, aura)))

        // Sanity: the entities are the types we think they are.
        driver.state.getEntity(sword)?.get<CardComponent>()?.typeLine?.isEquipment shouldBe true
        driver.state.getEntity(aura)?.get<CardComponent>()?.typeLine?.isAura shouldBe true

        val ctx = EffectContext(sourceId = shagrat, controllerId = you, opponentId = null)
        val evaluator = DynamicAmountEvaluator()
        // Equipment-filtered count excludes the Aura …
        evaluator.evaluate(driver.state, DynamicAmounts.equipmentAttachedToSelf(), ctx) shouldBe 1
        // … while the unfiltered count includes both.
        evaluator.evaluate(driver.state, DynamicAmounts.attachmentsOnSelf(), ctx) shouldBe 2
    }

    test("Shagrat attacks: attaches the targeted Equipment and amasses Orcs equal to Equipment attached") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val shagrat = driver.putCreatureOnBattlefield(you, "Shagrat, Loot Bearer")
        val sword = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        driver.removeSummoningSickness(shagrat)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(shagrat), opponent)
        // "attach up to one target Equipment to it" — choose Bonesplitter as the target.
        driver.submitTargetSelection(you, listOf(sword))
        driver.bothPass() // resolve the attack trigger (attach + amass)

        // Bonesplitter is now attached to Shagrat (control unchanged), and an Orc Army exists with
        // a single +1/+1 counter (amass Orcs 1, since one Equipment is attached).
        driver.state.getEntity(sword)?.get<AttachedToComponent>()?.targetId shouldBe shagrat
        val army = driver.state.getBattlefield(you).firstOrNull { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name?.contains("Army") == true
        }
        army shouldNotBe null
        driver.state.getEntity(army!!)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
})
