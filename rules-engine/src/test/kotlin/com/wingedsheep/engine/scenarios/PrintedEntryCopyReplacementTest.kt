package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Feature test for a **printed** "<entrants> enter as a copy of <that object>" replacement: an
 * `EntersAsCopy(copyOf = …)` on a battlefield permanent whose reference is relative to its source —
 * "Nontoken creatures you control enter as a copy of enchanted creature" (Infinite Reflection,
 * `EffectTarget.EnchantedCreature`) or "Creatures you control enter as a copy of this" (`Self`).
 *
 * What it pins (Infinite Reflection's rulings, read through the shared entry path the granted form
 * uses): every way a matching creature enters — a resolving spell, an effect putting a card onto the
 * battlefield, a token — is modified, only while the source is on the battlefield; the entrant's
 * filter reads the controller it enters under; the copy applies first, so the copied creature's own
 * enters-with counters and enters triggers apply and an entering Clone isn't asked; the reference is
 * read as each creature enters (a re-attached Aura copies its new host); and with two such
 * replacements copying different creatures the entrant's controller picks the copy applied last
 * (CR 616.1). The replacement is never spent.
 */
class PrintedEntryCopyReplacementTest : FunSpec({

    val mirrorAura = card("Test Mirror Aura") {
        manaCost = "{U}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature\nNontoken creatures you control enter as a copy of enchanted creature."
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        replacementEffect(
            EntersAsCopy(
                optional = false,
                copyOf = EffectTarget.EnchantedCreature,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.nontoken().youControl(), to = Zone.BATTLEFIELD
                )
            )
        )
    }

    val essence = card("Test Essence") {
        manaCost = "{G}"
        typeLine = "Creature — Avatar"
        oracleText = "Creatures you control enter as a copy of this creature."
        power = 6
        toughness = 6
        replacementEffect(
            EntersAsCopy(
                optional = false,
                copyOf = EffectTarget.Self,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.youControl(), to = Zone.BATTLEFIELD
                )
            )
        )
    }

    val seer = card("Test Seer") {
        manaCost = "{U}"
        typeLine = "Creature — Human Wizard"
        oracleText = "When this creature enters, draw a card."
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.DrawCards(1)
        }
    }

    val hydra = card("Test Hydra") {
        manaCost = "{G}"
        typeLine = "Creature — Hydra"
        oracleText = "This creature enters with two +1/+1 counters on it."
        power = 1
        toughness = 1
        replacementEffect(EntersWithCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 2, selfOnly = true))
    }

    val giant = card("Test Giant") {
        manaCost = "{G}"
        typeLine = "Creature — Giant"
        power = 3
        toughness = 3
    }

    val clone = card("Test Clone") {
        manaCost = "{U}"
        typeLine = "Creature — Shapeshifter"
        oracleText = "You may have this creature enter as a copy of any creature on the battlefield."
        power = 0
        toughness = 0
        replacementEffect(EntersAsCopy(optional = true))
    }

    val twinWolves = card("Test Twin Wolves") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Create two 2/2 green Wolf creature tokens."
        spell {
            effect = Effects.CreateToken(
                power = 2, toughness = 2, colors = setOf(Color.GREEN), creatureTypes = setOf("Wolf"), count = 2
            )
        }
    }

    val reanimate = card("Test Reanimate") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Put target creature card from a graveyard onto the battlefield under your control."
        spell {
            val t = target(TargetFilter.CreatureInGraveyard)
            effect = Effects.Move(t, Zone.BATTLEFIELD, controllerOverride = EffectTarget.Controller, fromZone = Zone.GRAVEYARD)
        }
    }

    val reattach = card("Test Reattach") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Attach target Aura you control to another creature."
        spell {
            val aura = target(TargetFilter(GameObjectFilter.Enchantment.withSubtype("Aura").youControl()))
            effect = Effects.AttachToChosenHost(aura)
        }
    }

    data class Setup(val driver: GameTestDriver, val me: EntityId, val opponent: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(mirrorAura, essence, seer, hydra, giant, clone, twinWolves, reanimate, reattach)
        )
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Setup(driver, me, driver.getOpponent(me))
    }

    fun GameTestDriver.cast(player: EntityId, name: String, targets: List<EntityId> = emptyList()): EntityId {
        val spell = putCardInHand(player, name)
        giveMana(player, Color.GREEN, 1)
        giveMana(player, Color.BLUE, 1)
        giveMana(player, Color.RED, 1)
        castSpell(player, spell, targets).error shouldBe null
        bothPass()
        return spell
    }

    fun GameTestDriver.nameOf(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    fun GameTestDriver.named(name: String) = state.getBattlefield().filter { nameOf(it) == name }

    fun GameTestDriver.enchant(player: EntityId, host: EntityId) = cast(player, "Test Mirror Aura", listOf(host))

    test("a creature spell you cast enters as a copy of the enchanted creature, every time") {
        val (driver, me) = setup()
        val host = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.enchant(me, host)

        driver.cast(me, "Test Seer")
        driver.cast(me, "Test Seer")
        withClue("both Seers entered as Giants; the printed replacement is never spent") {
            driver.named("Test Seer").shouldBeEmpty()
            driver.named("Test Giant") shouldHaveSize 3
        }
    }

    test("it copies an opponent's creature it enchants, and leaves the opponent's own creatures alone") {
        val (driver, me, opponent) = setup()
        val host = driver.putCreatureOnBattlefield(opponent, "Test Hydra")
        driver.enchant(me, host)

        driver.cast(me, "Test Giant")
        driver.getCreatures(me).map { driver.nameOf(it) } shouldBe listOf("Test Hydra")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.cast(opponent, "Test Giant")
        withClue("the opponent's Giant enters as itself: not a creature the Aura's controller controls") {
            driver.getCreatures(opponent).mapNotNull { driver.nameOf(it) }.sorted() shouldBe listOf("Test Giant", "Test Hydra")
        }
    }

    test("tokens are not nontoken creatures; a 'creatures you control' copy takes them too") {
        val (driver, me) = setup()
        val host = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.enchant(me, host)
        driver.cast(me, "Test Twin Wolves")
        val tokens = driver.state.getBattlefield().filter { driver.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens shouldHaveSize 2
        withClue("tokens aren't nontoken creatures: they enter as Wolves") {
            tokens.none { driver.nameOf(it) == "Test Giant" } shouldBe true
        }

        val (driver2, me2) = setup()
        driver2.putCreatureOnBattlefield(me2, "Test Essence")
        driver2.cast(me2, "Test Twin Wolves")
        val tokens2 = driver2.state.getBattlefield().filter { driver2.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens2.map { driver2.nameOf(it) } shouldBe listOf("Test Essence", "Test Essence")
    }

    test("a card put onto the battlefield by an effect enters as a copy, judged under the controller it enters under") {
        val (driver, me, opponent) = setup()
        val host = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.enchant(me, host)
        val dead = driver.putCardInGraveyard(opponent, "Test Seer")

        val spell = driver.putCardInHand(me, "Test Reanimate")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(dead, opponent, Zone.GRAVEYARD))).error shouldBe null
        driver.bothPass()

        withClue("the opponent's Seer, entering under my control, is a creature I control as it enters") {
            driver.state.getBattlefield().contains(dead) shouldBe true
            driver.nameOf(dead) shouldBe "Test Giant"
        }
    }

    test("the copy applies first: the copied creature's enters-with counters and enters trigger apply") {
        val (driver, me) = setup()
        val hydra = driver.putCreatureOnBattlefield(me, "Test Hydra")
        driver.enchant(me, hydra)
        driver.cast(me, "Test Giant")
        val copy = driver.named("Test Hydra").single { it != hydra }
        driver.state.getEntity(copy)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

        val (driver2, me2) = setup()
        val seer = driver2.putCreatureOnBattlefield(me2, "Test Seer")
        driver2.enchant(me2, seer)
        val handBefore = driver2.state.getHand(me2).size
        driver2.cast(me2, "Test Giant")
        driver2.bothPass()
        withClue("the Giant entered as a Seer and drew") {
            driver2.named("Test Seer") shouldHaveSize 2
            driver2.state.getHand(me2).size shouldBe handBefore + 1
        }
    }

    test("an entering Clone becomes a copy of the enchanted creature without being asked") {
        val (driver, me) = setup()
        val host = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.putCreatureOnBattlefield(me, "Test Seer")
        driver.enchant(me, host)
        driver.cast(me, "Test Clone")
        driver.state.pendingDecision shouldBe null
        driver.named("Test Giant") shouldHaveSize 2
    }

    test("the reference is read as each creature enters: moved to another creature, it copies that one") {
        val (driver, me) = setup()
        val giant = driver.putCreatureOnBattlefield(me, "Test Giant")
        val hydra = driver.putCreatureOnBattlefield(me, "Test Hydra")
        val aura = driver.enchant(me, giant)
        driver.cast(me, "Test Reattach", listOf(aura))
        val pick = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(pick.id, mapOf(0 to listOf(hydra)))).error shouldBe null

        driver.cast(me, "Test Seer")
        driver.named("Test Hydra") shouldHaveSize 2
        driver.named("Test Giant") shouldHaveSize 1
    }

    test("once its source has left the battlefield it no longer applies") {
        val (driver, me) = setup()
        val host = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.enchant(me, host)
        driver.cast(me, "Lightning Bolt", listOf(host))
        driver.state.getBattlefield().contains(host) shouldBe false

        driver.cast(me, "Test Seer")
        driver.named("Test Seer") shouldHaveSize 1
    }

    test("two copying different creatures: the entrant's controller picks the copy applied last") {
        val (driver, me) = setup()
        val giant = driver.putCreatureOnBattlefield(me, "Test Giant")
        val hydra = driver.putCreatureOnBattlefield(me, "Test Hydra")
        driver.enchant(me, giant)
        driver.enchant(me, hydra)

        val seer = driver.putCardInHand(me, "Test Seer")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, seer).error shouldBe null
        driver.bothPass()
        val decision = driver.pendingDecision as SelectCardsDecision
        decision.playerId shouldBe me
        decision.options.toSet() shouldBe setOf(giant, hydra)
        driver.submitDecision(me, CardsSelectedResponse(decision.id, listOf(giant))).error shouldBe null

        driver.named("Test Giant") shouldHaveSize 2
        driver.named("Test Hydra") shouldHaveSize 1
    }
})
