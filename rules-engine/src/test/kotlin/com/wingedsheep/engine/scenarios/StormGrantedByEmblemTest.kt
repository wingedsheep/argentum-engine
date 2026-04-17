package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.player.SpellKeywordGrant
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ral, Crackling Wit's ultimate emblem: "Instant and sorcery spells you cast
 * have storm." A non-Storm spell cast while this grant is active should
 * trigger Storm.
 *
 * The emblem is modeled via [GrantedSpellKeywordsComponent]; this test
 * exercises the grant path from [CastSpellHandler] directly.
 */
class StormGrantedByEmblemTest : FunSpec({

    test("Lightning Bolt with Storm-granting emblem produces a Storm trigger with copyCount = 2") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        val grant = GrantedSpellKeywordsComponent(
            grants = listOf(SpellKeywordGrant(Keyword.STORM, GameObjectFilter.InstantOrSorcery))
        )
        driver.replaceState(
            driver.state.copy(spellsCastThisTurn = 2)
                .updateEntity(caster) { it.with(grant) }
        )
        driver.putLandOnBattlefield(caster, "Mountain")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")

        driver.castSpell(caster, bolt, listOf(opponent)).isSuccess shouldBe true

        val stormTriggers = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 1
        (stormTriggers.single().effect as StormCopyEffect).copyCount shouldBe 2
    }

    test("without the emblem, Lightning Bolt casts with no Storm trigger") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Spells already cast this turn, but caster has no Storm-granting component.
        driver.replaceState(driver.state.copy(spellsCastThisTurn = 2))
        driver.putLandOnBattlefield(caster, "Mountain")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")

        driver.castSpell(caster, bolt, listOf(opponent)).isSuccess shouldBe true

        val stormTriggers = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 0
    }

    test("grant whose spellFilter doesn't match the spell's types does not produce a Storm trigger") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Filter only grants Storm to creature spells — Lightning Bolt is an instant.
        val grant = GrantedSpellKeywordsComponent(
            grants = listOf(SpellKeywordGrant(Keyword.STORM, GameObjectFilter.Creature))
        )
        driver.replaceState(
            driver.state.copy(spellsCastThisTurn = 2)
                .updateEntity(caster) { it.with(grant) }
        )
        driver.putLandOnBattlefield(caster, "Mountain")
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")

        driver.castSpell(caster, bolt, listOf(opponent)).isSuccess shouldBe true

        val stormTriggers = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.effect is StormCopyEffect }
        stormTriggers.size shouldBe 0
    }
})
