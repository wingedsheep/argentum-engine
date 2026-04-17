package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.player.SpellKeywordGrant
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.TendrilsOfAgony
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Phase 6 of `backlog/storm-implementation-correctness.md`: per CR 702.40b,
 * each instance of Storm on a spell triggers separately. The most reachable
 * scenario is a card with the printed Storm keyword cast while the controller
 * also has Ral, Crackling Wit's "Instant and sorcery spells you cast have
 * storm" emblem — two distinct sources of Storm grant the spell two
 * instances of the ability, which should produce two independent Storm
 * triggers on the stack.
 */
class StormMultipleInstancesTest : FunSpec({

    test("printed Storm + emblem grant produces two independent Storm triggers") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Mana for Tendrils ({2}{B}{B}) + the prior cantrip ({R} via Lightning Bolt).
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }

        // Drop a Lightning Bolt cast first to bump spellsCastThisTurn to 1.
        // Use direct state injection so the test isn't bound to mana details.
        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))

        // Grant Storm to instants/sorceries the caster casts (mirrors Ral's emblem).
        val granted = GrantedSpellKeywordsComponent(
            grants = listOf(SpellKeywordGrant(Keyword.STORM, GameObjectFilter.InstantOrSorcery))
        )
        val withGrant = driver.state.updateEntity(caster) { it.with(granted) }
        driver.replaceState(withGrant)

        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")

        val result = driver.castSpell(caster, tendrils, listOf(opponent))
        result.isSuccess shouldBe true

        // Stack should contain: original Tendrils spell + 2 Storm trigger abilities.
        val stack = driver.state.stack
        val stormTriggers = stack.mapNotNull { id ->
            driver.state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 2
        stormTriggers.forEach { (it.effect as StormCopyEffect).copyCount shouldBe 1 }

        // Original Tendrils spell still present.
        val tendrilsOnStack = stack.any { id ->
            driver.state.getEntity(id)?.get<SpellOnStackComponent>() != null && id == tendrils
        }
        tendrilsOnStack shouldBe true
    }

    test("printed Storm only (no grant) still produces a single Storm trigger") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))

        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        val result = driver.castSpell(caster, tendrils, listOf(opponent))
        result.isSuccess shouldBe true

        val stormTriggers = driver.state.stack.mapNotNull { id ->
            driver.state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 1
    }
})
