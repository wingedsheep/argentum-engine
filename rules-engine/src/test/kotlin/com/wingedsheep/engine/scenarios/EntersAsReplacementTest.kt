package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAs
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [EntersAs] — "… enters as a Mutant in addition to its other types" (CR 614.1c): a static entry
 * replacement that adds characteristics to the entering object as it arrives. Covers the global
 * ("each other creature you control") and self shapes, every entry path (cast, token, reanimation),
 * that the object's own enters triggers already see the new type, that the change outlives its
 * source, and that a new object (CR 400.7) does not keep it.
 */
class EntersAsReplacementTest : FunSpec({

    val mutant = EntersWithCharacteristics(addedSubtypes = setOf(Subtype("Mutant")))

    val lab = card("Mutant Lab") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Elf Wizard"
        power = 0
        toughness = 3
        replacementEffect(EntersAs(characteristics = mutant, otherOnly = true))
    }
    val vat = card("Vat Grown") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Ooze"
        power = 2
        toughness = 2
        replacementEffect(EntersAs(characteristics = mutant, selfOnly = true))
    }
    val watcher = card("Mutant Watcher") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature.withSubtype("Mutant")).enters()
            effect = Effects.GainLife(1)
        }
    }
    val sproutling = card("Test Sproutling") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.CreateToken(power = 1, toughness = 1, colors = setOf(Color.GREEN), creatureTypes = setOf("Saproling")) }
    }
    val reanimate = card("Test Reanimate") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val creature = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(creature)
        }
    }
    val bounce = card("Test Bounce") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Move(creature, Zone.HAND)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(lab, vat, watcher, sproutling, reanimate, bounce))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isMutant(id: EntityId) = state.projectedState.hasSubtype(id, "Mutant")

    fun GameTestDriver.named(player: EntityId, name: String): EntityId =
        getCreatures(player).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    fun GameTestDriver.cast(player: EntityId, name: String, color: Color, mana: Int, targets: List<EntityId> = emptyList()) {
        val card = putCardInHand(player, name)
        giveMana(player, color, mana)
        castSpell(player, card, targets)
        bothPass()
    }

    test("another creature you control enters as a Mutant in addition to its other types") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Mutant Lab")

        driver.cast(me, "Grizzly Bears", Color.GREEN, 2)
        val bears = driver.named(me, "Grizzly Bears")

        driver.isMutant(bears) shouldBe true
        driver.state.projectedState.hasSubtype(bears, "Bear") shouldBe true
    }

    test("an otherOnly source does not change itself as it enters") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.cast(me, "Mutant Lab", Color.GREEN, 3)

        driver.isMutant(driver.named(me, "Mutant Lab")) shouldBe false
    }

    test("an opponent's source changes nothing of yours") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Mutant Lab")

        driver.cast(me, "Grizzly Bears", Color.GREEN, 2)

        driver.isMutant(driver.named(me, "Grizzly Bears")) shouldBe false
    }

    test("tokens and reanimated creatures enter as Mutants too") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Mutant Lab")

        driver.cast(me, "Test Sproutling", Color.GREEN, 1)
        val token = driver.getCreatures(me).first { driver.state.projectedState.hasSubtype(it, "Saproling") }
        driver.isMutant(token) shouldBe true

        val dead = driver.putCardInGraveyard(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Test Reanimate")
        driver.giveMana(me, Color.BLACK, 1)
        driver.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(dead, me, Zone.GRAVEYARD))).error shouldBe null
        driver.bothPass()
        driver.state.getBattlefield().contains(dead) shouldBe true
        driver.isMutant(dead) shouldBe true
    }

    test("the entering creature's own enters triggers already see the added type") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Mutant Lab")
        driver.putCreatureOnBattlefield(me, "Mutant Watcher")
        val life = driver.getLifeTotal(me)

        driver.cast(me, "Grizzly Bears", Color.GREEN, 2)
        driver.bothPass() // resolve the watcher's trigger

        driver.getLifeTotal(me) shouldBe life + 1
    }

    test("the type outlives its source, but a new object entering without one is not changed") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val labId = driver.putCreatureOnBattlefield(me, "Mutant Lab")
        driver.cast(me, "Grizzly Bears", Color.GREEN, 2)
        val bears = driver.named(me, "Grizzly Bears")

        driver.cast(me, "Test Bounce", Color.BLUE, 1, listOf(labId))
        driver.isMutant(bears) shouldBe true

        driver.cast(me, "Test Bounce", Color.BLUE, 1, listOf(bears))
        driver.cast(me, "Grizzly Bears", Color.GREEN, 2)
        driver.isMutant(driver.named(me, "Grizzly Bears")) shouldBe false
    }

    test("a selfOnly effect changes its own card and nothing else") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        driver.cast(me, "Vat Grown", Color.GREEN, 2)
        driver.isMutant(driver.named(me, "Vat Grown")) shouldBe true

        driver.cast(me, "Grizzly Bears", Color.GREEN, 2)
        driver.isMutant(driver.named(me, "Grizzly Bears")) shouldBe false
    }

    test("pre-entry judgments see a static EntersAs (would a creature enter)") {
        val animator = card("Test Animator") {
            manaCost = "{3}"
            typeLine = "Artifact"
            replacementEffect(
                EntersAs(
                    characteristics = EntersWithCharacteristics.creature(2, 2, "Construct"),
                    otherOnly = true,
                    appliesTo = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(
                        filter = GameObjectFilter.Artifact.youControl(), to = Zone.BATTLEFIELD,
                    ),
                )
            )
        }
        val trinket = card("Test Trinket") {
            manaCost = "{1}"
            typeLine = "Artifact"
        }
        val driver = newDriver()
        driver.registerCards(listOf(animator, trinket))
        val me = driver.activePlayer!!
        val inHand = driver.putCardInHand(me, "Test Trinket")
        fun wouldBeCreature() = com.wingedsheep.engine.handlers.effects.EntryCharacteristics.wouldBeCreature(
            driver.state, inHand, me, null, false, driver.cardRegistry, driver.services.predicateEvaluator,
        )
        wouldBeCreature() shouldBe false

        driver.putPermanentOnBattlefield(me, "Test Animator")
        wouldBeCreature() shouldBe true

        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, inHand)
        driver.bothPass()
        driver.state.projectedState.isCreature(inHand) shouldBe true
        driver.state.projectedState.hasSubtype(inHand, "Construct") shouldBe true
    }

    test("description reads as the printed wording") {
        EntersAs(characteristics = mutant, otherOnly = true).description.contains("enters as a Mutant in addition to its other types") shouldBe true
    }
})
