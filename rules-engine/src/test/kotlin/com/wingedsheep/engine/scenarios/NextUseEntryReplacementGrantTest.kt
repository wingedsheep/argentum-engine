package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Feature test for a one-shot granted entry replacement: `GrantReplacementEffect(EntersWithCounters,
 * Self, Duration.NextUse)` — "the next time one or more enchantment creatures you control enter this
 * turn, each enters with two additional +1/+1 counters on it" (Storyweave).
 *
 * A replacement effect modifies one event; "the next time … enter" is a single event, so
 * every matching object entering in it is modified, and the effect is used up once it has applied.
 * An entry it doesn't apply to (a non-enchantment creature, an opponent's creature) leaves it
 * waiting; unused, it ends with the turn.
 */
class NextUseEntryReplacementGrantTest : FunSpec({

    val enchantmentCreatures = GameObjectFilter.Creature.youControl() and GameObjectFilter.Enchantment

    val weave = card("Test Weave") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "The next time one or more enchantment creatures you control enter this turn, each enters with two additional +1/+1 counters on it."
        spell {
            effect = Effects.GrantReplacementEffect(
                EntersWithCounters(
                    counterType = CounterType.PLUS_ONE_PLUS_ONE,
                    count = 2,
                    appliesTo = EventPattern.ZoneChangeEvent(filter = enchantmentCreatures, to = Zone.BATTLEFIELD)
                ),
                EffectTarget.Self,
                Duration.NextUse("one or more enchantment creatures you control enter")
            )
        }
    }

    fun spiritToken(count: Int, controller: EffectTarget? = null) = Effects.CreateToken(
        power = 1, toughness = 1, colors = setOf(Color.WHITE), creatureTypes = setOf("Spirit"),
        count = count, controller = controller, enchantmentToken = true
    )

    val spiritGift = card("Test Spirit Gift") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Each opponent creates a 1/1 white Spirit enchantment creature token."
        spell { effect = spiritToken(1, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }

    val twinSpirits = card("Test Twin Spirits") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Create two 1/1 white Spirit enchantment creature tokens."
        spell { effect = spiritToken(2) }
    }

    val spiritThenSpirit = card("Test Spirit Then Spirit") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Create a 1/1 white Spirit enchantment creature token. Then create another."
        spell { effect = spiritToken(1) then spiritToken(1) }
    }

    val gladeSpirit = card("Test Glade Spirit") {
        manaCost = "{G}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 2
    }

    data class Setup(val driver: GameTestDriver, val me: EntityId, val opponent: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(weave, twinSpirits, spiritThenSpirit, gladeSpirit, spiritGift))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Setup(driver, me, driver.getOpponent(me))
    }

    fun GameTestDriver.cast(player: EntityId, name: String) {
        val spell = putCardInHand(player, name)
        giveMana(player, Color.GREEN, 1)
        castSpell(player, spell).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.named(name: String) = state.getBattlefield()
        .filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    fun GameTestDriver.spirits() = state.getBattlefield()
        .filter { state.getEntity(it)?.has<TokenComponent>() == true }

    test("every enchantment creature entering in the same event gets the counters") {
        val (driver, me) = setup()
        driver.cast(me, "Test Weave")
        driver.cast(me, "Test Twin Spirits")

        driver.spirits().map { driver.plusOnes(it) } shouldBe listOf(2, 2)
        driver.state.grantedReplacementEffects.shouldBeEmpty()
    }

    test("it is used up by the first event: a second entry in the same resolution gets nothing") {
        val (driver, me) = setup()
        driver.cast(me, "Test Weave")
        driver.cast(me, "Test Spirit Then Spirit")

        driver.spirits().map { driver.plusOnes(it) }.sorted() shouldBe listOf(0, 2)
    }

    test("a resolving enchantment creature spell gets it, and the next one does not") {
        val (driver, me) = setup()
        driver.cast(me, "Test Weave")
        driver.cast(me, "Test Glade Spirit")
        driver.cast(me, "Test Glade Spirit")

        driver.named("Test Glade Spirit").map { driver.plusOnes(it) }.sorted() shouldBe listOf(0, 2)
    }

    test("a non-enchantment creature entering neither gets it nor uses it up") {
        val (driver, me) = setup()
        driver.cast(me, "Test Weave")
        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveMana(me, Color.GREEN, 3)
        driver.castSpell(me, courser).error shouldBe null
        driver.bothPass()
        driver.plusOnes(courser) shouldBe 0

        driver.cast(me, "Test Glade Spirit")
        driver.named("Test Glade Spirit").map { driver.plusOnes(it) } shouldBe listOf(2)
    }

    test("an opponent's enchantment creature neither gets it nor uses it up") {
        val (driver, me, opponent) = setup()
        driver.cast(me, "Test Weave")
        driver.cast(me, "Test Spirit Gift")
        val theirs = driver.spirits().single()
        driver.state.projectedState.getController(theirs) shouldBe opponent
        driver.plusOnes(theirs) shouldBe 0

        driver.cast(me, "Test Twin Spirits")
        driver.spirits().filter { driver.state.projectedState.getController(it) == me }
            .map { driver.plusOnes(it) } shouldBe listOf(2, 2)
    }

    test("unused, it ends with the turn") {
        val (driver, me) = setup()
        driver.cast(me, "Test Weave")
        driver.state.grantedReplacementEffects.size shouldBe 1

        driver.passPriorityUntil(Step.UPKEEP, driver.getOpponent(me))
        driver.state.grantedReplacementEffects.shouldBeEmpty()
    }
})
