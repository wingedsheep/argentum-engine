package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.MaskwoodNexus
import com.wingedsheep.mtg.sets.definitions.khm.cards.Realmwalker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Realmwalker (KHM #188) — {2}{G} Creature — Shapeshifter, 2/3.
 *
 * "Changeling. As this creature enters, choose a creature type. You may look at the top card of
 *  your library any time. You may cast creature spells of the chosen type from the top of your
 *  library."
 *
 * The permission's filter is `Creature.withChosenSubtype()`, read against the Realmwalker granting
 * it. The enumerator and the cast handler judge that filter on separate paths, so each test that
 * offers a cast also submits it — the handler is the authoritative re-check.
 */
class RealmwalkerScenarioTest : FunSpec({

    val elf = CardDefinition.creature(
        name = "Test Elf",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1,
    )
    val bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
    )
    val shapeshifter = CardDefinition.creature(
        name = "Test Shapeshifter",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype("Shapeshifter")),
        power = 1,
        toughness = 1,
        keywords = setOf(Keyword.CHANGELING),
    )

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Realmwalker, MaskwoodNexus, elf, bear, shapeshifter))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to you
    }

    /** Cast Realmwalker from hand for real, so its entry choice runs, and name [type]. */
    fun castRealmwalkerChoosing(driver: GameTestDriver, you: EntityId, type: String) {
        val cardId = driver.putCardInHand(you, "Realmwalker")
        driver.giveColorlessMana(you, 2)
        driver.giveMana(you, Color.GREEN)
        driver.submitSuccess(CastSpell(you, cardId, paymentStrategy = PaymentStrategy.FromPool))
        driver.bothPass()
        val decision = driver.pendingDecision as ChooseOptionDecision
        val index = decision.options.indexOfFirst { it.equals(type, ignoreCase = true) }
        driver.submitDecision(you, OptionChosenResponse(decision.id, index))
    }

    fun offered(driver: GameTestDriver, you: EntityId, name: String): Boolean =
        driver.legalActions(you).any { it.actionType == "CastSpell" && it.description.contains(name) }

    fun castFromTop(driver: GameTestDriver, you: EntityId, cardId: EntityId) =
        driver.submit(CastSpell(you, cardId, paymentStrategy = PaymentStrategy.FromPool))

    test("a creature of the chosen type on top is offered and casts") {
        val (driver, you) = setup()
        castRealmwalkerChoosing(driver, you, "Elf")
        val top = driver.putCardOnTopOfLibrary(you, "Test Elf")
        driver.giveColorlessMana(you, 1)

        offered(driver, you, "Test Elf") shouldBe true
        castFromTop(driver, you, top).error shouldBe null
        driver.state.getLibrary(you).contains(top) shouldBe false
        driver.bothPass()
        driver.state.getBattlefield(you).contains(top) shouldBe true
    }

    test("a creature of another type on top is neither offered nor castable") {
        val (driver, you) = setup()
        castRealmwalkerChoosing(driver, you, "Elf")
        val top = driver.putCardOnTopOfLibrary(you, "Test Bear")
        driver.giveColorlessMana(you, 1)

        offered(driver, you, "Test Bear") shouldBe false
        castFromTop(driver, you, top).error shouldNotBe null
        driver.state.getLibrary(you).first() shouldBe top
    }

    test("a changeling creature is every creature type, so it matches any choice") {
        val (driver, you) = setup()
        castRealmwalkerChoosing(driver, you, "Elf")
        val top = driver.putCardOnTopOfLibrary(you, "Test Shapeshifter")
        driver.giveColorlessMana(you, 1)

        offered(driver, you, "Test Shapeshifter") shouldBe true
        castFromTop(driver, you, top).error shouldBe null
    }

    test("a cross-zone type grant counts — Maskwood Nexus makes a Bear in the library every type") {
        val (driver, you) = setup()
        castRealmwalkerChoosing(driver, you, "Elf")
        driver.putPermanentOnBattlefield(you, "Maskwood Nexus")
        val top = driver.putCardOnTopOfLibrary(you, "Test Bear")
        driver.giveColorlessMana(you, 1)

        withClue("the enumerator and the cast handler must agree on the granted types") {
            offered(driver, you, "Test Bear") shouldBe true
            castFromTop(driver, you, top).error shouldBe null
        }
    }

    test("with no type chosen nothing on top is castable") {
        val (driver, you) = setup()
        // Put onto the battlefield directly: the "as this enters" choice never ran.
        driver.putPermanentOnBattlefield(you, "Realmwalker")
        val top = driver.putCardOnTopOfLibrary(you, "Test Shapeshifter")
        driver.giveColorlessMana(you, 1)

        withClue("no choice matches nothing — not even a creature that is every type (ruling)") {
            offered(driver, you, "Test Shapeshifter") shouldBe false
            castFromTop(driver, you, top).error shouldNotBe null
        }
        driver.state.getLibrary(you).first() shouldBe top
    }
})
