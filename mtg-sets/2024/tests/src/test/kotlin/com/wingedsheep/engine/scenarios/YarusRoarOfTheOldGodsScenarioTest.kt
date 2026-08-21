package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NightdrinkerMoroii
import com.wingedsheep.mtg.sets.definitions.mkm.cards.YarusRoarOfTheOldGods
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Yarus, Roar of the Old Gods — "Other creatures you control have haste. Whenever one or more
 * face-down creatures you control deal combat damage to a player, draw a card. Whenever a face-down
 * creature you control dies, return it to the battlefield face down under its owner's control if
 * it's a permanent card, then turn it face up."
 *
 * Both trigger halves rest on engine changes made for this card, and the tests are aimed at those:
 *
 * - the **damage** half needed the blanket `FaceDownComponent` skip removed from
 *   `TriggerDetector.detectCombatDamageBatchTriggers`. The batching contract is what the removal
 *   must not disturb, so there is a two-attacker test: one draw, not two. And because that guard
 *   was the only thing keeping a face-down creature out of a *name*-filtered batch trigger, a
 *   control card proves the replacement — `PredicateEvaluator`'s face-down masking — actually holds.
 * - the **death** half needed `EntitySnapshot.wasFaceDown`, because a card put into a graveyard is
 *   turned face up (CR 708.4) and the battlefield entity is gone by trigger-gating time. Without
 *   that channel the trigger simply never matched.
 *
 * The face-up control tests are the other half of each: a hard-cast creature connecting must draw
 * nothing, and a face-up creature dying must stay dead.
 */
