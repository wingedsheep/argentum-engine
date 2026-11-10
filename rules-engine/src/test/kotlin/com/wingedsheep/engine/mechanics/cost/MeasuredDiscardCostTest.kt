package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.costs.VariableCostMeasure
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The measured `AbilityCost.DiscardX` — "Discard a card with mana value X" (Kozilek, the Great
 * Distortion; Knollspine Invocation).
 *
 * The discarded card's mana value is X. With no `{X}` in the cost the pick announces X (CR 601.2b
 * via CR 602.2b), so the engine asks for the discard first — offering only cards whose mana value
 * leaves a legal X-bounded target — and then for the target. With `{X}` mana the mana fixes X and
 * only cards of exactly that mana value may be discarded. A discarded card's `{X}` counts as 0
 * (CR 202.3e); a spell on the stack counts the X it was cast for (CR 202.3e as well).
 */
class MeasuredDiscardCostTest : FunSpec({

    val distortion = card("Test Distortion") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.DiscardCardWithManaValueX()
            target(TargetFilter.SpellOnStack.manaValueEqualsX())
            effect = Effects.CounterSpell()
        }
    }
    val invocation = card("Test Invocation") {
        manaCost = "{1}{R}"
        typeLine = "Enchantment"
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{X}"), Costs.DiscardCardWithManaValueX())
            val t = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.xValue(), t)
        }
    }
    val two = card("Test Two Drop") {
        manaCost = "{1}{R}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val three = card("Test Three Drop") {
        manaCost = "{2}{R}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val xDrop = card("Test X Drop") {
        manaCost = "{X}{R}{R}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(DynamicAmounts.xValue()) }
    }

    val distortionAbility = distortion.activatedAbilities.single().id
    val invocationAbility = invocation.activatedAbilities.single().id

    fun setup(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(distortion, invocation, two, three, xDrop))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Cast [name] from [player]'s hand, leaving it on the stack with [player] holding priority. */
    fun GameTestDriver.castFromHand(player: EntityId, name: String, xValue: Int? = null): EntityId {
        val card = putCardInHand(player, name)
        repeat(5) { putPermanentOnBattlefield(player, "Mountain") }
        val result = if (xValue != null) castXSpell(player, card, xValue) else castSpell(player, card)
        result.error shouldBe null
        return card
    }

    fun GameTestDriver.hand(player: EntityId) = state.getZone(ZoneKey(player, Zone.HAND))
    fun GameTestDriver.graveyard(player: EntityId) = state.getZone(ZoneKey(player, Zone.GRAVEYARD))

    test("the discard is asked first, offering only cards matching a spell's mana value; then the target") {
        val d = setup()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Distortion")
        val spell = d.castFromHand(you, "Test Three Drop")
        val matching = d.putCardInHand(you, "Test Three Drop")
        val wrongValue = d.putCardInHand(you, "Test Two Drop")

        d.legalActions(you).any { (it.action as? ActivateAbility)?.sourceId == source } shouldBe true
        d.submit(ActivateAbility(playerId = you, sourceId = source, abilityId = distortionAbility))
            .outcome.shouldBeInstanceOf<Outcome.Paused>()

        // Even a single eligible card is asked for — skipping would reveal the rest of the hand.
        val discard = d.pendingDecision
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        discard.options shouldBe listOf(matching)
        discard.minSelections shouldBe 1
        discard.maxSelections shouldBe 1
        d.submitDecision(you, CardsSelectedResponse(discard.id, listOf(matching)))

        // X = 3 now, so the target is a spell with mana value exactly 3.
        val target = d.pendingDecision
        target.shouldBeInstanceOf<ChooseTargetsDecision>()
        target.legalTargets[0] shouldBe listOf(spell)
        d.submitDecision(you, TargetsResponse(target.id, mapOf(0 to listOf(spell))))

        d.graveyard(you).contains(matching) shouldBe true
        d.hand(you).contains(wrongValue) shouldBe true
        d.bothPass()

        // The ability resolved first and countered the spell.
        d.graveyard(you).contains(spell) shouldBe true
        d.state.stack.isEmpty() shouldBe true
    }

    test("with two spells on the stack the discard covers both mana values and the target narrows to X") {
        val d = setup()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Distortion")
        val twoSpell = d.castFromHand(you, "Test Two Drop")
        val threeSpell = d.castFromHand(you, "Test Three Drop")
        val twoCard = d.putCardInHand(you, "Test Two Drop")
        val threeCard = d.putCardInHand(you, "Test Three Drop")

        d.submit(ActivateAbility(playerId = you, sourceId = source, abilityId = distortionAbility))
        val discard = d.pendingDecision
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        // Lands in hand (mana value 0) match no spell; both spells' mana values are covered.
        discard.options.toSet() shouldBe setOf(twoCard, threeCard)
        d.submitDecision(you, CardsSelectedResponse(discard.id, listOf(twoCard)))

        val target = d.pendingDecision
        target.shouldBeInstanceOf<ChooseTargetsDecision>()
        target.legalTargets[0] shouldBe listOf(twoSpell)
        d.submitDecision(you, TargetsResponse(target.id, mapOf(0 to listOf(twoSpell))))
        d.bothPass()
        d.graveyard(you).contains(twoSpell) shouldBe true
        d.state.stack.contains(threeSpell) shouldBe true
    }

    test("targets sent before the discard are refused: the pick announces X first") {
        val d = setup()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Distortion")
        val bigSpell = d.castFromHand(you, "Test X Drop", xValue = 3) // mana value 5
        d.castFromHand(you, "Test Three Drop")
        val three = d.putCardInHand(you, "Test Three Drop")

        d.submit(
            ActivateAbility(
                playerId = you, sourceId = source, abilityId = distortionAbility,
                targets = listOf(ChosenTarget.Spell(bigSpell)),
            )
        ).error!! shouldContain "before the targets"
        d.hand(you).contains(three) shouldBe true
        d.pendingDecision shouldBe null
    }

    test("a spell's X counts at the value it was cast for; a card in hand counts its X as 0") {
        val d = setup()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Distortion")
        // {X}{R}{R} cast with X = 1 has mana value 3 on the stack.
        val spell = d.castFromHand(you, "Test X Drop", xValue = 1)
        val three = d.putCardInHand(you, "Test Three Drop")
        // The same card in hand is mana value 2 — it can't pay for a mana-value-3 counter.
        d.putCardInHand(you, "Test X Drop")

        d.submit(ActivateAbility(playerId = you, sourceId = source, abilityId = distortionAbility))
        val discard = d.pendingDecision
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        discard.options shouldBe listOf(three)
        d.submitDecision(you, CardsSelectedResponse(discard.id, listOf(three)))
        val target = d.pendingDecision
        target.shouldBeInstanceOf<ChooseTargetsDecision>()
        target.legalTargets[0] shouldBe listOf(spell)
    }

    test("with no card matching any spell's mana value the ability isn't offered") {
        val d = setup()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Distortion")
        d.castFromHand(you, "Test Three Drop")
        val two = d.putCardInHand(you, "Test Two Drop")

        d.legalActions(you).none { (it.action as? ActivateAbility)?.sourceId == source } shouldBe true
        d.submit(ActivateAbility(playerId = you, sourceId = source, abilityId = distortionAbility))
            .error!! shouldContain "leaves a legal target"
        d.hand(you).contains(two) shouldBe true
    }

    test("a pre-filled discard announces X: a mismatched target is rejected and nothing is discarded") {
        val d = setup()
        val you = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(you, "Test Distortion")
        val spell = d.castFromHand(you, "Test Three Drop")
        val two = d.putCardInHand(you, "Test Two Drop")
        val three = d.putCardInHand(you, "Test Three Drop")
        val spellTarget = listOf(ChosenTarget.Spell(spell))

        d.submit(
            ActivateAbility(
                playerId = you, sourceId = source, abilityId = distortionAbility,
                targets = spellTarget,
                costPayment = AdditionalCostPayment(discardedCards = listOf(two)),
            )
        ).error!! shouldContain "mana value exactly X"
        d.hand(you).contains(two) shouldBe true

        // A claimed X that the discarded card doesn't measure is refused at payment.
        d.submit(
            ActivateAbility(
                playerId = you, sourceId = source, abilityId = distortionAbility,
                targets = spellTarget, xValue = 3,
                costPayment = AdditionalCostPayment(discardedCards = listOf(two)),
            )
        ).error!! shouldContain "mana value X"
        d.hand(you).contains(two) shouldBe true

        d.submit(
            ActivateAbility(
                playerId = you, sourceId = source, abilityId = distortionAbility,
                targets = spellTarget,
                costPayment = AdditionalCostPayment(discardedCards = listOf(three)),
            )
        ).error shouldBe null
        d.graveyard(you).contains(three) shouldBe true
        d.bothPass()
        d.graveyard(you).contains(spell) shouldBe true
    }

    test("with {X} mana the mana fixes X and only a card of exactly that mana value may be discarded") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val source = d.putPermanentOnBattlefield(you, "Test Invocation")
        repeat(2) { d.putPermanentOnBattlefield(you, "Mountain") }
        val two = d.putCardInHand(you, "Test Two Drop")
        d.putCardInHand(you, "Test Three Drop")
        d.putCardInHand(you, "Test X Drop") // mana value 2 in hand
        val xInHand = d.hand(you).last()

        d.submit(
            ActivateAbility(
                playerId = you, sourceId = source, abilityId = invocationAbility,
                targets = listOf(ChosenTarget.Player(opponent)), xValue = 2,
            )
        ).outcome.shouldBeInstanceOf<Outcome.Paused>()
        val discard = d.pendingDecision
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        discard.options.toSet() shouldBe setOf(two, xInHand)
        d.submitDecision(you, CardsSelectedResponse(discard.id, listOf(two)))

        d.graveyard(you).contains(two) shouldBe true
        val life = d.getLifeTotal(opponent)
        d.bothPass()
        d.getLifeTotal(opponent) shouldBe life - 2
    }

    test("the measured discard is exactly one card; X cards has no printed count") {
        shouldThrow<IllegalArgumentException> { AbilityCost.DiscardX(count = 1) }
        shouldThrow<IllegalArgumentException> {
            AbilityCost.DiscardX(xMeasure = VariableCostMeasure.TOTAL_MANA_VALUE)
        }
        shouldThrow<IllegalArgumentException> {
            AbilityCost.DiscardX(xMeasure = VariableCostMeasure.TOTAL_POWER, count = 1)
        }
        shouldThrow<IllegalArgumentException> {
            AbilityCost.DiscardX(xMeasure = VariableCostMeasure.TOTAL_MANA_VALUE, count = 2)
        }
        AbilityCost.DiscardX(xMeasure = VariableCostMeasure.TOTAL_MANA_VALUE, count = 1).description shouldBe
            "Discard a card with mana value X"
    }
})
