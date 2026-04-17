package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.scourge.cards.TendrilsOfAgony
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 603.3b: when a spell with Storm is cast, the Storm trigger is put onto the
 * stack *on top of* the spell itself, so it resolves first and creates the
 * copies before the original spell resolves.
 *
 * `state.stack` is a List where `first()` is the bottom and `last()` is the
 * top (see [com.wingedsheep.engine.state.GameState.pushToStack]).
 */
class StormStackOrderTest : FunSpec({

    test("Storm trigger is on top of the original spell immediately after casting (603.3b)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        driver.replaceState(driver.state.copy(spellsCastThisTurn = 1))
        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        val stack = driver.state.stack
        stack.size shouldBe 2

        // Bottom of the stack: the original Tendrils spell.
        val bottomId = stack.first()
        bottomId shouldBe tendrils
        (driver.state.getEntity(bottomId)?.get<SpellOnStackComponent>() != null) shouldBe true

        // Top of the stack: the Storm triggered ability.
        val topId = stack.last()
        val topTrigger = driver.state.getEntity(topId)?.get<TriggeredAbilityOnStackComponent>()
        (topTrigger?.effect is StormCopyEffect) shouldBe true
    }

    test("two Storm instances both sit above the spell, with the spell still at the bottom") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TendrilsOfAgony))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Two Storm instances via Ral's-style grant on top of printed Storm.
        val granted = com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent(
            grants = listOf(
                com.wingedsheep.engine.state.components.player.SpellKeywordGrant(
                    com.wingedsheep.sdk.core.Keyword.STORM,
                    com.wingedsheep.sdk.scripting.GameObjectFilter.InstantOrSorcery
                )
            )
        )
        driver.replaceState(
            driver.state.copy(spellsCastThisTurn = 1)
                .updateEntity(caster) { it.with(granted) }
        )

        repeat(4) { driver.putLandOnBattlefield(caster, "Swamp") }
        val tendrils = driver.putCardInHand(caster, "Tendrils of Agony")
        driver.castSpell(caster, tendrils, listOf(opponent)).isSuccess shouldBe true

        val stack = driver.state.stack
        stack.first() shouldBe tendrils

        val aboveSpell = stack.drop(1)
        aboveSpell.size shouldBe 2
        aboveSpell.all { id ->
            driver.state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()?.effect is StormCopyEffect
        } shouldBe true
    }
})
