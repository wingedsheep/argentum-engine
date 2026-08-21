package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The **cast-this-way destination rider** — `CastFromCollectionWithoutPayingCostEffect
 * .insteadOfGraveyard`, stamped on the cast card as
 * [AfterResolveDestinationComponent] and read by `StackResolver` at each of the points a spell can
 * leave the stack for a graveyard.
 *
 * The card tests (Kylox's Voltstrider, Flotsam // Jetsam) cover the resolved path. This file covers
 * the three the cards can't reach on their own, and they are the ones that break silently:
 *
 * - **Countered.** CR 701.5a puts a countered spell into its owner's graveyard. "If that spell would
 *   be put into a graveyard" is unqualified, so the rider applies there too — a Counterspell must
 *   not be a way to dodge it.
 * - **Fizzled.** CR 608.2b: a spell whose every target is illegal doesn't resolve and is put into
 *   its owner's graveyard. Same clause, same answer.
 * - **No rider at all.** The control: a spell cast the same way without `insteadOfGraveyard` goes to
 *   the graveyard normally, which is what proves the three above are the rider's doing.
 *
 * Both destinations are exercised, because they take different branches at the end
 * ([Zone.EXILE] vs. a bottom-of-library placement in [Zone.LIBRARY]) and only one of them was
 * reachable before this rider existed.
 */
class AfterResolveDestinationScenarioTest : FunSpec({

    /** A targetless payoff, so the resolved-path assertions are about the destination only. */
    val payoff = card("Test Rider Payoff") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(2) }
    }

    /**
     * A payoff that targets, so its target can be removed in response and the spell can fizzle
     * (CR 608.2b) without anybody countering it.
     */
    val targetedPayoff = card("Test Rider Targeted Payoff") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            target("target creature", Targets.Creature)
            effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0))
        }
    }

    /**
     * The granter: "cast an instant from your graveyard" with [destination] as the rider. One
     * builder rather than three near-identical cards, so the only difference between the cases
     * under test is the rider itself.
     */
    fun granter(name: String, destination: AfterResolveDestination?) = card(name) {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Composite(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        filter = GameObjectFilter.InstantOrSorcery
                    ),
                    storeAs = "pool"
                ),
                SelectFromCollectionEffect(
                    from = "pool",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "pick"
                ),
                Effects.CastFromCollectionWithoutPayingCost(
                    from = "pick",
                    insteadOfGraveyard = destination
                )
            )
        }
    }

    val bottomGranter = granter("Test Bottom Granter", AfterResolveDestination.BOTTOM_OF_LIBRARY)
    val exileGranter = granter("Test Exile Granter", AfterResolveDestination.EXILE)
    val plainGranter = granter("Test Plain Granter", null)

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(payoff, targetedPayoff, bottomGranter, exileGranter, plainGranter))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    /**
     * Cast [granterName] and pick [payoffId] out of the graveyard, stopping with the payoff spell
     * still on the stack so the caller can counter it, break its target, or just let it resolve.
     */
    fun GameTestDriver.castGranterAndPick(
        you: EntityId,
        granterName: String,
        payoffId: EntityId,
        payoffTargets: List<ChosenTarget> = emptyList(),
    ) {
        val granterId = putCardInHand(you, granterName)
        giveMana(you, Color.BLUE, 4)
        submit(
            CastSpell(you, granterId, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (!isPaused && state.stack.isNotEmpty() && guard++ < 10) bothPass()
        if (payoffTargets.isEmpty()) {
            submitCardSelection(you, listOf(payoffId))
        } else {
            submitCardSelection(you, listOf(payoffId))
            // The synthesized cast pauses for the payoff's own target before it hits the stack.
            submitTargetSelection(you, payoffTargets.map { (it as ChosenTarget.Permanent).entityId })
        }
    }

    test("a resolved spell goes to the bottom of its owner's library") {
        val driver = newDriver()
        val you = driver.player1
        val payoffId = driver.putCardInGraveyard(you, "Test Rider Payoff")
        val librarySizeBefore = driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size
        val lifeBefore = driver.getLifeTotal(you)

        driver.castGranterAndPick(you, "Test Bottom Granter", payoffId)
        driver.settle()

        driver.getLifeTotal(you) shouldBe lifeBefore + 2
        withClue("bottom, not shuffled in and not the graveyard") {
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).last() shouldBe payoffId
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).size shouldBe librarySizeBefore + 1
            driver.getGraveyardCardNames(you).contains("Test Rider Payoff") shouldBe false
        }
        withClue("the marker is cleaned off the card once it has been honoured") {
            driver.state.getEntity(payoffId)?.get<AfterResolveDestinationComponent>() shouldBe null
        }
    }

    test("a countered spell is redirected too — CR 701.5a is still 'put into a graveyard'") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val payoffId = driver.putCardInGraveyard(you, "Test Rider Payoff")
        val counter = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)

        driver.castGranterAndPick(you, "Test Bottom Granter", payoffId)
        withClue("the payoff waits on the stack, so it can be responded to") {
            driver.state.stack.contains(payoffId) shouldBe true
        }

        // Priority sits with the active player once the payoff is on the stack; hand it over.
        driver.passPriority(you)
        driver.submit(
            CastSpell(
                opponent, counter,
                targets = listOf(ChosenTarget.Spell(payoffId)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.settle()

        val lifeAfter = driver.getLifeTotal(you)
        withClue("it really was countered — no life gained") {
            lifeAfter shouldBe 20
        }
        withClue("a countered spell carrying the rider still skips the graveyard") {
            driver.getGraveyardCardNames(you).contains("Test Rider Payoff") shouldBe false
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).last() shouldBe payoffId
        }
    }

    test("a fizzled spell is redirected too — CR 608.2b") {
        val driver = newDriver()
        val you = driver.player1
        val payoffId = driver.putCardInGraveyard(you, "Test Rider Targeted Payoff")
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        driver.castGranterAndPick(
            you, "Test Exile Granter", payoffId,
            payoffTargets = listOf(ChosenTarget.Permanent(bear)),
        )
        withClue("the payoff waits on the stack with its target chosen") {
            driver.state.stack.contains(payoffId) shouldBe true
        }

        // Break the only target while the spell waits: every target is now illegal, so the spell
        // is removed from the stack without resolving.
        driver.moveToGraveyard(bear)
        driver.settle()

        withClue("a fizzled spell carrying the rider is exiled, not put into the graveyard") {
            driver.getGraveyardCardNames(you).contains("Test Rider Targeted Payoff") shouldBe false
            driver.getExile(you).contains(payoffId) shouldBe true
        }
    }

    test("without the rider the same cast goes to the graveyard — the control") {
        val driver = newDriver()
        val you = driver.player1
        val payoffId = driver.putCardInGraveyard(you, "Test Rider Payoff")

        driver.castGranterAndPick(you, "Test Plain Granter", payoffId)
        driver.settle()

        withClue("nothing was stamped, so CR 608.2m applies unchanged") {
            driver.getGraveyardCardNames(you).contains("Test Rider Payoff") shouldBe true
            driver.getExile(you).contains(payoffId) shouldBe false
            driver.state.getZone(ZoneKey(you, Zone.LIBRARY)).contains(payoffId) shouldBe false
        }
    }

    test("a declined pick leaves no rider on any card in the pool") {
        val driver = newDriver()
        val you = driver.player1
        val payoffId = driver.putCardInGraveyard(you, "Test Rider Payoff")
        val granterId = driver.putCardInHand(you, "Test Exile Granter")
        driver.giveMana(you, Color.BLUE, 4)

        driver.submit(
            CastSpell(you, granterId, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        var guard = 0
        while (!driver.isPaused && driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.submitCardSelection(you, emptyList())
        driver.settle()

        withClue("the rider is scoped to the card actually cast, never to the offered pool") {
            driver.state.getEntity(payoffId)?.get<AfterResolveDestinationComponent>() shouldBe null
            driver.getGraveyardCardNames(you).contains("Test Rider Payoff") shouldBe true
        }
    }

    test("the destination enum maps to the zone StackResolver moves the card to") {
        AfterResolveDestinationComponent(AfterResolveDestination.EXILE).zone shouldBe Zone.EXILE
        AfterResolveDestinationComponent(AfterResolveDestination.BOTTOM_OF_LIBRARY).zone shouldBe Zone.LIBRARY
        withClue("the default is the pre-existing 'exile it instead' behaviour") {
            AfterResolveDestinationComponent().destination shouldBe AfterResolveDestination.EXILE
        }
    }
})
