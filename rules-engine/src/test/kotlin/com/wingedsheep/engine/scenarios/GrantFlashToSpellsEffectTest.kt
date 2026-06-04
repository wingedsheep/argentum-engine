package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.FlashGrantsThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantFlashToSpellsEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Covers the Gap 4 primitive: [GrantFlashToSpellsEffect] / `FlashGrantsThisTurnComponent`.
 *
 * Two layers of coverage:
 *  1. **Direct cast-permission check** — seed `FlashGrantsThisTurnComponent` on a player and
 *     confirm a sorcery-speed spell becomes castable during the end step, that opponents do not
 *     benefit, and that the filter is honored.
 *  2. **End-to-end resolution** — cast an inline analogue of Borne Upon a Wind ({1}{U} instant:
 *     "You may cast spells this turn as though they had flash. Draw a card."), resolve it, then
 *     confirm a sorcery becomes castable later in the turn and that cleanup at end of turn
 *     removes the grant (CR 514.2).
 *
 * CR references verified against MagicCompRules_20260417.txt:
 *  - 702.8a — flash means "you may play this card any time you could cast an instant"
 *  - 601.3 — a player can begin to cast a spell only if a rule or effect allows it
 *  - 514.2 — at the end-of-turn cleanup step, "until end of turn" effects end
 */
class GrantFlashToSpellsEffectTest : FunSpec({

    val testSorcery = CardDefinition.sorcery(
        name = "Test Sorcery",
        manaCost = ManaCost.parse("{1}{R}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = DrawCardsEffect(1))
    )

    val testCreature = CardDefinition.creature(
        name = "Test Beast",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 2,
        toughness = 2
    )

    // Inline analogue of Borne Upon a Wind, restricted to the gap-4 portion + a draw, so
    // end-to-end resolution can be exercised before the card itself lands in its own commit.
    val flashGranter = CardDefinition.instant(
        name = "Test Flash Granter",
        manaCost = ManaCost.parse("{1}{U}"),
        oracleText = "You may cast spells this turn as though they had flash. Draw a card.",
        script = CardScript.spell(
            effect = CompositeEffect(
                listOf(
                    GrantFlashToSpellsEffect(
                        spellFilter = GameObjectFilter.Any,
                        duration = Duration.EndOfTurn
                    ),
                    DrawCardsEffect(1)
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(testSorcery, testCreature, flashGranter)
        )
        return driver
    }

    // ------------------------------------------------------------------
    // 1. Direct cast-permission tests — seed the component, verify lookup.
    // ------------------------------------------------------------------

    test("baseline: without the grant a sorcery can't be cast during the end step") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sorcery = driver.putCardInHand(p1, "Test Sorcery")
        driver.giveMana(p1, Color.RED, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    test("FlashGrantsThisTurnComponent makes a sorcery legal at end step (CR 702.8a, 601.3)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sorcery = driver.putCardInHand(p1, "Test Sorcery")
        driver.giveMana(p1, Color.RED, 2)

        // Seed the player-scoped flash permission directly (any spell, EOT duration).
        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsThisTurnComponent(
                        filters = listOf(GameObjectFilter.Any),
                        removeOn = PlayerEffectRemoval.EndOfTurn
                    )
                )
            }
        )
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    test("the grant is owner-scoped: an opponent's matching spell does not gain flash") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sorcery = driver.putCardInHand(p2, "Test Sorcery")
        driver.giveMana(p2, Color.RED, 2)

        // Grant on p1 — p2's sorcery must NOT inherit flash.
        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsThisTurnComponent(
                        filters = listOf(GameObjectFilter.Any),
                        removeOn = PlayerEffectRemoval.EndOfTurn
                    )
                )
            }
        )
        driver.passPriorityUntil(Step.END)
        driver.passPriority(p1) // give p2 priority

        val result = driver.submit(
            CastSpell(playerId = p2, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    test("filter is respected: a Sorcery-only grant does not flash a creature spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putCardInHand(p1, "Test Beast")
        driver.giveMana(p1, Color.GREEN, 2)

        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsThisTurnComponent(
                        filters = listOf(GameObjectFilter.Sorcery),
                        removeOn = PlayerEffectRemoval.EndOfTurn
                    )
                )
            }
        )
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = beast, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    test("filter is respected: a Sorcery-only grant does flash a sorcery spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sorcery = driver.putCardInHand(p1, "Test Sorcery")
        driver.giveMana(p1, Color.RED, 2)

        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsThisTurnComponent(
                        filters = listOf(GameObjectFilter.Sorcery),
                        removeOn = PlayerEffectRemoval.EndOfTurn
                    )
                )
            }
        )
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    // ------------------------------------------------------------------
    // 2. End-to-end: resolve an analogue Borne-Upon-a-Wind effect.
    // ------------------------------------------------------------------

    test("resolving GrantFlashToSpellsEffect records the grant on the controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val granter = driver.putCardInHand(p1, "Test Flash Granter")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.submitSuccess(
            CastSpell(playerId = p1, cardId = granter, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        val grants = driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>()
        grants.shouldNotBeNull()
        grants.filters shouldBe listOf(GameObjectFilter.Any)
        grants.removeOn shouldBe PlayerEffectRemoval.EndOfTurn
    }

    test("the grant clears at the end-of-turn cleanup step (CR 514.2)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Seed the grant directly; we only want to assert cleanup, not the resolution path.
        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsThisTurnComponent(
                        filters = listOf(GameObjectFilter.Any),
                        removeOn = PlayerEffectRemoval.EndOfTurn
                    )
                )
            }
        )
        driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>().shouldNotBeNull()

        // Advance into the opponent's upkeep — p1's CLEANUP has already run on the way through.
        // (UNTAP is auto-advanced past without exposing a priority window, so targeting UPKEEP.)
        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>().shouldBeNull()
    }

    test("multiple grants stack additively in one component (filters list grows)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!

        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsThisTurnComponent(filters = listOf(GameObjectFilter.Sorcery))
                )
            }
        )
        val before = driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>()!!.filters.size

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val granter = driver.putCardInHand(p1, "Test Flash Granter")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.submitSuccess(
            CastSpell(playerId = p1, cardId = granter, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        val after = driver.state.getEntity(p1)?.get<FlashGrantsThisTurnComponent>()!!.filters.size
        (after - before) shouldBe 1
    }
})
