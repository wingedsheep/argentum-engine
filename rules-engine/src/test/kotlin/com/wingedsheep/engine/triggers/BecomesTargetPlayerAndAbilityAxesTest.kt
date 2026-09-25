package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Engine coverage for the two new axes on the becomes-target trigger:
 *
 *  - **player targets** — `StackResolver.emitBecomesTarget` now emits a [BecomesTargetEvent] when a
 *    *player* is chosen as a target (CR 601.2c: "The chosen objects and/or players each become a
 *    target of that spell"), stamped `targetIsPlayer = true`; the trigger side opts in with
 *    `EventPattern.BecomesTargetEvent.includePlayerTargets`.
 *  - **`abilitiesOnly`** — the mirror of the existing `spellsOnly`, reading the same `sourceIsSpell`
 *    axis: "becomes the target of an **ability**" (Loki, God of Mischief) versus "of a **spell**"
 *    (King of the Oathbreakers).
 *
 * The file has two halves. The *emission* half drives real casts/activations and asserts on the
 * events the engine produced — the only thing that can prove the player half is wired at all. The
 * *matching* half drives [TriggerDetector] with synthesized events (the u31 `CountersPlacedEvent`
 * shape) so every combination of the two axes is asserted against the same observers.
 */
class BecomesTargetPlayerAndAbilityAxesTest : FunSpec({

    // =========================================================================
    // Test cards
    // =========================================================================

    /** A targeted ACTIVATED ability whose target is a player. */
    val playerPinger = card("Target Player Pinger") {
        manaCost = "{1}"
        typeLine = "Creature — Human Wizard"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            val victim = target(Targets.Player)
            effect = Effects.LoseLife(1, victim)
            description = "{T}: Target player loses 1 life."
        }
    }

    /** A SPELL whose only target is a player. */
    val playerDrain = card("Target Player Drain") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val victim = target(Targets.Player)
            effect = Effects.LoseLife(1, victim)
        }
    }

    /** A SPELL that targets a player *and* a creature — two target slots, two events. */
    val playerAndCreatureSpell = card("Target Player And Creature") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val victim = target(Targets.Player)
            val creature = target(TargetFilter.Creature)
            effect = Effects.LoseLife(1, victim) then Effects.DealDamage(1, creature)
        }
    }

    /** "Change the target of target spell or ability with a single target" (Willbender's effect). */
    val retargeter = card("Test Retargeter") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOrAbilityOnStack)
            effect = Effects.ChangeTarget()
        }
    }

    // ---- observers ----------------------------------------------------------

    /** Loki's exact shape: an ability you control, players included, once each turn. */
    val lokiLike = card("Ability Player Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Wizard"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a().becomesTarget(byYou = true, abilitiesOnly = true, includePlayerTargets = true)
            oncePerTurn = true
            effect = Effects.DrawCards(1)
        }
    }

    /** `abilitiesOnly` without the player opt-in — objects only. */
    val abilityObjectObserver = card("Ability Object Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Wizard"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a().becomesTarget(byYou = true, abilitiesOnly = true)
            effect = Effects.DrawCards(1)
        }
    }

    /** The player opt-in without `abilitiesOnly` — spells count too. */
    val anySourcePlayerObserver = card("Any Source Player Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Wizard"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a().becomesTarget(includePlayerTargets = true)
            effect = Effects.DrawCards(1)
        }
    }

    /** The pre-existing shape, untouched: no axes at all. Must stay blind to players. */
    val plainObserver = card("Plain Becomes Target Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Wizard"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a().becomesTarget()
            effect = Effects.DrawCards(1)
        }
    }

    /** King of the Oathbreakers' half: `spellsOnly`, the mirror being asserted against. */
    val spellObserver = card("Spell Only Observer") {
        manaCost = "{0}"
        typeLine = "Creature — Human Wizard"
        power = 0
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a().becomesTarget(spellsOnly = true)
            effect = Effects.DrawCards(1)
        }
    }

    val extras = listOf(
        playerPinger, playerDrain, playerAndCreatureSpell, retargeter,
        lokiLike, abilityObjectObserver, anySourcePlayerObserver, plainObserver, spellObserver,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extras)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun becomesTargetEvents(result: ExecutionResult) =
        result.events.filterIsInstance<BecomesTargetEvent>()

    // =========================================================================
    // Emission — a player really does become a target
    // =========================================================================

    context("emission") {

        test("a spell targeting a player emits a player BecomesTargetEvent") {
            val driver = createDriver()
            val drain = driver.putCardInHand(driver.player1, "Target Player Drain")
            driver.giveMana(driver.player1, Color.BLACK, 1)

            val result = driver.castSpellWithTargets(
                driver.player1, drain, listOf(ChosenTarget.Player(driver.player2))
            )
            result.error shouldBe null

            val events = becomesTargetEvents(result)
            events shouldHaveSize 1
            events.first().targetEntityId shouldBe driver.player2
            events.first().targetIsPlayer shouldBe true
            withClue("a cast spell is the targeting source") {
                events.first().sourceIsSpell shouldBe true
            }
            withClue("the event carries the player's name, not \"Unknown\"") {
                events.first().targetName shouldNotBe "Unknown"
            }
        }

        test("an activated ability targeting a player emits a player BecomesTargetEvent") {
            val driver = createDriver()
            val pinger = driver.putCreatureOnBattlefield(driver.player1, "Target Player Pinger")
            driver.removeSummoningSickness(pinger)

            val abilityId = playerPinger.activatedAbilities.first().id
            val result = driver.submit(
                ActivateAbility(
                    playerId = driver.player1,
                    sourceId = pinger,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Player(driver.player2))
                )
            )
            result.error shouldBe null

            val events = becomesTargetEvents(result)
            events shouldHaveSize 1
            events.first().targetEntityId shouldBe driver.player2
            events.first().targetIsPlayer shouldBe true
            withClue("an activated ability is not a spell") {
                events.first().sourceIsSpell shouldBe false
            }
        }

        test("a triggered ability targeting a player emits a player BecomesTargetEvent") {
            val driver = createDriver()
            val source = driver.putCreatureOnBattlefield(driver.player1, "Target Player Pinger")

            // putTriggeredAbility is the third of the four target-declaration sites; drive it
            // directly rather than through a card so the assertion is about the site, not about
            // whichever trigger happened to be convenient.
            val result = driver.services.stackResolver.putTriggeredAbility(
                state = driver.state,
                ability = TriggeredAbilityOnStackComponent(
                    sourceId = source,
                    sourceName = "Target Player Pinger",
                    controllerId = driver.player1,
                    effect = Effects.LoseLife(1, EffectTarget.ContextTarget(0)),
                    description = "Target player loses 1 life."
                ),
                targets = listOf(ChosenTarget.Player(driver.player2))
            )
            result.error shouldBe null

            val events = becomesTargetEvents(result)
            events shouldHaveSize 1
            events.first().targetEntityId shouldBe driver.player2
            events.first().targetIsPlayer shouldBe true
            withClue("a triggered ability is not a spell") {
                events.first().sourceIsSpell shouldBe false
            }
        }

        test("a spell targeting a player AND a permanent emits one event for each") {
            val driver = createDriver()
            val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
            val spell = driver.putCardInHand(driver.player1, "Target Player And Creature")
            driver.giveMana(driver.player1, Color.BLACK, 1)

            val result = driver.castSpellWithTargets(
                driver.player1, spell,
                listOf(ChosenTarget.Player(driver.player2), ChosenTarget.Permanent(bears))
            )
            result.error shouldBe null

            val events = becomesTargetEvents(result)
            events shouldHaveSize 2
            events.single { it.targetIsPlayer }.targetEntityId shouldBe driver.player2
            events.single { !it.targetIsPlayer }.targetEntityId shouldBe bears
            withClue("both halves came from the same cast spell") {
                events.all { it.sourceIsSpell } shouldBe true
            }
        }

        /**
         * **Characterization of a known bug, not endorsement.** The three retarget/reselect sites
         * (`ManaPaymentContinuationResumer.resumeChangeSpellTarget`, `ContestedRetargetLogic.advance`,
         * `ReselectTargetRandomlyExecutor`) rewrite a stack object's `TargetsComponent` without
         * emitting any [BecomesTargetEvent] — for players *and* for permanents alike.
         *
         * That is **wrong** under the rules, not an open question: CR 115.9c counts the objects and
         * players chosen as targets when the spell or ability was put on the stack "(as modified by
         * effects that changed those targets)", so a redirected object *is* one of its targets, and
         * by CR 603.2f a "becomes" trigger fires at the moment the named event happens — which for a
         * redirect is the moment the new object becomes a target. So ward (CR 702.21a) and every
         * other becomes-target trigger *should* fire on a Spellskite/Misdirection redirect and do
         * not.
         *
         * The bug predates this change and is orthogonal to it (the player half rides on the four
         * target-*declaration* sites), so it is pinned here rather than fixed — a fix newly wakes
         * ward across the whole pool and needs its own unit. This test therefore locks in
         * current-and-wrong behaviour: when that unit lands, **invert** this test, don't delete it.
         */
        test("changing a spell's target to a player emits no BecomesTargetEvent (known bug, CR 115.9c)") {
            val driver = createDriver()
            // Both spells are cast by the same player, back to back, so priority never has to
            // change hands; an empty battlefield leaves the opposing player as the only legal
            // new target for the Bolt.
            driver.giveMana(driver.player1, Color.RED, 1)
            val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
            driver.castSpell(driver.player1, bolt, listOf(driver.player2)).error shouldBe null

            val boltOnStack = driver.state.stack.single { id ->
                driver.state.getEntity(id)?.get<CardComponent>()?.name == "Lightning Bolt"
            }
            driver.giveMana(driver.player1, Color.BLUE, 1)
            val redirect = driver.putCardInHand(driver.player1, "Test Retargeter")
            driver.castSpellWithTargets(
                driver.player1, redirect, listOf(ChosenTarget.Spell(boltOnStack))
            ).error shouldBe null

            // Resolve the retargeter; it pauses to let its controller pick a new target.
            driver.bothPass()
            val decision = driver.state.pendingDecision as? SelectCardsDecision
            decision.shouldNotBeNull()

            val result = driver.submitCardSelection(driver.player1, listOf(driver.player1))
            withClue("the retarget path is silent for every target kind today") {
                becomesTargetEvents(result) shouldHaveSize 0
            }
        }
    }

    // =========================================================================
    // Matching — the two axes, against synthesized events
    // =========================================================================

    context("matching") {

        fun targeted(
            driver: GameTestDriver,
            targetId: EntityId,
            controllerId: EntityId,
            sourceIsSpell: Boolean,
            targetIsPlayer: Boolean,
        ) = BecomesTargetEvent(
            targetEntityId = targetId,
            targetName = "",
            sourceEntityId = driver.state.stack.firstOrNull() ?: EntityId.generate(),
            controllerId = controllerId,
            firstTimeByThisController = true,
            targetIsSpell = false,
            sourceIsSpell = sourceIsSpell,
            targetIsPlayer = targetIsPlayer,
        )

        fun firings(driver: GameTestDriver, event: BecomesTargetEvent, observerId: EntityId) =
            TriggerDetector(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null), conditionEvaluator = PredicateEvaluator(cardRegistry = null).conditions)
                .detectTriggers(driver.state, listOf(event))
                .filter { it.ability.trigger is EventPattern.BecomesTargetEvent && it.sourceId == observerId }

        test("abilitiesOnly fires on an ability and stays quiet on a spell") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Ability Object Observer")
            val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

            withClue("targeted by an ability") {
                firings(
                    driver,
                    targeted(driver, bears, driver.player1, sourceIsSpell = false, targetIsPlayer = false),
                    observer
                ) shouldHaveSize 1
            }
            withClue("targeted by a spell — abilitiesOnly must reject it") {
                firings(
                    driver,
                    targeted(driver, bears, driver.player1, sourceIsSpell = true, targetIsPlayer = false),
                    observer
                ) shouldHaveSize 0
            }
        }

        test("spellsOnly is unchanged and remains the exact mirror") {
            val driver = createDriver()
            val observer = driver.putCreatureOnBattlefield(driver.player1, "Spell Only Observer")
            val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

            firings(
                driver,
                targeted(driver, bears, driver.player1, sourceIsSpell = true, targetIsPlayer = false),
                observer
            ) shouldHaveSize 1
            firings(
                driver,
                targeted(driver, bears, driver.player1, sourceIsSpell = false, targetIsPlayer = false),
                observer
            ) shouldHaveSize 0
        }

        test("includePlayerTargets is required for a targeted player to fire anything") {
            val driver = createDriver()
            val loki = driver.putCreatureOnBattlefield(driver.player1, "Ability Player Observer")
            val objectOnly = driver.putCreatureOnBattlefield(driver.player1, "Ability Object Observer")
            val plain = driver.putCreatureOnBattlefield(driver.player1, "Plain Becomes Target Observer")

            val event = targeted(
                driver, driver.player2, driver.player1, sourceIsSpell = false, targetIsPlayer = true
            )

            withClue("the opted-in observer fires") {
                firings(driver, event, loki) shouldHaveSize 1
            }
            withClue("abilitiesOnly without the player opt-in must not fire") {
                firings(driver, event, objectOnly) shouldHaveSize 0
            }
            withClue("the pre-existing axis-free wording must not fire on a player") {
                firings(driver, event, plain) shouldHaveSize 0
            }
        }

        test("a player targeted by a SPELL does not fire an abilitiesOnly observer") {
            val driver = createDriver()
            val loki = driver.putCreatureOnBattlefield(driver.player1, "Ability Player Observer")
            val anySource = driver.putCreatureOnBattlefield(driver.player1, "Any Source Player Observer")

            val event = targeted(
                driver, driver.player2, driver.player1, sourceIsSpell = true, targetIsPlayer = true
            )

            withClue("Loki's wording is abilities only") {
                firings(driver, event, loki) shouldHaveSize 0
            }
            withClue("the same player opt-in without abilitiesOnly still fires on a spell") {
                firings(driver, event, anySource) shouldHaveSize 1
            }
        }

        test("an ability targeting its own controller fires the player observer") {
            val driver = createDriver()
            val loki = driver.putCreatureOnBattlefield(driver.player1, "Ability Player Observer")

            // "target player" can be the ability's own controller — nothing in the targeting rules
            // excludes yourself — and Loki's wording has no "an opponent" qualifier, so it fires.
            val event = targeted(
                driver, driver.player1, driver.player1, sourceIsSpell = false, targetIsPlayer = true
            )
            firings(driver, event, loki) shouldHaveSize 1
        }

        test("byYou still filters: an opponent's ability targeting a player does not fire it") {
            val driver = createDriver()
            val loki = driver.putCreatureOnBattlefield(driver.player1, "Ability Player Observer")

            val event = targeted(
                driver, driver.player1, driver.player2, sourceIsSpell = false, targetIsPlayer = true
            )
            firings(driver, event, loki) shouldHaveSize 0
        }

        test("permanent targets still fire the opted-in observer — the player axis only widens") {
            val driver = createDriver()
            val loki = driver.putCreatureOnBattlefield(driver.player1, "Ability Player Observer")
            val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

            val event = targeted(
                driver, bears, driver.player1, sourceIsSpell = false, targetIsPlayer = false
            )
            firings(driver, event, loki) shouldHaveSize 1
        }
    }

    // =========================================================================
    // Pattern data
    // =========================================================================

    test("spellsOnly and abilitiesOnly together are rejected at construction") {
        val thrown = runCatching {
            EventPattern.BecomesTargetEvent(spellsOnly = true, abilitiesOnly = true)
        }.exceptionOrNull()
        thrown.shouldNotBeNull()
    }

    test("the pattern describes both new axes") {
        EventPattern.BecomesTargetEvent(
            abilitiesOnly = true, includePlayerTargets = true, byYou = true
        ).description shouldBe "a player or permanent becomes the target of an ability you control"
    }

    test("the pre-existing descriptions are untouched") {
        EventPattern.BecomesTargetEvent().description shouldBe
            "a card or permanent becomes the target of a spell or ability"
        EventPattern.BecomesTargetEvent(spellsOnly = true).description shouldBe
            "a card or permanent becomes the target of a spell"
    }
})
