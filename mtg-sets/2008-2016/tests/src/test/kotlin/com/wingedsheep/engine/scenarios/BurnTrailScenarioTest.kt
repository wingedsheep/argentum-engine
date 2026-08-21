package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.shm.cards.BurnTrail
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Burn Trail (SHM) — {3}{R} Sorcery
 *
 * "Burn Trail deals 3 damage to any target.
 *  Conspire (As you cast this spell, you may tap two untapped creatures you control that share a
 *  color with it. When you do, copy it and you may choose a new target for the copy.)"
 *
 * Shadowmoor is the first set in the corpus to *print* conspire — the only prior coverage
 * (`ConspireTest`) exercises the keyword **granted** by Raiding Schemes. The two paths differ:
 * a granted conspire lives on `SpellGrantedKeywordsComponent`, while a printed one has to survive
 * `CardBuilder`'s derivation of `Keyword.CONSPIRE` from `keywordAbility(KeywordAbility.Conspire)`
 * into `CardDefinition.keywords`, which is what `GrantedKeywordResolver.hasKeyword` reads. These
 * tests are that derivation's only end-to-end coverage.
 *
 * Burn Trail is a **sorcery**, which matters: `CastSpellHandler` only builds the conspire copy
 * trigger when `cardDef.script.spellEffect != null`, so conspire on a permanent spell would tap
 * the creatures and produce no copy. Every conspire card in Shadowmoor is an instant or a sorcery.
 */
class BurnTrailScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BurnTrail))
        return driver
    }

    test("printed Conspire is derived onto the card's keywords") {
        BurnTrail.keywords.contains(Keyword.CONSPIRE) shouldBe true
    }

    test("tapping two red creatures copies the spell and the copy can be retargeted") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        // Two untapped red creatures — Burn Trail is red, so they share a color with it.
        val goblin1 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        val goblin2 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        repeat(4) { driver.putLandOnBattlefield(caster, "Mountain") }
        val burnTrail = driver.putCardInHand(caster, "Burn Trail")

        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = burnTrail,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.AutoPay,
                conspiredCreatures = listOf(goblin1, goblin2)
            )
        ).isSuccess shouldBe true

        // Conspire's additional cost taps both creatures.
        driver.state.getEntity(goblin1)!!.has<TappedComponent>() shouldBe true
        driver.state.getEntity(goblin2)!!.has<TappedComponent>() shouldBe true

        // The reflexive "when you do" trigger resolves and pauses to retarget the copy.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass()
            guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true

        driver.submitTargetSelection(caster, listOf(caster)).isSuccess shouldBe true

        val copyId = driver.state.stack.single { id ->
            val e = driver.state.getEntity(id)
            e?.get<SpellOnStackComponent>() != null && e.has<CopyOfComponent>()
        }
        driver.state.getEntity(copyId)!!.get<TargetsComponent>()?.targets shouldBe
            listOf(ChosenTarget.Player(caster))
    }

    test("declining Conspire leaves the creatures untapped and makes no copy") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        val goblin1 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        val goblin2 = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        repeat(4) { driver.putLandOnBattlefield(caster, "Mountain") }
        val burnTrail = driver.putCardInHand(caster, "Burn Trail")

        driver.castSpell(caster, burnTrail, listOf(opponent)).isSuccess shouldBe true

        driver.state.getEntity(goblin1)!!.has<TappedComponent>() shouldBe false
        driver.state.getEntity(goblin2)!!.has<TappedComponent>() shouldBe false
        driver.state.stack.any { driver.state.getEntity(it)?.has<CopyOfComponent>() == true } shouldBe false
    }

    test("Conspire rejects creatures that share no color with the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        val redGoblin = driver.putCreatureOnBattlefield(caster, "Goblin Guide")
        // Centaur Courser is green — Burn Trail is mono-red, so this pairing is illegal.
        val greenCentaur = driver.putCreatureOnBattlefield(caster, "Centaur Courser")
        repeat(4) { driver.putLandOnBattlefield(caster, "Mountain") }
        val burnTrail = driver.putCardInHand(caster, "Burn Trail")

        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = burnTrail,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.AutoPay,
                conspiredCreatures = listOf(redGoblin, greenCentaur)
            )
        ).isSuccess shouldBe false
    }
})
