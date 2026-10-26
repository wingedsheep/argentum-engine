package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
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
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Feature test for a granted "enters as a copy of *that* object" replacement:
 * `GrantReplacementEffect(EntersAsCopy(copyOf = <target>), Self, Duration.NextUse)` — "the next time
 * one or more creatures or planeswalkers enter this turn, they enter as copies of the chosen
 * creature" (Mystic Reflection).
 *
 * The rulings it pins: every creature entering in that one event becomes a copy, whoever controls it
 * and however it enters (a resolving spell, tokens, an effect putting a card onto the battlefield);
 * the copy applies first, so the copied creature's own "enters with" and enters triggers work and a
 * Clone's own copy choice is moot; a chosen creature that has left the battlefield is copied as it
 * last existed; an entry it doesn't apply to leaves it waiting, and unused it ends with the turn.
 */
class GrantedEntryCopyReplacementTest : FunSpec({

    val reflection = card("Test Reflection") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Choose target nonlegendary creature. The next time one or more creatures or planeswalkers enter this turn, they enter as copies of the chosen creature."
        spell {
            val chosen = target(TargetFilter.Creature.nonlegendary())
            effect = Effects.GrantReplacementEffect(
                EntersAsCopy(
                    optional = false,
                    copyOf = chosen,
                    appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter.CreatureOrPlaneswalker, to = Zone.BATTLEFIELD)
                ),
                EffectTarget.Self,
                Duration.NextUse("one or more creatures or planeswalkers enter")
            )
        }
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

    val relic = card("Test Relic") {
        manaCost = "{G}"
        typeLine = "Artifact"
    }

    fun wolves(count: Int, controller: EffectTarget? = null) = Effects.CreateToken(
        power = 2, toughness = 2, colors = setOf(Color.GREEN), creatureTypes = setOf("Wolf"),
        count = count, controller = controller
    )

    val twinWolves = card("Test Twin Wolves") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Create two 2/2 green Wolf creature tokens."
        spell { effect = wolves(2) }
    }

    val wolfGift = card("Test Wolf Gift") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Each opponent creates a 2/2 green Wolf creature token."
        spell { effect = wolves(1, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }

    val reanimate = card("Test Reanimate") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Return target creature card from your graveyard to the battlefield."
        spell {
            val t = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        }
    }

    val wolfThenWolf = card("Test Wolf Then Wolf") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Create a 2/2 green Wolf creature token. Then create another."
        spell { effect = wolves(1) then wolves(1) }
    }

    val arbor = card("Test Arbor") {
        typeLine = "Land Creature — Forest Dryad"
        power = 1
        toughness = 1
    }

    data class Setup(val driver: GameTestDriver, val me: EntityId, val opponent: EntityId)

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(reflection, seer, hydra, giant, clone, relic, twinWolves, wolfGift, reanimate, wolfThenWolf, arbor))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Setup(driver, me, driver.getOpponent(me))
    }

    fun GameTestDriver.cast(player: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        val spell = putCardInHand(player, name)
        giveMana(player, Color.GREEN, 1)
        giveMana(player, Color.BLUE, 1)
        giveMana(player, Color.RED, 1)
        castSpell(player, spell, targets).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.reflect(player: EntityId, chosen: EntityId) = cast(player, "Test Reflection", listOf(chosen))

    fun GameTestDriver.nameOf(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name

    fun GameTestDriver.named(name: String) = state.getBattlefield().filter { nameOf(it) == name }

    fun GameTestDriver.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("a resolving creature spell enters as a copy of the chosen creature, and the next one doesn't") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)

        driver.cast(me, "Test Seer")
        withClue("the Seer entered as a copy of the Giant") {
            driver.named("Test Seer").shouldBeEmpty()
            driver.named("Test Giant") shouldHaveSize 2
        }
        driver.state.grantedReplacementEffects.shouldBeEmpty()

        driver.cast(me, "Test Seer")
        driver.named("Test Seer") shouldHaveSize 1
    }

    test("the copy applies first: the copied creature's enters-with counters and enters trigger work") {
        val (driver, me) = setup()
        val hydra = driver.putCreatureOnBattlefield(me, "Test Hydra")
        driver.reflect(me, hydra)
        driver.cast(me, "Test Giant")
        val copies = driver.named("Test Hydra").filter { it != hydra }
        copies shouldHaveSize 1
        driver.plusOnes(copies.single()) shouldBe 2

        val (driver2, me2) = setup()
        val seer = driver2.putCreatureOnBattlefield(me2, "Test Seer")
        driver2.reflect(me2, seer)
        val handBefore = driver2.state.getHand(me2).size
        driver2.cast(me2, "Test Giant")
        driver2.bothPass() // resolve the copied enters trigger
        withClue("the Giant entered as a Seer, and the Seer's draw trigger fired") {
            driver2.named("Test Seer") shouldHaveSize 2
            driver2.state.getHand(me2).size shouldBe handBefore + 1
        }
    }

    test("every creature entering in the same event becomes a copy — two tokens at once") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        driver.cast(me, "Test Twin Wolves")

        val tokens = driver.state.getBattlefield().filter { driver.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens shouldHaveSize 2
        tokens.map { driver.nameOf(it) } shouldBe listOf("Test Giant", "Test Giant")
        driver.state.grantedReplacementEffects.shouldBeEmpty()
    }

    test("it applies to a creature entering under an opponent's control") {
        val (driver, me, opponent) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        driver.cast(me, "Test Wolf Gift")

        driver.getCreatures(opponent).map { driver.nameOf(it) } shouldBe listOf("Test Giant")
    }

    test("a card put onto the battlefield by an effect enters as a copy") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        val dead = driver.putCardInGraveyard(me, "Test Seer")
        driver.reflect(me, chosen)
        val spell = driver.putCardInHand(me, "Test Reanimate")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(dead, me, Zone.GRAVEYARD))).error shouldBe null
        driver.bothPass()

        driver.nameOf(dead) shouldBe "Test Giant"
        driver.state.getBattlefield().contains(dead) shouldBe true
    }

    test("a Clone entering becomes a copy of the chosen creature without asking for its own choice") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.putCreatureOnBattlefield(me, "Test Seer")
        driver.reflect(me, chosen)
        driver.cast(me, "Test Clone")

        driver.state.pendingDecision shouldBe null
        driver.named("Test Giant") shouldHaveSize 2
    }

    test("a chosen creature that has left the battlefield is copied as it last existed") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        driver.cast(me, "Lightning Bolt", listOf(chosen))
        driver.state.getBattlefield().contains(chosen) shouldBe false

        driver.cast(me, "Test Seer")
        driver.named("Test Giant") shouldHaveSize 1
    }

    test("an entry it doesn't apply to leaves it waiting; unused, it ends with the turn") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        driver.cast(me, "Test Relic")
        driver.named("Test Relic") shouldHaveSize 1
        driver.state.grantedReplacementEffects shouldHaveSize 1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.getOpponent(me))
        driver.state.grantedReplacementEffects.shouldBeEmpty()
    }
    test("two grants both apply to the first entry and are both spent; the copy shown is the most recent one's") {
        val (driver, me) = setup()
        val giant = driver.putCreatureOnBattlefield(me, "Test Giant")
        val hydra = driver.putCreatureOnBattlefield(me, "Test Hydra")
        driver.reflect(me, giant)
        driver.reflect(me, hydra)
        driver.state.grantedReplacementEffects shouldHaveSize 2

        driver.cast(me, "Test Seer")
        driver.named("Test Hydra") shouldHaveSize 2
        driver.state.grantedReplacementEffects.shouldBeEmpty()

        driver.cast(me, "Test Seer")
        withClue("the older grant was used up by the same entry, so the next creature enters as itself") {
            driver.named("Test Seer") shouldHaveSize 1
        }
    }

    test("a later, separate entry within the same resolving spell is not copied") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        driver.cast(me, "Test Wolf Then Wolf")

        val tokens = driver.state.getBattlefield().filter { driver.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens.mapNotNull { driver.nameOf(it) }.sorted() shouldBe listOf("Test Giant", "Wolf Token").sorted()
    }

    test("the copy takes the chosen creature's printed values, not its counters or pump") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        driver.cast(me, "Giant Growth", listOf(chosen))
        driver.cast(me, "Test Seer")

        val copy = driver.named("Test Giant").single { it != chosen }
        driver.state.projectedState.getPower(copy) shouldBe 3
        driver.state.projectedState.getToughness(copy) shouldBe 3
    }

    test("a land creature that is played enters as a copy") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        val land = driver.putCardInHand(me, "Test Arbor")
        driver.playLand(me, land).error shouldBe null

        driver.nameOf(land) shouldBe "Test Giant"
        driver.state.grantedReplacementEffects.shouldBeEmpty()
    }

    test("a face-down creature spell is modified too: it stays face down, copying under that status") {
        val (driver, me) = setup()
        val chosen = driver.putCreatureOnBattlefield(me, "Test Giant")
        driver.reflect(me, chosen)
        val morph = driver.putCardInHand(me, "Morph Test Creature")
        driver.giveMana(me, Color.GREEN, 3)
        driver.submit(CastSpell(me, morph, castFaceDown = true)).error shouldBe null
        driver.bothPass()

        driver.state.getBattlefield().contains(morph) shouldBe true
        driver.state.getEntity(morph)?.has<FaceDownComponent>() shouldBe true
        driver.state.projectedState.getPower(morph) shouldBe 2
        driver.nameOf(morph) shouldBe "Test Giant"
        driver.state.grantedReplacementEffects.shouldBeEmpty()
    }
})
