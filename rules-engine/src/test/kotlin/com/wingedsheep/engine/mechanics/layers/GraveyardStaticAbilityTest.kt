package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.ZoneScopedStaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Static abilities that function from a graveyard — [ZoneScopedStaticAbility] (CR 113.6b: "An
 * ability that states which zones it functions in functions only from those zones").
 *
 * The rules pinned here:
 * - **On in the graveyard, off on the battlefield.** The ability states its zone, so it does not
 *   function from the battlefield (CR 113.6b), and does from the graveyard.
 * - **"You" is the owner.** A card in a graveyard has no controller; CR 108.4a / 109.5 read "you"
 *   on it as its owner — for the condition ("you control a Mountain") and for the affected set
 *   ("creatures you control") alike. An opponent's Mountain or creatures don't count.
 * - **Live for exactly as long as the card is there** — it starts when the card dies and stops the
 *   moment it leaves the graveyard.
 * - **A granted keyword is a projected keyword**, so haste granted from the graveyard lets a
 *   creature that came under its controller's control this turn attack (CR 302.6 reads abilities
 *   through the projection like any other).
 */
class GraveyardStaticAbilityTest : FunSpec({

    // "Haste. As long as this card is in your graveyard and you control a Mountain, creatures you
    // control have haste." — Anger's shape on a test body.
    val incarnation = card("Test Rage Incarnation") {
        manaCost = "{3}{R}"
        typeLine = "Creature — Incarnation"
        power = 2
        toughness = 2
        staticAbility {
            ability = GrantKeyword(Keyword.HASTE, GroupFilter(GameObjectFilter.Creature.youControl()))
            condition = Conditions.YouControl(Filters.MountainCard)
            activeZones = setOf(Zone.GRAVEYARD)
        }
    }

    // An unconditional graveyard lord, to separate the zone gate from the condition.
    val gravePumper = card("Test Grave Pumper") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        staticAbility {
            ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.youControl()))
            activeZones = setOf(Zone.GRAVEYARD)
        }
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(incarnation)
        driver.registerCard(gravePumper)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.hasHaste(id: EntityId) = state.projectedState.hasKeyword(id, Keyword.HASTE)

    test("the DSL wraps a graveyard-scoped static and leaves a battlefield one bare") {
        incarnation.script.staticAbilities.single().shouldBeInstanceOf<ZoneScopedStaticAbility>()
            .activeZones shouldBe setOf(Zone.GRAVEYARD)
        shouldThrow<IllegalArgumentException> {
            ZoneScopedStaticAbility(GrantKeyword(Keyword.HASTE, GroupFilter.source()), setOf(Zone.BATTLEFIELD))
        }
    }

    test("from the graveyard with a Mountain, the owner's creatures have haste") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.putLandOnBattlefield(you, "Mountain")
        driver.hasHaste(bear) shouldBe false

        driver.putCardInGraveyard(you, "Test Rage Incarnation")
        driver.hasHaste(bear) shouldBe true
    }

    test("without a Mountain the grant is off, and an opponent's Mountain is not yours") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.putCardInGraveyard(you, "Test Rage Incarnation")
        driver.hasHaste(bear) shouldBe false

        driver.putLandOnBattlefield(opponent, "Mountain")
        withClue("\"you control a Mountain\" reads the card's owner, not any player") {
            driver.hasHaste(bear) shouldBe false
        }

        driver.putLandOnBattlefield(you, "Mountain")
        driver.hasHaste(bear) shouldBe true
    }

    test("only the owner's creatures are affected, in either player's graveyard") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val yourBear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putLandOnBattlefield(you, "Mountain")
        driver.putLandOnBattlefield(opponent, "Mountain")

        driver.putCardInGraveyard(you, "Test Rage Incarnation")
        driver.hasHaste(yourBear) shouldBe true
        driver.hasHaste(theirBear) shouldBe false

        val pumper = driver.putCardInGraveyard(opponent, "Test Grave Pumper")
        driver.state.projectedState.getPower(theirBear) shouldBe 3
        driver.state.projectedState.getPower(yourBear) shouldBe 2
        // The graveyard card itself is not a creature on the battlefield.
        driver.state.projectedState.getPower(pumper) shouldBe null
    }

    test("on the battlefield the ability does not function; it switches on when the card dies") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.putLandOnBattlefield(you, "Mountain")
        val rage = driver.putCreatureOnBattlefield(you, "Test Rage Incarnation")

        withClue("CR 113.6b — a graveyard-scoped ability is off on the battlefield") {
            driver.hasHaste(bear) shouldBe false
            driver.hasHaste(rage) shouldBe false
        }

        // Kill it the real way, so the card crosses the zone change it would in a game.
        driver.putLandOnBattlefield(you, "Mountain")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.RED, 1)
        driver.castSpell(you, bolt, listOf(rage)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.assertInGraveyard(you, "Test Rage Incarnation")
        driver.hasHaste(bear) shouldBe true
    }

    test("the grant stops the moment the card leaves the graveyard") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        driver.putLandOnBattlefield(you, "Mountain")
        val rage = driver.putCardInGraveyard(you, "Test Rage Incarnation")
        driver.hasHaste(bear) shouldBe true

        driver.replaceState(
            driver.state
                .removeFromZone(ZoneKey(you, Zone.GRAVEYARD), rage)
                .addToZone(ZoneKey(you, Zone.EXILE), rage)
        )
        driver.hasHaste(bear) shouldBe false
    }

    test("haste granted from the graveyard lets a creature that just arrived attack this turn") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putLandOnBattlefield(you, "Mountain")
        driver.putCardInGraveyard(you, "Test Rage Incarnation")
        // Placed by the helper with summoning sickness, as if it had just entered.
        val bear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val round = driver.state.turnNumber

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // The engine skips declare-attackers when nothing can attack, so a same-round step proves
        // the bear was a legal attacker this turn.
        driver.state.turnNumber shouldBe round
        driver.declareAttackers(you, listOf(bear), opponent).outcome shouldBe Outcome.Done
    }
})
