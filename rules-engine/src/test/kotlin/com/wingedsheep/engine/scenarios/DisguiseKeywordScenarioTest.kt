package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.DogWalker
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NightdrinkerMoroii
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RakishScoundrel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Disguise (CR 702.168) — morph plus ward {2}.
 *
 * The mechanic reuses the whole morph pipeline: the same {3} sorcery-speed face-down cast, the same
 * generic turn-face-up special action (CR 116.2b). What is new is one *characteristic*: per
 * CR 702.168a a card cast with disguise becomes "a 2/2 face-down creature with ward {2}, no name,
 * no subtypes, and no mana cost", so the ward belongs to the face-down characteristic-defining
 * effect rather than to the card underneath. These tests pin down both halves — that the ward is
 * there while face down and gone the moment it isn't, and that everything printed on the card stays
 * suppressed until the flip (CR 708.2).
 */
class DisguiseKeywordScenarioTest : FunSpec({

    // A morph creature whose *face-up* card has ward. CR 708.2 says a face-down permanent has no
    // characteristics beyond those the rules that made it face down list, and morph (CR 702.37a)
    // lists none — so this creature must NOT have ward while it is face down. Guards the printed
    // card's ward from leaking through the face-down projection.
    val wardedMorphBear = card("Warded Morph Bear") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        morph = "{2}"
        keywordAbility(KeywordAbility.ward("{2}"))
    }

    val allCards = TestCards.all + listOf(
        NightdrinkerMoroii, DogWalker, RakishScoundrel, wardedMorphBear
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    /**
     * Put [cardName] onto the battlefield face down under [mode], deriving its turn-up data exactly
     * the way a real face-down entry does.
     */
    fun GameTestDriver.putFaceDown(
        playerId: EntityId,
        cardName: String,
        mode: FaceDownMode
    ): EntityId {
        val id = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent).with(FaceDownModeComponent(mode))
                FaceDownTurnUp.dataFor(cardDef, cardName, mode)?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    context("casting face down (CR 702.168a — {3}, sorcery speed)") {

        test("a disguise card can be cast face down for {3}") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val card = driver.putCardInHand(player, "Nightdrinker Moroii")
            driver.giveMana(player, Color.BLACK, 3)

            driver.submit(
                CastSpell(
                    playerId = player,
                    cardId = card,
                    castFaceDown = true,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).isSuccess shouldBe true
            driver.stackSize shouldBe 1

            // Resolving it produces a face-down permanent stamped with the DISGUISE mode.
            driver.bothPass()
            val permanent = driver.getPermanents(player).single { pid ->
                driver.state.getEntity(pid)?.has<FaceDownComponent>() == true
            }
            driver.state.getEntity(permanent)?.get<FaceDownModeComponent>()?.mode shouldBe
                FaceDownMode.DISGUISE
        }

        test("the face-down cast is offered as a legal action from hand") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.putCardInHand(player, "Nightdrinker Moroii")
            repeat(3) { driver.putLandOnBattlefield(player, "Swamp") }

            driver.legalActions(player)
                .filter { it.actionType == "CastFaceDown" }
                .map { it.description } shouldContain "Cast Nightdrinker Moroii face-down"
        }

        test("a card with neither morph nor disguise cannot be cast face down") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val bear = driver.putCardInHand(player, "Grizzly Bears")
            driver.giveMana(player, Color.BLACK, 3)

            driver.submit(
                CastSpell(
                    playerId = player,
                    cardId = bear,
                    castFaceDown = true,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).isSuccess shouldBe false
        }

        test("the face-down cast is unavailable at instant speed") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            // The opponent's turn: this player never has sorcery-speed permission.
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val opponent = driver.getOpponent(player)

            driver.putCardInHand(opponent, "Nightdrinker Moroii")
            repeat(3) { driver.putLandOnBattlefield(opponent, "Swamp") }

            driver.legalActions(opponent).none { it.actionType == "CastFaceDown" } shouldBe true
        }
    }

    context("face-down characteristics (CR 702.168a / 708.2)") {

        test("a disguised permanent is a 2/2 with ward and none of the card's own keywords") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Nightdrinker Moroii is a printed 4/2 with flying.
            val moroii = driver.putFaceDown(player, "Nightdrinker Moroii", FaceDownMode.DISGUISE)

            val projected = driver.state.projectedState
            projected.getPower(moroii) shouldBe 2
            projected.getToughness(moroii) shouldBe 2
            projected.hasKeyword(moroii, Keyword.FLYING) shouldBe false
            projected.hasKeyword(moroii, Keyword.WARD) shouldBe true
        }

        test("a morphed permanent has no ward — morph lists no such characteristic") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val bear = driver.putFaceDown(player, "Warded Morph Bear", FaceDownMode.MORPH)

            driver.state.projectedState.hasKeyword(bear, Keyword.WARD) shouldBe false
        }

        test("turning face up ends the ward and restores the printed characteristics") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val moroii = driver.putFaceDown(player, "Nightdrinker Moroii", FaceDownMode.DISGUISE)
            driver.giveMana(player, Color.BLACK, 2) // Disguise {B}{B}

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = moroii,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null

            driver.state.getEntity(moroii)?.get<FaceDownComponent>() shouldBe null
            driver.state.getEntity(moroii)?.get<FaceDownModeComponent>() shouldBe null

            val projected = driver.state.projectedState
            projected.getPower(moroii) shouldBe 4
            projected.getToughness(moroii) shouldBe 2
            projected.hasKeyword(moroii, Keyword.FLYING) shouldBe true
            projected.hasKeyword(moroii, Keyword.WARD) shouldBe false
        }
    }

    context("ward {2} on a face-down permanent (CR 702.21 / 702.168a)") {

        test("an opponent targeting a disguised permanent must pay the {2} ward cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val moroii = driver.putFaceDown(opponent, "Nightdrinker Moroii", FaceDownMode.DISGUISE)

            repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
            driver.giveMana(player, Color.RED, 1)
            val bolt = driver.putCardInHand(player, "Lightning Bolt")
            driver.castSpellWithTargets(player, bolt, listOf(ChosenTarget.Permanent(moroii)))
                .isSuccess shouldBe true

            // Resolve the ward trigger — the caster is asked for {2}.
            driver.bothPass()
            val decision = driver.pendingDecision
            decision.shouldNotBeNull()
            decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
            decision.playerId shouldBe player
            decision.requiredCost shouldBe "{2}"
        }

        test("declining the ward cost counters the spell, leaving the permanent face down") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val moroii = driver.putFaceDown(opponent, "Nightdrinker Moroii", FaceDownMode.DISGUISE)

            repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
            driver.giveMana(player, Color.RED, 1)
            val bolt = driver.putCardInHand(player, "Lightning Bolt")
            driver.castSpellWithTargets(player, bolt, listOf(ChosenTarget.Permanent(moroii)))

            driver.bothPass()
            driver.submitManaAutoPayOrDecline(player, autoPay = false)
            repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

            // Bolt was countered, so the 2/2 survives and stays face down.
            driver.state.getEntity(moroii)?.get<FaceDownComponent>() shouldBe FaceDownComponent
            driver.getGraveyardCardNames(player) shouldContain "Lightning Bolt"
        }

        test("the printed card's ward does not apply while it is face down (CR 708.2)") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Face-down under MORPH, so the mechanic contributes no ward — and the ward printed on
            // the card underneath is one of the abilities CR 708.2 suppresses.
            val bear = driver.putFaceDown(opponent, "Warded Morph Bear", FaceDownMode.MORPH)

            repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
            driver.giveMana(player, Color.RED, 1)
            val bolt = driver.putCardInHand(player, "Lightning Bolt")
            driver.castSpellWithTargets(player, bolt, listOf(ChosenTarget.Permanent(bear)))

            driver.bothPass()
            // No ward trigger at all: nothing to pay, and Bolt kills the 2/2 outright.
            driver.pendingDecision shouldBe null
            repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }
            driver.state.getEntity(bear) shouldNotBe null
            driver.getPermanents(opponent).contains(bear) shouldBe false
        }
    }

    context("turning face up (CR 702.168d)") {

        test("the turn-up action is offered at instant speed for the disguise cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val moroii = driver.putFaceDown(player, "Nightdrinker Moroii", FaceDownMode.DISGUISE)
            repeat(2) { driver.putLandOnBattlefield(player, "Swamp") }

            val turnUp = driver.legalActions(player).filter { it.action is TurnFaceUp }
            turnUp.size shouldBe 1
            turnUp.single().manaCostString shouldBe "{B}{B}"
            (turnUp.single().action as TurnFaceUp).sourceId shouldBe moroii
        }

        test("the derived turn-up procedure is the printed disguise cost") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val moroii = driver.putFaceDown(player, "Nightdrinker Moroii", FaceDownMode.DISGUISE)

            val data = driver.state.getEntity(moroii)?.get<MorphDataComponent>()
            data.shouldNotBeNull()
            data.procedures.size shouldBe 1
            data.procedures.single().mechanic shouldBe FaceDownMode.DISGUISE
            data.procedures.single().cost.description shouldBe "{B}{B}"
        }

        test("enters-the-battlefield triggers do not fire on the flip (CR 702.168d)") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Nightdrinker Moroii's "when this creature enters, you lose 3 life".
            val moroii = driver.putFaceDown(player, "Nightdrinker Moroii", FaceDownMode.DISGUISE)
            driver.giveMana(player, Color.BLACK, 2)

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = moroii,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

            driver.state.getEntity(moroii)?.get<FaceDownComponent>() shouldBe null
            driver.assertLifeTotal(player, 20)
        }

        test("a 'when turned face up' trigger does fire") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            // Dog Walker: "When this creature is turned face up, create two tapped 1/1 white Dogs."
            val walker = driver.putFaceDown(player, "Dog Walker", FaceDownMode.DISGUISE)
            driver.giveMana(player, Color.RED, 2) // Disguise {R/W}{R/W}

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = walker,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null
            repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

            val dogs = driver.getCreatures(player).filter { driver.getCardName(it) == "Dog Token" }
            dogs.size shouldBe 2
            dogs.all { driver.isTapped(it) } shouldBe true
        }
    }

    context("'enters or is turned face up' (Rakish Scoundrel — one ability, two conditions)") {

        /**
         * Drive the game forward, answering each "choose targets" prompt with [target], and return
         * how many such prompts appeared. One ability that fires once must ask exactly once — two
         * prompts would mean the two trigger conditions were treated as separate abilities.
         *
         * Stops as soon as the stack is empty with nothing pending: the granted indestructible only
         * lasts until end of turn, so passing priority any further would expire what we're asserting.
         */
        fun GameTestDriver.resolveTargetPrompts(player: EntityId, target: EntityId): Int {
            var prompts = 0
            repeat(8) {
                if (pendingDecision is ChooseTargetsDecision) {
                    prompts++
                    submitTargetSelection(player, listOf(target))
                } else if (pendingDecision == null && stackSize == 0 && prompts > 0) {
                    return prompts
                } else if (state.priorityPlayerId != null && !isPaused) {
                    bothPass()
                }
            }
            return prompts
        }

        test("fires exactly once when turned face up") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val scoundrel = driver.putFaceDown(player, "Rakish Scoundrel", FaceDownMode.DISGUISE)
            val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
            // Disguise {4}{B/G}{B/G} — six mana.
            driver.giveMana(player, Color.BLACK, 6)

            driver.submit(
                TurnFaceUp(
                    playerId = player,
                    sourceId = scoundrel,
                    paymentStrategy = PaymentStrategy.FromPool
                )
            ).error shouldBe null

            driver.resolveTargetPrompts(player, bear) shouldBe 1
            driver.state.getEntity(scoundrel)?.get<FaceDownComponent>() shouldBe null
            driver.state.projectedState.hasKeyword(bear, Keyword.INDESTRUCTIBLE) shouldBe true
        }

        test("fires exactly once when it enters the battlefield face up") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
            val card = driver.putCardInHand(player, "Rakish Scoundrel")
            driver.giveMana(player, Color.BLACK, 3) // {2}{B}
            driver.giveMana(player, Color.GREEN, 1) // {G}

            driver.castSpell(player, card).error shouldBe null

            driver.resolveTargetPrompts(player, bear) shouldBe 1
            driver.findPermanent(player, "Rakish Scoundrel") shouldNotBe null
            driver.state.projectedState.hasKeyword(bear, Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }
})
