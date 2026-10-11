package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.FlashGrant
import com.wingedsheep.engine.state.components.player.FlashGrantsComponent
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
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Covers the Gap 4 primitive: [GrantFlashToSpellsEffect] / `FlashGrantsComponent`.
 *
 * Two layers of coverage:
 *  1. **Direct cast-permission check** — seed `FlashGrantsComponent` on a player and
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
        driver.passPriorityUntil(Step.END)
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5); the cast must
        // still fail because there is no flash grant, not for lack of mana
        driver.giveMana(p1, Color.RED, 2)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldNotBe Outcome.Done
    }

    test("FlashGrantsComponent makes a sorcery legal at end step (CR 702.8a, 601.3)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sorcery = driver.putCardInHand(p1, "Test Sorcery")

        // Seed the player-scoped flash permission directly (any spell, EOT duration).
        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Any, PlayerEffectRemoval.EndOfTurn)))
                )
            }
        )
        driver.passPriorityUntil(Step.END)
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5)
        driver.giveMana(p1, Color.RED, 2)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldBe Outcome.Done
    }

    test("the grant is owner-scoped: an opponent's matching spell does not gain flash") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sorcery = driver.putCardInHand(p2, "Test Sorcery")

        // Grant on p1 — p2's sorcery must NOT inherit flash.
        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Any, PlayerEffectRemoval.EndOfTurn)))
                )
            }
        )
        driver.passPriorityUntil(Step.END)
        driver.passPriority(p1) // give p2 priority
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5); the cast must
        // still fail because the grant is owner-scoped to p1, not for lack of mana
        driver.giveMana(p2, Color.RED, 2)

        val result = driver.submit(
            CastSpell(playerId = p2, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldNotBe Outcome.Done
    }

    test("filter is respected: a Sorcery-only grant does not flash a creature spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val beast = driver.putCardInHand(p1, "Test Beast")

        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Sorcery, PlayerEffectRemoval.EndOfTurn)))
                )
            }
        )
        driver.passPriorityUntil(Step.END)
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5); the cast must
        // still fail because a creature isn't matched by the Sorcery-only grant, not for lack of mana
        driver.giveMana(p1, Color.GREEN, 2)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = beast, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldNotBe Outcome.Done
    }

    test("filter is respected: a Sorcery-only grant does flash a sorcery spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sorcery = driver.putCardInHand(p1, "Test Sorcery")

        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Sorcery, PlayerEffectRemoval.EndOfTurn)))
                )
            }
        )
        driver.passPriorityUntil(Step.END)
        // mana added here — unspent mana empties as each step/phase ends (CR 500.5)
        driver.giveMana(p1, Color.RED, 2)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldBe Outcome.Done
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

        val grants = driver.state.getEntity(p1)?.get<FlashGrantsComponent>()
        grants.shouldNotBeNull()
        grants.filters shouldBe listOf(GameObjectFilter.Any)
        grants.grants.single().removeOn shouldBe PlayerEffectRemoval.EndOfTurn
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
                    FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Any, PlayerEffectRemoval.EndOfTurn)))
                )
            }
        )
        driver.state.getEntity(p1)?.get<FlashGrantsComponent>().shouldNotBeNull()

        // Advance into the opponent's upkeep — p1's CLEANUP has already run on the way through.
        // (UNTAP is auto-advanced past without exposing a priority window, so targeting UPKEEP.)
        driver.passPriorityUntil(Step.UPKEEP)

        driver.state.getEntity(p1)?.get<FlashGrantsComponent>().shouldBeNull()
    }

    test("multiple grants stack additively in one component (filters list grows)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!

        driver.replaceState(
            driver.state.updateEntity(p1) { container ->
                container.with(
                    FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Sorcery)))
                )
            }
        )
        val before = driver.state.getEntity(p1)?.get<FlashGrantsComponent>()!!.filters.size

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val granter = driver.putCardInHand(p1, "Test Flash Granter")
        driver.giveMana(p1, Color.BLUE, 2)
        driver.submitSuccess(
            CastSpell(playerId = p1, cardId = granter, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        val after = driver.state.getEntity(p1)?.get<FlashGrantsComponent>()!!.filters.size
        (after - before) shouldBe 1
    }

    // ------------------------------------------------------------------
    // 3. Per-grant lifetimes — "until your next turn" (Teferi, Time Raveler's +1).
    // ------------------------------------------------------------------

    test("an until-your-next-turn grant survives cleanup, works on the opponent's turn, and lapses at the holder's next turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.replaceState(
            driver.state.updateEntity(p1) {
                it.with(FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Sorcery, PlayerEffectRemoval.UntilYourNextTurn))))
            }
        )
        val sorcery = driver.putCardInHand(p1, "Test Sorcery")

        // Into the opponent's turn: p1's cleanup ran, the grant did not lapse.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldNotBe p1
        driver.state.getEntity(p1)?.get<FlashGrantsComponent>().shouldNotBeNull()

        // p1 casts the sorcery at instant speed during the opponent's end step.
        driver.passPriorityUntil(Step.END, driver.activePlayer!!)
        while (driver.state.priorityPlayerId != p1) driver.passPriority(driver.state.priorityPlayerId!!)
        driver.giveMana(p1, Color.RED, 2)
        driver.submitSuccess(CastSpell(playerId = p1, cardId = sorcery, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()

        // p1's next turn: gone after their untap step.
        driver.passPriorityUntil(Step.UPKEEP, p1)
        driver.state.getEntity(p1)?.get<FlashGrantsComponent>().shouldBeNull()
    }

    test("grants of different lifetimes expire independently") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.replaceState(
            driver.state.updateEntity(p1) {
                it.with(
                    FlashGrantsComponent(
                        listOf(
                            FlashGrant(GameObjectFilter.Sorcery, PlayerEffectRemoval.UntilYourNextTurn),
                            FlashGrant(GameObjectFilter.Any, PlayerEffectRemoval.EndOfTurn)
                        )
                    )
                )
            }
        )

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.getEntity(p1)?.get<FlashGrantsComponent>()!!.grants shouldBe
            listOf(FlashGrant(GameObjectFilter.Sorcery, PlayerEffectRemoval.UntilYourNextTurn))
    }

    test("GrantFlashToSpells with Duration.UntilYourNextTurn records an until-your-next-turn grant") {
        val driver = GameTestDriver()
        val untilNextTurnGranter = CardDefinition.instant(
            name = "Test Lasting Flash Granter",
            manaCost = ManaCost.parse("{U}"),
            oracleText = "Until your next turn, you may cast sorcery spells as though they had flash.",
            script = CardScript.spell(
                effect = GrantFlashToSpellsEffect(spellFilter = GameObjectFilter.Sorcery, duration = Duration.UntilYourNextTurn)
            )
        )
        driver.registerCards(TestCards.all + listOf(testSorcery, untilNextTurnGranter))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val granter = driver.putCardInHand(p1, "Test Lasting Flash Granter")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.submitSuccess(CastSpell(playerId = p1, cardId = granter, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()

        driver.state.getEntity(p1)?.get<FlashGrantsComponent>()!!.grants.single().removeOn shouldBe
            PlayerEffectRemoval.UntilYourNextTurn
    }
})
