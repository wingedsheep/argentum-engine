package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Airbend (Avatar: The Last Airbender) — "Exile target nonland permanent. While it's exiled, its
 * owner may cast it for {2} rather than its mana cost."
 *
 * Proves the engine feature end-to-end through an inline test instant ([airbendTester]) that uses
 * [Effects.Airbend]:
 *   - the targeted permanent is exiled to *its owner's* exile;
 *   - only the *owner* (not the airbend's controller) is granted a may-play permission;
 *   - the recast cost is a fixed {2}, *replacing* the printed mana cost (not added to it);
 *   - the owner can actually recast it from exile paying {2} of any color, and the fixed-cost
 *     component is cleaned up once it leaves exile.
 */
class AirbendScenarioTest : FunSpec({

    // {W} instant: "Airbend target nonland permanent." Target-agnostic Effects.Airbend() airbends
    // whatever the spell targets (CardSource.ChosenTargets).
    val airbendTester = card("Airbend Tester") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        oracleText = "Airbend target nonland permanent."
        spell {
            target("target nonland permanent", Targets.NonlandPermanent)
            effect = Effects.Airbend()
        }
    }

    // {W} instant: "Airbend up to one target creature or spell." Exercises the airbend stack branch
    // (the combined cross-zone target + the spell-vs-permanent ConditionalEffect dispatch), the same
    // shape as Aang, Swift Savior's ETB.
    val airbendOrSpellTester = card("Airbend Or Spell Tester") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Instant"
        oracleText = "Airbend up to one target creature or spell."
        spell {
            target(
                "up to one target creature or spell",
                TargetObject(
                    count = 1,
                    optional = true,
                    filter = TargetFilter.anyOf(TargetFilter.Creature, TargetFilter.SpellOnStack)
                )
            )
            effect = ConditionalEffect(
                condition = Conditions.TargetIsSpellOnStack(0),
                // Spell branch: airbend "exiles it" (not a counter) — Effects.AirbendSpell, which
                // also fires "whenever you airbend" once the spell is exiled (CR 701.65b).
                effect = Effects.AirbendSpell(ManaCost.parse("{2}")),
                elseEffect = Effects.Airbend()
            )
        }
    }

    // {W} sorcery: "Airbend all creatures." Exercises Effects.AirbendAll (battlefield gather).
    val airbendAllTester = card("Airbend All Tester") {
        manaCost = "{W}"
        colorIdentity = "W"
        typeLine = "Sorcery"
        oracleText = "Airbend all creatures."
        spell {
            // excludeSelf = false: the sorcery itself isn't a creature, but be explicit that every
            // creature (both players') is gathered.
            effect = Effects.AirbendAll(GameObjectFilter.Creature, excludeSelf = false)
        }
    }

    // {1}{G} creature that can't be countered — proves airbending a spell is "exile it", not a
    // counter, so it works on can't-be-countered spells.
    val uncounterableBears = card("Uncounterable Bears") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        cantBeCountered = true
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(airbendTester, airbendOrSpellTester, airbendAllTester, uncounterableBears)
        )
        return driver
    }

    /** The "cast this exiled card" legal action for [player], or null if they may not. */
    fun castAction(driver: GameTestDriver, player: EntityId, cardId: EntityId): LegalAction? =
        LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.FULL)
            .firstOrNull { it.actionType == "CastSpell" && (it.action as? CastSpell)?.cardId == cardId }

    test("airbending an opponent's permanent exiles it to its owner and grants only the owner a {2} recast") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls (and owns) Grizzly Bears — printed cost {1}{G}, which needs green mana.
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Airbend Tester")
        driver.putLandOnBattlefield(me, "Plains") // {W} for the airbend spell

        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        driver.bothPass()

        // Exiled into its OWNER's exile pile, and off the battlefield.
        driver.state.getZone(ZoneKey(opp, Zone.EXILE)) shouldContain bears
        driver.state.getZone(ZoneKey(opp, Zone.BATTLEFIELD)) shouldNotContain bears

        // Only the owner may recast it, for a fixed {2} (not the printed {1}{G}). The airbend's
        // controller gets no permission.
        val ownerAction = castAction(driver, opp, bears)
        ownerAction.shouldNotBeNull()
        ownerAction.manaCostString shouldBe "{2}"
        castAction(driver, me, bears).shouldBeNull()
    }

    test("airbending your own permanent lets you recast it for {2}, replacing the printed cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // I own Grizzly Bears ({1}{G}); airbend it, then recast it the same turn (sorcery speed —
        // I keep priority in my main) using only white mana, proving {1}{G} became {2} generic.
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Airbend Tester")
        repeat(3) { driver.putLandOnBattlefield(me, "Plains") } // {W} for airbend + {2} for the recast

        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        driver.bothPass()

        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain bears
        val recast = castAction(driver, me, bears)
        recast.shouldNotBeNull()
        recast.manaCostString shouldBe "{2}"

        // Recast from exile for {2} of white mana — impossible at the printed {1}{G}.
        driver.submitSuccess(CastSpell(playerId = me, cardId = bears, paymentStrategy = PaymentStrategy.AutoPay))
        driver.bothPass()

        // Back on the battlefield; the fixed-cost stamp is cleaned up so a later exile is unaffected.
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldContain bears
        driver.state.getEntity(bears)?.get<PlayWithFixedAlternativeManaCostComponent>().shouldBeNull()
    }

    test("a cost increase still applies on top of the fixed airbend cost (alt cost replaces, tax stacks)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Airbend Tester")
        driver.putLandOnBattlefield(me, "Plains")

        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        driver.bothPass()

        // The fixed-{2} airbend recast is in place; now layer a Soul Partition-style {1} tax on the
        // same exiled card. A cost increase isn't part of the cost the alternative replaces, so it
        // applies on top: {2} + {1} = {3}, not {2}.
        driver.state.getEntity(bears)?.get<PlayWithFixedAlternativeManaCostComponent>().shouldNotBeNull()
        driver.addComponent(bears, PlayWithCostIncreaseComponent(controllerId = me, amount = 1))
        castAction(driver, me, bears)?.manaCostString shouldBe "{3}"
    }

    test("airbending a spell exiles it from the stack, and its owner may recast it for {2}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // I cast a creature spell (Grizzly Bears, {1}{G}); the opponent responds by airbending it.
        repeat(2) { driver.putLandOnBattlefield(me, "Plains") }
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        val bearsSpell = driver.putCardInHand(me, "Grizzly Bears")
        driver.putLandOnBattlefield(opp, "Plains") // {W} for the airbend instant
        val airbend = driver.putCardInHand(opp, "Airbend Or Spell Tester")

        driver.submitSuccess(CastSpell(playerId = me, cardId = bearsSpell, paymentStrategy = PaymentStrategy.AutoPay))
        driver.passPriority(me) // pass priority to the opponent with the Bears spell on the stack
        val bearsOnStack = driver.state.stack.first()

        driver.submitSuccess(
            CastSpell(
                playerId = opp,
                cardId = airbend,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(bearsOnStack)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        driver.bothPass() // resolve the airbend instant: exile the Bears spell from the stack
        driver.bothPass() // the Bears spell is gone (exiled), nothing resolves

        // Exiled to its OWNER's exile (not graveyard); off the stack.
        driver.state.stack.shouldNotContain(bearsOnStack)
        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain bearsOnStack
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)) shouldNotContain bearsOnStack

        // Only the owner may recast it from exile, for {2}.
        val ownerAction = castAction(driver, me, bearsOnStack)
        ownerAction.shouldNotBeNull()
        ownerAction.manaCostString shouldBe "{2}"
        castAction(driver, opp, bearsOnStack).shouldBeNull()
    }

    test("airbending a spell that can't be countered still exiles it (airbend is not a counter)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // I cast Uncounterable Bears ({1}{G}, can't be countered); the opponent airbends the spell.
        repeat(2) { driver.putLandOnBattlefield(me, "Plains") }
        driver.giveMana(me, com.wingedsheep.sdk.core.Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        val bearsSpell = driver.putCardInHand(me, "Uncounterable Bears")
        driver.putLandOnBattlefield(opp, "Plains") // {W} for the airbend instant
        val airbend = driver.putCardInHand(opp, "Airbend Or Spell Tester")

        driver.submitSuccess(CastSpell(playerId = me, cardId = bearsSpell, paymentStrategy = PaymentStrategy.AutoPay))
        driver.passPriority(me)
        val bearsOnStack = driver.state.stack.first()

        driver.submitSuccess(
            CastSpell(
                playerId = opp,
                cardId = airbend,
                targets = listOf(ChosenTarget.Spell(bearsOnStack)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        driver.bothPass() // resolve the airbend instant
        driver.bothPass()

        // Despite "can't be countered", the spell is exiled from the stack (it leaves and never
        // resolves) — because airbend says "exile it", not "counter it".
        driver.state.stack.shouldNotContain(bearsOnStack)
        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain bearsOnStack
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldNotContain bearsOnStack
        castAction(driver, me, bearsOnStack)?.manaCostString shouldBe "{2}"
    }

    test("Effects.AirbendAll exiles every creature to its owner with a {2} recast") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val myBears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Airbend All Tester")
        driver.putLandOnBattlefield(me, "Plains") // {W}

        driver.submitSuccess(CastSpell(playerId = me, cardId = spell, paymentStrategy = PaymentStrategy.AutoPay))
        driver.bothPass()

        // Each creature lands in its own owner's exile; each owner may recast theirs for {2}.
        driver.state.getZone(ZoneKey(me, Zone.EXILE)) shouldContain myBears
        driver.state.getZone(ZoneKey(opp, Zone.EXILE)) shouldContain oppBears
        castAction(driver, me, myBears)?.manaCostString shouldBe "{2}"
        castAction(driver, opp, oppBears)?.manaCostString shouldBe "{2}"
    }
})