class YarusRoarOfTheOldGodsScenarioTest : FunSpec({

    // A vanilla 2/2 with no morph or disguise — stands in for a *cloaked* or *manifested* permanent
    // (a card put face down by an effect, not cast face down). It is the honest test subject for the
    // dies trigger: the card underneath needs no face-down mechanic of its own for Yarus to bring
    // it back.
    val plainBear = card("Plain Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        // 4/1 on purpose: face down it is a 2/2 (CR 708.2), so power alone tells "came back face
        // up" apart from "came back still face down".
        power = 4
        toughness = 1
    }

    // A plain 2/2 blocker, defined here rather than borrowed from the corpus so the combat maths in
    // these tests can't drift with someone else's card.
    val testBlocker = card("Test Blocker") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Wall"
        power = 2
        toughness = 2
    }

    // A creature whose printed name is what a name-filtered batch trigger would look for. Used to
    // prove that removing the face-down guard did NOT open name-filtered triggers to face-down
    // creatures: a face-down permanent has no name (CR 708.2a).
    val namedHitter = card("Named Test Hitter") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
    }

    val allCards = TestCards.all +
        listOf(YarusRoarOfTheOldGods, NightdrinkerMoroii, plainBear, testBlocker, namedHitter)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        return driver
    }

    /**
     * Put [cardName] onto the battlefield face down under [mode], deriving its turn-up data the way
     * a real face-down entry does. Mirrors `DisguiseKeywordScenarioTest.putFaceDown`.
     */
    fun GameTestDriver.putFaceDown(
        playerId: EntityId,
        cardName: String,
        mode: FaceDownMode = FaceDownMode.CLOAK
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

    context("other creatures you control have haste") {

        test("a creature that entered this turn can attack alongside Yarus") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)

            driver.putCreatureOnBattlefield(player, "Yarus, Roar of the Old Gods")
            // Deliberately NOT removeSummoningSickness — haste is what has to let this attack.
            val bear = driver.putCreatureOnBattlefield(player, "Plain Test Bear")

            driver.state.projectedState.hasKeyword(bear, Keyword.HASTE) shouldBe true

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            withClue("the granted haste lets a freshly-entered creature attack") {
                driver.declareAttackers(player, listOf(bear), opponent).isSuccess shouldBe true
            }
        }
    }

    context("whenever one or more face-down creatures you control deal combat damage to a player") {

        test("a face-down attacker connecting draws a card") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)

            driver.putCreatureOnBattlefield(player, "Yarus, Roar of the Old Gods")
            val hidden = driver.putFaceDown(player, "Plain Test Bear")

            val handBefore = driver.getHandSize(player)
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(player, listOf(hidden), opponent).isSuccess shouldBe true
            driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
            driver.declareNoBlockers(opponent)
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            withClue("the face-down 2/2 got through") {
                driver.getLifeTotal(opponent) shouldBe 18
            }
            withClue("the batch trigger fired once — this is the removed FaceDownComponent guard") {
                driver.getHandSize(player) shouldBe handBefore + 1
            }
        }

        test("two face-down attackers hitting the same player still draw only one card") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)

            driver.putCreatureOnBattlefield(player, "Yarus, Roar of the Old Gods")
            val first = driver.putFaceDown(player, "Plain Test Bear")
            val second = driver.putFaceDown(player, "Nightdrinker Moroii", FaceDownMode.DISGUISE)

            val handBefore = driver.getHandSize(player)
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(player, listOf(first, second), opponent).isSuccess shouldBe true
            driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
            driver.declareNoBlockers(opponent)
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            withClue("both 2/2s connected") {
                driver.getLifeTotal(opponent) shouldBe 16
            }
            withClue("\"one or more\" batches per damaged player, so exactly one draw") {
                driver.getHandSize(player) shouldBe handBefore + 1
            }
        }

        test("a face-up attacker connecting draws nothing") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)

            driver.putCreatureOnBattlefield(player, "Yarus, Roar of the Old Gods")
            val bear = driver.putCreatureOnBattlefield(player, "Plain Test Bear")
            driver.removeSummoningSickness(bear)

            val handBefore = driver.getHandSize(player)
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(player, listOf(bear), opponent).isSuccess shouldBe true
            driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
            driver.declareNoBlockers(opponent)
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            withClue("face up it hits for its printed 4, not the 2 it would while hidden") {
                driver.getLifeTotal(opponent) shouldBe 16
            }
            withClue("the trigger is filtered to face-down creatures") {
                driver.getHandSize(player) shouldBe handBefore
            }
        }

        test("a name-filtered batch trigger still ignores a face-down creature") {
            // The control for the guard removal: with the blanket FaceDownComponent skip gone, a
            // face-down source is excluded from a name-filtered trigger only because
            // PredicateEvaluator masks `NameEquals` behind isFaceDown (CR 708.2a). If that masking
            // regressed, this would fire.
            val watcher = card("Name Watcher") {
                manaCost = "{2}{W}"
                typeLine = "Creature — Human Soldier"
                power = 1
                toughness = 3
                triggeredAbility {
                    trigger = com.wingedsheep.sdk.scripting.TriggerSpec(
                        com.wingedsheep.sdk.scripting.EventPattern.OneOrMoreDealCombatDamageToPlayerEvent(
                            sourceFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Creature
                                .named("Named Test Hitter")
                        ),
                        com.wingedsheep.sdk.scripting.TriggerBinding.ANY
                    )
                    effect = com.wingedsheep.sdk.dsl.Effects.DrawCards(1)
                    description = "Whenever one or more creatures named Named Test Hitter you " +
                        "control deal combat damage to a player, draw a card."
                }
            }
            val driver = GameTestDriver()
            driver.registerCards(allCards + watcher)
            driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)

            driver.putCreatureOnBattlefield(player, "Name Watcher")
            val hidden = driver.putFaceDown(player, "Named Test Hitter")

            val handBefore = driver.getHandSize(player)
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(player, listOf(hidden), opponent).isSuccess shouldBe true
            driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
            driver.declareNoBlockers(opponent)
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            driver.getLifeTotal(opponent) shouldBe 18
            withClue("face down, it has no name, so the name-filtered trigger must not fire") {
                driver.getHandSize(player) shouldBe handBefore
            }
        }
    }

    context("whenever a face-down creature you control dies") {

        test("a face-down creature that dies comes back face up") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)

            driver.putCreatureOnBattlefield(player, "Yarus, Roar of the Old Gods")
            val hidden = driver.putFaceDown(player, "Plain Test Bear")

            // Kill it in combat: a 2/2 face-down attacker into a 3/3 blocker.
            val blocker = driver.putCreatureOnBattlefield(opponent, "Test Blocker")
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(player, listOf(hidden), opponent).isSuccess shouldBe true
            driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
            driver.declareBlockers(opponent, mapOf(blocker to listOf(hidden)))
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            val returned = driver.findPermanent(player, "Plain Test Bear")
            withClue("wasFaceDown LKI is what lets this trigger match at all") {
                returned.shouldNotBeNull()
            }
            withClue("it was returned face down and then turned face up") {
                driver.state.getEntity(returned!!)?.has<FaceDownComponent>() shouldBe false
            }
            withClue("back as its real 4/1 self, not the 2/2 it was while hidden") {
                driver.state.projectedState.getPower(returned!!) shouldBe 4
            }
        }

        test("a face-up creature that dies is not returned") {
            val driver = createDriver()
            val player = driver.activePlayer!!
            val opponent = driver.getOpponent(player)

            driver.putCreatureOnBattlefield(player, "Yarus, Roar of the Old Gods")
            val bear = driver.putCreatureOnBattlefield(player, "Plain Test Bear")
            driver.removeSummoningSickness(bear)

            val blocker = driver.putCreatureOnBattlefield(opponent, "Test Blocker")
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(player, listOf(bear), opponent).isSuccess shouldBe true
            driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
            driver.declareBlockers(opponent, mapOf(blocker to listOf(bear)))
            driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

            withClue("the trigger is filtered to face-down creatures") {
                driver.findPermanent(player, "Plain Test Bear") shouldBe null
                driver.getGraveyardCardNames(player) shouldContain "Plain Test Bear"
            }
        }
    }
})
