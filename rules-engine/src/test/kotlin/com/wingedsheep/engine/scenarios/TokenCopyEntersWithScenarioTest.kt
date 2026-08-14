package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.SpiderPunk
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine mechanic: **token copies run the copied card's "enters-with" replacements** (CR 707.2 — a
 * copy has the copied card's abilities). Exercises the four token-copy executors' shared
 * enters-with pipeline through [com.wingedsheep.engine.handlers.effects.token.CreateTokenCopyOfTargetExecutor]:
 *
 *  1. a token copy of a creature that "enters with a +1/+1 counter" (CR 614.1c) gets the counter;
 *  2. a token copy of a Spider entering while Spider-Punk grants riot (CR 702.136) gets the
 *     enters-with choice, and choosing the counter mode adds a +1/+1 counter;
 *  3. two riot granters give the token two separate choices (CR 702.136b);
 *  4. a multi-token copy where each token gets its own choice — proving the per-token pause loop.
 */
class TokenCopyEntersWithScenarioTest : FunSpec({

    // A creature that enters with a +1/+1 counter (a printed self EntersWithCounters replacement).
    val counterCreature = card("Test Counter Bearer") {
        manaCost = "{2}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
        replacementEffect(
            EntersWithCounters(
                counterType = CounterTypeFilter.PlusOnePlusOne,
                count = 1,
                selfOnly = true,
            )
        )
    }

    // A plain Spider, to exercise granted riot on a token copy.
    val testSpider = card("Test Web-Spinner") {
        manaCost = "{2}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Spider"
        power = 1
        toughness = 1
    }

    // A second, non-legendary lord granting riot to Spiders — a Spider can then carry two grants.
    val testRiotLord = card("Test Riot Lord") {
        manaCost = "{2}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        staticAbility {
            ability = GrantKeyword(
                Keyword.RIOT,
                GroupFilter(GameObjectFilter.Creature.withSubtype("Spider").youControl(), excludeSelf = true),
            )
        }
    }

    // "Create a token that's a copy of target creature."
    val duplicator = card("Test Duplicator") {
        manaCost = "{1}{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        spell {
            val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
            effect = Effects.CreateTokenCopyOfTarget(t)
        }
    }

    // "Create two tokens that are copies of target creature."
    val doubleDuplicator = card("Test Double Duplicator") {
        manaCost = "{1}{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        spell {
            val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
            effect = Effects.CreateTokenCopyOfTarget(t, count = 2)
        }
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(SpiderPunk, counterCreature, testSpider, testRiotLord, duplicator, doubleDuplicator)
        )
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun plusOne(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Token copies of [name] you control (excludes the original nontoken permanent). */
    fun tokenCopies(driver: GameTestDriver, you: EntityId, name: String): List<EntityId> =
        driver.state.getBattlefield().filter { id ->
            val e = driver.state.getEntity(id) ?: return@filter false
            e.get<CardComponent>()?.name == name &&
                e.get<TokenComponent>() != null &&
                driver.state.projectedState.getController(id) == you
        }

    fun chooseRiotMode(driver: GameTestDriver, you: EntityId, needle: String) {
        val pick = driver.pendingDecision as ChooseOptionDecision
        val idx = pick.options.indexOfFirst { it.contains(needle, ignoreCase = true) }
        driver.submitDecision(you, OptionChosenResponse(pick.id, idx))
        settle(driver)
    }

    fun castCopy(driver: GameTestDriver, you: EntityId, spellName: String, targetId: EntityId) {
        driver.giveMana(you, Color.BLUE, 1)
        driver.giveColorlessMana(you, 1)
        val spell = driver.putCardInHand(you, spellName)
        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Permanent(targetId)))
        settle(driver)
    }

    test("token copy of a creature that enters with a +1/+1 counter gets the counter") {
        val (driver, you) = newGame()
        val original = driver.putCreatureOnBattlefield(you, "Test Counter Bearer")

        castCopy(driver, you, "Test Duplicator", original)

        val tokens = tokenCopies(driver, you, "Test Counter Bearer")
        tokens.size shouldBe 1
        plusOne(driver, tokens.first()) shouldBe 1
    }

    test("token copy of a Spider while Spider-Punk grants riot gets the enters-with choice") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk") // grants riot to other Spiders
        val spider = driver.putCreatureOnBattlefield(you, "Test Web-Spinner")

        castCopy(driver, you, "Test Duplicator", spider) // pauses on the synthesized riot choice
        chooseRiotMode(driver, you, "counter")

        val tokens = tokenCopies(driver, you, "Test Web-Spinner")
        tokens.size shouldBe 1
        plusOne(driver, tokens.first()) shouldBe 1
    }

    test("two riot granters give a token copy two separate choices (CR 702.136b)") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk")
        driver.putCreatureOnBattlefield(you, "Test Riot Lord")
        val spider = driver.putCreatureOnBattlefield(you, "Test Web-Spinner")

        castCopy(driver, you, "Test Duplicator", spider)
        chooseRiotMode(driver, you, "counter") // first granted instance
        chooseRiotMode(driver, you, "counter") // second instance re-pauses separately

        val tokens = tokenCopies(driver, you, "Test Web-Spinner")
        tokens.size shouldBe 1
        plusOne(driver, tokens.first()) shouldBe 2
    }

    test("multi-token copy: each token gets its own enters-with choice (per-token pause loop)") {
        val (driver, you) = newGame()
        driver.putCreatureOnBattlefield(you, "Spider-Punk")
        val spider = driver.putCreatureOnBattlefield(you, "Test Web-Spinner")

        castCopy(driver, you, "Test Double Duplicator", spider)
        chooseRiotMode(driver, you, "counter") // first token
        chooseRiotMode(driver, you, "counter") // second token, created only after the first resolved

        val tokens = tokenCopies(driver, you, "Test Web-Spinner")
        tokens.size shouldBe 2
        tokens.forEach { plusOne(driver, it) shouldBe 1 }
    }
})
