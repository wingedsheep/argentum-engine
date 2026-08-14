package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NightdrinkerMoroii
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cloak (CR 701.58) — manifest plus ward {2}.
 *
 * "To cloak a card, turn it face down. It becomes a 2/2 face-down creature card with ward {2}, no
 * name, no subtypes, and no mana cost." The turn-up procedure is manifest's (CR 701.58b: pay the
 * card's mana cost, creature cards only), and the ward is a face-down characteristic exactly as it
 * is for disguise — which makes cloak's whole surface one [FaceDownMode] variant.
 *
 * Two rules get their own coverage here because they have no morph/manifest analogue in the engine
 * yet: CR 701.58c/d (a cloaked card that *also* has morph or disguise can be turned face up by
 * either procedure — two legal actions on one permanent) and CR 701.58g (a cloaked instant or
 * sorcery that would turn face up is revealed and stays face down).
 */
class CloakScenarioTest : FunSpec({

    // A plain creature card: cloaking it gives the mana-cost turn-up procedure of CR 701.58b.
    val plainBear = card("Cloakable Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // A creature that prints morph as well: CR 701.58c gives it two turn-up procedures once cloaked.
    val morphBear = card("Cloakable Morph Bear") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
        morph = "{G}"
    }

    val allCards = TestCards.all + listOf(plainBear, morphBear, NightdrinkerMoroii)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    /** Cloak [cardName] onto [playerId]'s battlefield, deriving turn-up data the way a real entry does. */
    fun GameTestDriver.cloak(playerId: EntityId, cardName: String): EntityId {
        val id = putPermanentOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent)
                    .with(FaceDownModeComponent(FaceDownMode.CLOAK))
                FaceDownTurnUp.dataFor(cardDef, cardName, FaceDownMode.CLOAK)?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    context("face-down characteristics (CR 701.58a)") {

        test("a cloaked card is a 2/2 with ward {2}") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Cloakable Morph Bear") // a printed 3/3

            val projected = driver.state.projectedState
            projected.getPower(cloaked) shouldBe 2
            projected.getToughness(cloaked) shouldBe 2
            projected.hasKeyword(cloaked, Keyword.WARD) shouldBe true
        }

        test("an opponent targeting a cloaked permanent must pay the {2} ward cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(opponent, "Cloakable Bear")

            repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
            driver.giveMana(player, Color.RED, 1)
            val bolt = driver.putCardInHand(player, "Lightning Bolt")
            driver.castSpellWithTargets(player, bolt, listOf(ChosenTarget.Permanent(cloaked)))
                .isSuccess shouldBe true

            driver.bothPass()
            val decision = driver.pendingDecision
            decision.shouldNotBeNull()
            decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
            decision.requiredCost shouldBe "{2}"
        }

        test("the ward ends when the permanent is turned face up") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Cloakable Bear")
            driver.giveMana(player, Color.GREEN, 2) // its printed {1}{G}

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = cloaked,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null

            driver.state.getEntity(cloaked)?.get<FaceDownComponent>() shouldBe null
            driver.state.getEntity(cloaked)?.get<FaceDownModeComponent>() shouldBe null
            driver.state.projectedState.hasKeyword(cloaked, Keyword.WARD) shouldBe false
        }
    }

    context("turning face up (CR 701.58b)") {

        test("a cloaked creature card turns face up for its own mana cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Cloakable Bear")

            val data = driver.state.getEntity(cloaked)?.get<MorphDataComponent>()
            data.shouldNotBeNull()
            data.procedures.map { it.mechanic } shouldContainExactly listOf(FaceDownMode.CLOAK)
            data.procedures.single().cost.description shouldBe "{1}{G}"

            driver.giveMana(player, Color.GREEN, 2)
            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = cloaked,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            driver.state.projectedState.getPower(cloaked) shouldBe 2
            driver.state.getEntity(cloaked)?.get<FaceDownComponent>() shouldBe null
        }

        test("a cloaked non-creature card has no turn-up procedure at all") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Lightning Bolt is an instant — CR 701.58b's "if it's a creature card" fails.
            val cloaked = driver.cloak(player, "Lightning Bolt")

            driver.state.getEntity(cloaked)?.get<MorphDataComponent>() shouldBe null
            driver.legalActions(player).none { it.action is TurnFaceUp } shouldBe true
            // And the special action is rejected outright, not silently ignored.
            driver.submit(TurnFaceUp(playerId = player, sourceId = cloaked)).isSuccess shouldBe false
        }
    }

    context("either procedure for a cloaked morph/disguise card (CR 701.58c/d)") {

        test("a cloaked morph card offers both the mana cost and the morph cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Cloakable Morph Bear")

            val data = driver.state.getEntity(cloaked)?.get<MorphDataComponent>()
            data.shouldNotBeNull()
            data.procedures.map { it.mechanic } shouldContainExactly
                listOf(FaceDownMode.CLOAK, FaceDownMode.MORPH)
            data.procedures.map { it.cost.description } shouldContainExactly listOf("{4}{G}", "{G}")

            // Enough mana for the cheap morph route only — both actions are still enumerated, and
            // the cheap one is affordable.
            repeat(5) { driver.putLandOnBattlefield(player, "Forest") }
            val turnUps = driver.legalActions(player).filter { it.action is TurnFaceUp }
            turnUps.map { it.manaCostString } shouldContainExactly listOf("{4}{G}", "{G}")
            turnUps.map { (it.action as TurnFaceUp).procedureIndex } shouldContainExactly listOf(0, 1)
        }

        test("a cloaked disguise card offers both the mana cost and the disguise cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Nightdrinker Moroii: {3}{B} mana cost, Disguise {B}{B}.
            val cloaked = driver.cloak(player, "Nightdrinker Moroii")

            val data = driver.state.getEntity(cloaked)?.get<MorphDataComponent>()
            data.shouldNotBeNull()
            data.procedures.map { it.mechanic } shouldContainExactly
                listOf(FaceDownMode.CLOAK, FaceDownMode.DISGUISE)
            data.procedures.map { it.cost.description } shouldContainExactly listOf("{3}{B}", "{B}{B}")
        }

        test("choosing the morph procedure pays the morph cost, not the mana cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Cloakable Morph Bear")
            // Exactly {G} — enough for the morph procedure, nowhere near the {4}{G} mana cost.
            driver.giveMana(player, Color.GREEN, 1)

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = cloaked,
                    paymentStrategy = PaymentStrategy.FromPool,
                    procedureIndex = 1
                )
            ).error shouldBe null
            driver.state.getEntity(cloaked)?.get<FaceDownComponent>() shouldBe null
            driver.state.projectedState.getPower(cloaked) shouldBe 3
        }

        test("an out-of-range procedure index is rejected") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Cloakable Bear")
            driver.giveMana(player, Color.GREEN, 5)

            driver.submit(
                TurnFaceUp(playerId = player, sourceId = cloaked, procedureIndex = 7)
            ).isSuccess shouldBe false
            driver.state.getEntity(cloaked)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        }
    }

    context("a cloaked instant or sorcery that would turn face up (CR 701.58g)") {

        test("is revealed and stays face down, and fires no turned-face-up trigger") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Lightning Bolt")

            // An effect — not the special action — tries to turn it face up (Expose the Culprit's
            // "turn target face-down creature face up" is the printed case).
            val executor = com.wingedsheep.engine.handlers.effects.permanent.types
                .TurnFaceUpExecutor(driver.cardRegistry)
            val result = executor.execute(
                driver.state,
                com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect(
                    com.wingedsheep.sdk.scripting.targets.EffectTarget.Self
                ),
                EffectContext(sourceId = cloaked, controllerId = player)
            )

            result.error shouldBe null
            // Still face down…
            result.state.getEntity(cloaked)?.get<FaceDownComponent>() shouldBe FaceDownComponent
            result.state.getEntity(cloaked)?.get<FaceDownModeComponent>()?.mode shouldBe
                FaceDownMode.CLOAK
            // …and no TurnFaceUpEvent, so "whenever a permanent is turned face up" can't trigger.
            result.events.none { it is TurnFaceUpEvent } shouldBe true
            // It was revealed to all players instead.
            result.events.any {
                it is com.wingedsheep.engine.core.CardsRevealedEvent &&
                    it.cardNames.contains("Lightning Bolt")
            } shouldBe true
        }

        test("a cloaked creature card is unaffected by that rule and does flip" ) {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val cloaked = driver.cloak(player, "Cloakable Bear")

            val executor = com.wingedsheep.engine.handlers.effects.permanent.types
                .TurnFaceUpExecutor(driver.cardRegistry)
            val result = executor.execute(
                driver.state,
                com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect(
                    com.wingedsheep.sdk.scripting.targets.EffectTarget.Self
                ),
                EffectContext(sourceId = cloaked, controllerId = player)
            )

            result.state.getEntity(cloaked)?.get<FaceDownComponent>() shouldBe null
            result.events.any { it is TurnFaceUpEvent } shouldBe true
        }
    }

    context("Hide in Plain Sight — cloak from the top of the library") {

        test("cloaks two of the top five and bottoms the rest") {
            val driver = createDriver()
            driver.registerCards(
                listOf(com.wingedsheep.mtg.sets.definitions.mkm.cards.HideInPlainSight)
            )
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Stack the top of the library with creature cards so the picks are identifiable.
            repeat(5) { driver.putCardOnTopOfLibrary(player, "Cloakable Bear") }
            val librarySizeBefore = driver.state.getLibrary(player).size

            val spell = driver.putCardInHand(player, "Hide in Plain Sight")
            driver.giveMana(player, Color.GREEN, 4)
            driver.castSpell(player, spell).error shouldBe null
            driver.bothPass()

            // Pick the two to cloak, then let the rest of the pipeline run.
            driver.pendingDecision.shouldNotBeNull()
            driver.autoResolveDecision()
            repeat(3) { if (driver.state.priorityPlayerId != null && !driver.isPaused) driver.bothPass() }

            val cloaked = driver.getPermanents(player).filter {
                driver.state.getEntity(it)?.has<FaceDownComponent>() == true
            }
            cloaked.size shouldBe 2
            cloaked.forEach { id ->
                driver.state.getEntity(id)?.get<FaceDownModeComponent>()?.mode shouldBe
                    FaceDownMode.CLOAK
                driver.state.projectedState.hasKeyword(id, Keyword.WARD) shouldBe true
                driver.state.projectedState.getPower(id) shouldBe 2
            }
            // Five left the library; two are on the battlefield and three went back to the bottom.
            driver.state.getLibrary(player).size shouldBe librarySizeBefore - 2
        }
    }
})
