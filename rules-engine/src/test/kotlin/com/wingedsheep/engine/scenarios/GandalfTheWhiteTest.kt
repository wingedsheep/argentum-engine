package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GandalfTheWhite
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.BattlefieldDirection
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gandalf the White ({3}{W}{W} Legendary Creature — Avatar Wizard 4/5):
 *   Flash
 *   You may cast legendary spells and artifact spells as though they had flash.
 *   If a legendary permanent or an artifact entering or leaving the battlefield causes a
 *   triggered ability of a permanent you control to trigger, that ability triggers an
 *   additional time.
 *
 * Each clause is covered:
 *  * Flash + flash-permission static — legendary/artifact spells castable at instant speed,
 *    controller-only; vanilla spells unaffected.
 *  * Trigger-count modifier (CR 603.2d) — ETB and LTB triggers of a permanent the controller
 *    controls fire an additional time when caused by a legendary or artifact entering or
 *    leaving. mustBeYouControl = false on the cause: opponent's legendary/artifact still
 *    triggers the doubling. The "of a permanent you control" half remains controller-only.
 */
class GandalfTheWhiteTest : FunSpec({

    val testLegendaryBeast = CardDefinition.creature(
        name = "Test Legendary Beast",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 2,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY)
    )

    val testArtifact = CardDefinition(
        name = "Test Trinket",
        manaCost = ManaCost.parse("{2}"),
        typeLine = TypeLine.artifact(),
        oracleText = ""
    )

    val testPlainCreature = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    // Artifact creature so the same entity can both die on the battlefield (Doom Blade target)
    // AND satisfy Gandalf's `Artifact ∨ Legendary` filter on the cause event.
    val testArtifactCreature = CardDefinition.artifactCreature(
        name = "Test Construct",
        manaCost = ManaCost.parse("{2}"),
        subtypes = setOf(Subtype("Construct")),
        power = 1,
        toughness = 1
    )

    // "Soul Warden"-ish witness — watches every other creature entering and draws a card.
    // Drawing is observable via hand size; doubling shows up as +2 instead of +1 per ETB.
    val etbWatcher = card("ETB Witness") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1
        oracleText = "Whenever another creature enters the battlefield, draw a card."
        triggeredAbility {
            trigger = Triggers.entersBattlefield(
                filter = GameObjectFilter.Creature,
                binding = TriggerBinding.OTHER
            )
            effect = Effects.DrawCards(1)
        }
    }

    // A non-legendary sibling of Gandalf's third clause — same primitive, broad filter, both
    // directions. Used to assert that two ETB-or-LTB doublers stack additively without
    // dragging the legend rule into the test.
    val testDoubler = card("Test Extra-Trigger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "If a permanent entering or leaving the battlefield causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time."
        staticAbility {
            ability = AdditionalETBOrLTBTriggers(
                filter = GameObjectFilter.Any,
                mustBeYouControl = false,
                directions = setOf(BattlefieldDirection.ENTERING, BattlefieldDirection.LEAVING)
            )
        }
    }

    // A sorcery that makes a legendary 2/2 creature token. Used to prove the LTB doubler reads
    // the cause's last-known type info (CR 603.10a): a token is swept by 704.5d as it dies, so
    // matching its `Legendary`-ness against live state would fail — the doubler must consult the
    // ZoneChangeEvent's last-known type line instead.
    val summonLegendToken = card("Summon Legend") {
        manaCost = "{1}{G}"
        typeLine = "Sorcery"
        oracleText = "Create a legendary 2/2 green Spirit creature token."
        spell {
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Spirit"),
                legendary = true
            )
        }
    }

    // Turns a plain creature into an artifact for as long as it's on the battlefield (Layer 4).
    // Once it dies, its graveyard card line is the printed (non-artifact) one again — only the
    // ZoneChangeEvent's last-known type line still records the artifact-ness. This is the case
    // that distinguishes live-state matching from last-known matching in the LTB doubler.
    val artifactify = card("Artifactify") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Target creature becomes an artifact in addition to its other types."
        spell {
            val creature = target("target creature", Targets.Creature)
            effect = Effects.AddCardType("ARTIFACT", creature)
        }
    }

    // Same shape for LTB. We avoid a graveyard restriction so sacrifice/destroy/return all work.
    val ltbWatcher = card("LTB Witness") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Cleric"
        power = 1
        toughness = 1
        oracleText = "Whenever another creature leaves the battlefield, draw a card."
        triggeredAbility {
            trigger = Triggers.leavesBattlefield(
                filter = GameObjectFilter.Creature,
                binding = TriggerBinding.OTHER
            )
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(
                GandalfTheWhite,
                testLegendaryBeast,
                testArtifact,
                testPlainCreature,
                testArtifactCreature,
                etbWatcher,
                ltbWatcher,
                testDoubler,
                summonLegendToken,
                artifactify
            )
        )
        return driver
    }

    test("controller may cast a legendary creature at instant speed with Gandalf the White") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        val legendary = driver.putCardInHand(p1, "Test Legendary Beast")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = legendary, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    test("controller may cast an artifact at instant speed with Gandalf the White") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        val trinket = driver.putCardInHand(p1, "Test Trinket")
        driver.giveColorlessMana(p1, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = trinket, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe true
    }

    test("a vanilla non-legendary non-artifact creature still can't be cast at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        val bear = driver.putCardInHand(p1, "Test Bear")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.passPriorityUntil(Step.END)

        val result = driver.submit(
            CastSpell(playerId = p1, cardId = bear, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    test("the static is controller-only: an opponent's legendary creature doesn't gain flash") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        val legendary = driver.putCardInHand(p2, "Test Legendary Beast")
        driver.giveMana(p2, Color.GREEN, 2)
        driver.passPriorityUntil(Step.END)
        driver.passPriority(p1)

        val result = driver.submit(
            CastSpell(playerId = p2, cardId = legendary, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.isSuccess shouldBe false
    }

    // ------------------------------------------------------------------
    // Third clause — "triggers an additional time" (CR 603.2d) via
    // AdditionalETBOrLTBTriggers with directions = {ENTERING, LEAVING}.
    // ------------------------------------------------------------------

    test("ETB: a legendary creature entering doubles your ETB-watcher trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p1, "ETB Witness")
        val legendary = driver.putCardInHand(p1, "Test Legendary Beast")
        driver.giveMana(p1, Color.GREEN, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, legendary).isSuccess shouldBe true
        driver.bothPass()  // resolve the legendary
        driver.bothPass()  // first ETB Witness draw
        driver.bothPass()  // duplicated ETB Witness draw

        // Cast (-1) + 2 draws from the doubled trigger = net +1.
        (driver.getHandSize(p1) - handBefore) shouldBe 1
    }

    test("ETB: a non-legendary non-artifact creature does NOT double the trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p1, "ETB Witness")
        val bear = driver.putCardInHand(p1, "Test Bear")
        driver.giveMana(p1, Color.GREEN, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, bear).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()  // single ETB Witness draw

        // Cast (-1) + 1 draw (no doubling) = net 0.
        (driver.getHandSize(p1) - handBefore) shouldBe 0
    }

    test("ETB: mustBeYouControl=false — opponent's legendary still doubles your ETB trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p1, "ETB Witness")
        val legendary = driver.putCardInHand(p2, "Test Legendary Beast")

        // Advance to p2's main phase so they can cast the (non-flash) legendary creature at
        // sorcery speed. Gandalf's flash-permission static is controller-only, so p2 can't
        // borrow flash to cast it on p1's turn.
        driver.passPriorityUntil(Step.UPKEEP)        // p2's upkeep
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN) // p2's precombat main
        driver.giveMana(p2, Color.GREEN, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p2, legendary).isSuccess shouldBe true
        driver.bothPass()  // resolve legendary
        driver.bothPass()  // first ETB Witness draw (p1)
        driver.bothPass()  // duplicated ETB Witness draw (p1)

        // p1 doesn't cast — hand only grows from the doubled draws on p1's ETB Witness.
        (driver.getHandSize(p1) - handBefore) shouldBe 2
    }

    test("ETB: 'of a permanent YOU control' — opponent's ETB Witness is not doubled by your Gandalf") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p2, "ETB Witness")
        val legendary = driver.putCardInHand(p1, "Test Legendary Beast")
        driver.giveMana(p1, Color.GREEN, 2)

        val p2HandBefore = driver.getHandSize(p2)
        driver.castSpell(p1, legendary).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass() // single (un-doubled) opponent draw

        // Opponent's ETB Witness fires once; Gandalf does NOT duplicate it because the
        // trigger's controller is p2, not p1 (Gandalf's controller).
        (driver.getHandSize(p2) - p2HandBefore) shouldBe 1
    }

    test("LTB: an artifact creature dying doubles your LTB-watcher trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p1, "LTB Witness")
        val construct = driver.putCreatureOnBattlefield(p1, "Test Construct")
        val doomBlade = driver.putCardInHand(p1, "Doom Blade")
        driver.giveMana(p1, Color.BLACK, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, doomBlade, listOf(construct)).isSuccess shouldBe true
        driver.bothPass()  // resolve Doom Blade — Test Construct dies
        driver.bothPass()  // first LTB Witness draw
        driver.bothPass()  // duplicated LTB Witness draw

        // p1 cast (-1 from hand) + 2 draws = net +1.
        (driver.getHandSize(p1) - handBefore) shouldBe 1
    }

    test("LTB: a legendary creature TOKEN dying doubles your trigger via last-known type info (CR 603.10a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p1, "LTB Witness")

        // Make the legendary token by resolving a real spell, so it enters legitimately.
        val summon = driver.putCardInHand(p1, "Summon Legend")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.castSpell(p1, summon).isSuccess shouldBe true
        driver.bothPass()  // resolve Summon Legend → legendary 2/2 Spirit token

        // The only creature that's neither Gandalf nor the witness is the token.
        val token = driver.getCreatures(p1).first { id ->
            driver.getCardName(id) !in setOf("Gandalf the White", "LTB Witness")
        }

        val doomBlade = driver.putCardInHand(p1, "Doom Blade")
        driver.giveMana(p1, Color.BLACK, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, doomBlade, listOf(token)).isSuccess shouldBe true
        driver.bothPass()  // resolve Doom Blade — the legendary token dies and is swept (704.5d)
        driver.bothPass()  // first LTB Witness draw
        driver.bothPass()  // duplicated LTB Witness draw (Gandalf, matching the token's last-known Legendary)

        // Doom Blade cast (-1) + 2 draws from the doubled trigger = net +1.
        (driver.getHandSize(p1) - handBefore) shouldBe 1
    }

    test("LTB: a creature made an artifact by a continuous effect, then dying, doubles via last-known type (CR 603.10a)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p1, "LTB Witness")
        // Plain (non-artifact, non-legendary) creature — its PRINTED type never matches Gandalf.
        val bear = driver.putCreatureOnBattlefield(p1, "Test Bear")

        // Make the bear an artifact for as long as it's on the battlefield.
        val artifactify = driver.putCardInHand(p1, "Artifactify")
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, artifactify, listOf(bear)).isSuccess shouldBe true
        driver.bothPass()  // resolve Artifactify — bear is now an artifact creature on the battlefield

        val doomBlade = driver.putCardInHand(p1, "Doom Blade")
        driver.giveMana(p1, Color.BLACK, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, doomBlade, listOf(bear)).isSuccess shouldBe true
        driver.bothPass()  // resolve Doom Blade — the (now-artifact) bear dies; in the graveyard it
                           // is a plain creature again, so only last-known info records the artifact type
        driver.bothPass()  // first LTB Witness draw
        driver.bothPass()  // duplicated LTB Witness draw (Gandalf matches the bear's last-known Artifact type)

        // Doom Blade cast (-1) + 2 draws from the doubled trigger = net +1.
        (driver.getHandSize(p1) - handBefore) shouldBe 1
    }

    test("LTB: a non-legendary non-artifact creature dying does NOT double the trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putCreatureOnBattlefield(p1, "LTB Witness")
        val bear = driver.putCreatureOnBattlefield(p1, "Test Bear")
        val doomBlade = driver.putCardInHand(p1, "Doom Blade")
        driver.giveMana(p1, Color.BLACK, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, doomBlade, listOf(bear)).isSuccess shouldBe true
        driver.bothPass()  // resolve Doom Blade
        driver.bothPass()  // single LTB Witness draw

        // p1 cast (-1 from hand) + 1 draw = net 0.
        (driver.getHandSize(p1) - handBefore) shouldBe 0
    }

    test("two ETB-or-LTB doublers stack additively: an artifact ETB triggers the watcher three times") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Gandalf + a non-legendary sibling using the same primitive. Avoids the legend rule
        // SBA that two Gandalfs would invoke once a spell resolves.
        driver.putPermanentOnBattlefield(p1, "Gandalf the White")
        driver.putPermanentOnBattlefield(p1, "Test Extra-Trigger")
        driver.putCreatureOnBattlefield(p1, "ETB Witness")
        val construct = driver.putCardInHand(p1, "Test Construct")
        driver.giveColorlessMana(p1, 2)

        val handBefore = driver.getHandSize(p1)
        driver.castSpell(p1, construct).isSuccess shouldBe true
        driver.bothPass()  // resolve Test Construct ETB
        driver.bothPass()  // original ETB Witness draw
        driver.bothPass()  // duplicated by Gandalf
        driver.bothPass()  // duplicated by Test Extra-Trigger

        // p1 cast (-1 from hand) + 3 draws = net +2.
        (driver.getHandSize(p1) - handBefore) shouldBe 2
    }
})
