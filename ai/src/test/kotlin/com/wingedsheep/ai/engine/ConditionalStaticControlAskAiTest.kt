package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.mtg.sets.definitions.frf.cards.BattleBrawler
import com.wingedsheep.mtg.sets.definitions.frf.cards.SultaiRunemark
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.lea.cards.HillGiant
import com.wingedsheep.mtg.sets.definitions.lea.cards.SavannahLions
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A static gated on "as long as you control a <kind of> permanent" (Fate Reforged's Battle Brawler and
 * Runemarks) asks for that kind of permanent, as a trigger's intervening "if you control …" does: the
 * deck's own permanents of that kind are what switch it on.
 */
class ConditionalStaticControlAskAiTest : FunSpec({

    test("Battle Brawler asks for red or white permanents, not green ones") {
        val brawler = SynergyAxes.of(BattleBrawler)
        brawler.asks.any { SynergyAxes.of(HillGiant).answers(it) } shouldBe true
        brawler.asks.any { SynergyAxes.of(SavannahLions).answers(it) } shouldBe true
        brawler.asks.any { SynergyAxes.of(GrizzlyBears).answers(it) } shouldBe false
    }

    test("Sultai Runemark's deathtouch clause asks for green or blue permanents") {
        val runemark = SynergyAxes.of(SultaiRunemark)
        runemark.asks.any { SynergyAxes.of(GrizzlyBears).answers(it) } shouldBe true
        runemark.asks.any { SynergyAxes.of(HillGiant).answers(it) } shouldBe false
    }
})
