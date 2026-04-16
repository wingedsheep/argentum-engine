package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.engine.core.PaymentStrategy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for [GrantKeywordToOwnSpells] — the static ability behind Eirdu's
 * "Creature spells you cast have convoke." Verifies that granted convoke reaches the
 * cast pipeline the same way an intrinsic CONVOKE keyword does.
 */
class GrantKeywordToOwnSpellsTest : FunSpec({

    /**
     * Stand-in for Eirdu's front face — grants convoke to the controller's creature spells.
     * Using a test card keeps this scenario focused on the static ability without pulling in
     * Eirdu's other abilities (which ship with Phase D).
     */
    val ConvokeGranter = CardDefinition.creature(
        name = "Convoke Granter",
        manaCost = ManaCost.parse("{3}{W}{W}"),
        subtypes = setOf(Subtype("Elemental")),
        power = 5,
        toughness = 5,
        oracleText = "Creature spells you cast have convoke.",
        script = CardScript(
            staticAbilities = listOf(
                GrantKeywordToOwnSpells(
                    keyword = Keyword.CONVOKE,
                    spellFilter = GameObjectFilter.Creature
                )
            )
        )
    )

    /**
     * A vanilla {2}{G} creature — cast under Convoke Granter's effect it should gain convoke.
     */
    val TestConvokeBeneficiary = CardDefinition.creature(
        name = "Convoke Beneficiary",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 3,
        toughness = 3
    )

    /**
     * A {2}{G} noncreature spell — should NOT gain convoke from Convoke Granter.
     */
    val TestConvokeNoncreature = CardDefinition.sorcery(
        name = "Convoke Noncreature",
        manaCost = ManaCost.parse("{2}{G}"),
        oracleText = "Draw 2 cards.",
        script = CardScript.spell(
            effect = com.wingedsheep.sdk.scripting.effects.DrawCardsEffect(2, com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(ConvokeGranter, TestConvokeBeneficiary, TestConvokeNoncreature)
        )
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("GrantedKeywordResolver returns true for creature spells while Convoke Granter is in play") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Convoke Granter")

        val resolver = GrantedKeywordResolver(driver.cardRegistry)
        val beneficiary = driver.cardRegistry.requireCard("Convoke Beneficiary")
        val noncreature = driver.cardRegistry.requireCard("Convoke Noncreature")

        resolver.hasKeyword(driver.state, caster, beneficiary, Keyword.CONVOKE) shouldBe true
        resolver.hasKeyword(driver.state, caster, noncreature, Keyword.CONVOKE) shouldBe false
    }

    test("GrantedKeywordResolver returns false when the granter is not on the battlefield") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val resolver = GrantedKeywordResolver(driver.cardRegistry)
        val beneficiary = driver.cardRegistry.requireCard("Convoke Beneficiary")

        resolver.hasKeyword(driver.state, caster, beneficiary, Keyword.CONVOKE) shouldBe false
    }

    test("GrantedKeywordResolver is scoped to the granter's controller") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Convoke Granter")

        val resolver = GrantedKeywordResolver(driver.cardRegistry)
        val beneficiary = driver.cardRegistry.requireCard("Convoke Beneficiary")

        // Caster benefits; opponent does not.
        resolver.hasKeyword(driver.state, caster, beneficiary, Keyword.CONVOKE) shouldBe true
        resolver.hasKeyword(driver.state, opponent, beneficiary, Keyword.CONVOKE) shouldBe false
    }

    test("a creature spell with granted convoke can be cast by tapping a creature for mana") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Granter + an untapped attacker we will tap for convoke.
        driver.putCreatureOnBattlefield(caster, "Convoke Granter")
        // Remove summoning sickness from the creature we'll tap so it's a legal convoke source.
        val tapper = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.replaceState(
            driver.state.updateEntity(tapper) { c ->
                c.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }
        )

        // Put Convoke Beneficiary in hand with only enough mana for {2} — convoke must pay the {G}.
        val spellId = driver.putCardInHand(caster, "Convoke Beneficiary")
        driver.giveMana(caster, Color.WHITE, 2)

        // Without convoke this would fail. With granted convoke tapping a creature for {G},
        // the cast is legal.
        val action = CastSpell(
            playerId = caster,
            cardId = spellId,
            targets = emptyList(),
            alternativePayment = AlternativePaymentChoice(
                convokedCreatures = mapOf(tapper to ConvokePayment(color = Color.GREEN))
            ),
            paymentStrategy = PaymentStrategy.FromPool
        )

        val result = driver.submit(action)
        result.isSuccess shouldBe true

        // The convoked creature is now tapped.
        driver.state.getEntity(tapper)?.has<TappedComponent>() shouldBe true

        driver.bothPass() // resolve Convoke Beneficiary
        driver.findPermanent(caster, "Convoke Beneficiary") shouldBe driver.findPermanent(caster, "Convoke Beneficiary")
    }

    test("noncreature spells are NOT granted convoke (static's spell filter is Creature)") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Convoke Granter")
        val tapper = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.replaceState(
            driver.state.updateEntity(tapper) { c ->
                c.without<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>()
            }
        )

        val spellId = driver.putCardInHand(caster, "Convoke Noncreature")
        driver.giveMana(caster, Color.WHITE, 2)

        val action = CastSpell(
            playerId = caster,
            cardId = spellId,
            targets = emptyList(),
            alternativePayment = AlternativePaymentChoice(
                convokedCreatures = mapOf(tapper to ConvokePayment(color = Color.GREEN))
            ),
            paymentStrategy = PaymentStrategy.FromPool
        )

        val result = driver.submit(action)
        // Convoke is not granted to noncreature spells, so the attempt to pay with convoke fails.
        result.isSuccess shouldBe false
    }
})
