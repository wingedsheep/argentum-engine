package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.SetColors
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [SetColors] — the layer-5 color-*setting* static (CR 613.1e) and its off-battlefield reach,
 * Mycosynth Lattice's "All cards that aren't on the battlefield, spells, and permanents are
 * colorless" (ruling: "makes everything, in every zone of the game, colorless").
 *
 * Pins: permanents of both players lose their colors; cards in every other zone and spells on the
 * stack of both players are colorless; `Player.You` reaches only its controller's; the effect ends
 * with its source; and the readers that see an object off the battlefield — color predicates, cast
 * triggers, cast history, and protection against a spell — all read the overlay.
 */
class SetColorsOffBattlefieldTest : FunSpec({

    val everythingColorless = card("Test Everything Colorless") {
        manaCost = "{6}"
        typeLine = "Artifact"
        staticAbility {
            ability = SetColors(emptySet(), GroupFilter(GameObjectFilter.Any), offBattlefield = Player.Each)
        }
    }

    val mineColorless = card("Test Mine Colorless") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility {
            ability = SetColors(emptySet(), GroupFilter(GameObjectFilter.Any.youControl()), offBattlefield = Player.You)
        }
    }

    val battlefieldBlue = card("Test Battlefield Blue") {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility {
            ability = SetColors(setOf(Color.BLUE), GroupFilter(GameObjectFilter.Creature))
        }
    }

    val redWatcher = card("Test Red Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.anyPlayer.casts(GameObjectFilter.Any.withColor(Color.RED))
            effect = Effects.GainLife(1)
        }
    }

    val proRedBear = card("Test Pro-Red Bear") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.RED)))
    }

    val redMedallion = card("Test Red Medallion") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.RED)),
                modification = CostModification.ReduceGeneric(1),
            )
        }
    }

    val redCantrip = card("Test Red Cantrip") {
        manaCost = "{1}{R}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    val predicates = PredicateEvaluator(cardRegistry = null)

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(everythingColorless, mineColorless, battlefieldBlue, redWatcher, proRedBear, redMedallion, redCantrip))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isColorless(id: EntityId): Boolean =
        predicates.matches(state, state.projectedState, id, GameObjectFilter(cardPredicates = listOf(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsColorless)), PredicateContext(controllerId = player1))

    fun GameTestDriver.isRed(id: EntityId): Boolean =
        predicates.matches(state, state.projectedState, id, GameObjectFilter.Any.withColor(Color.RED), PredicateContext(controllerId = player1))

    test("every permanent, of either player, is colorless") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Everything Colorless")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")

        driver.state.projectedState.getColors(mine) shouldBe emptySet()
        driver.state.projectedState.getColors(theirs) shouldBe emptySet()
        driver.isColorless(theirs) shouldBe true
    }

    test("every card outside the battlefield, of either player and in every zone, is colorless") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Everything Colorless")
        val cards = listOf(
            driver.putCardInHand(driver.player1, "Lightning Bolt"),
            driver.putCardInGraveyard(driver.player1, "Lightning Bolt"),
            driver.putCardOnTopOfLibrary(driver.player1, "Lightning Bolt"),
            driver.putCardInExile(driver.player1, "Lightning Bolt"),
            driver.putCardInHand(driver.player2, "Lightning Bolt"),
            driver.putCardInGraveyard(driver.player2, "Centaur Courser"),
        )
        for (id in cards) {
            driver.isColorless(id) shouldBe true
            driver.isRed(id) shouldBe false
            driver.state.projectedState.colorsOf(id) shouldBe emptySet()
        }
    }

    test("a spell on the stack is colorless, so a red-spell cast trigger doesn't fire") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Everything Colorless")
        driver.putPermanentOnBattlefield(driver.player1, "Test Red Watcher")
        val life = driver.getLifeTotal(driver.player1)

        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpell(driver.player1, bolt, listOf(driver.player2)).error shouldBe null
        driver.isColorless(bolt) shouldBe true
        driver.state.spellsCastThisTurnByPlayer[driver.player1]!!.single().colors shouldBe emptySet()
        driver.bothPass()

        driver.getLifeTotal(driver.player1) shouldBe life
    }

    test("without the static a red spell is red and the watcher fires") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Red Watcher")
        val life = driver.getLifeTotal(driver.player1)
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.isRed(bolt) shouldBe true
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpell(driver.player1, bolt, listOf(driver.player2))
        driver.bothPass()
        driver.getLifeTotal(driver.player1) shouldBe life + 1
    }

    test("a colorless Lightning Bolt can target and damage a creature with protection from red") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Everything Colorless")
        val bear = driver.putCreatureOnBattlefield(driver.player2, "Test Pro-Red Bear")
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)

        driver.castSpell(driver.player1, bolt, listOf(bear)).error shouldBe null
        driver.bothPass()

        driver.state.getBattlefield().contains(bear) shouldBe false
    }

    test("protection from red still stops a red Lightning Bolt without the static") {
        val driver = newDriver()
        val bear = driver.putCreatureOnBattlefield(driver.player2, "Test Pro-Red Bear")
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpell(driver.player1, bolt, listOf(bear)).error shouldNotBe null
    }

    test("Player.You reaches only its controller's cards, spells and permanents") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Mine Colorless")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val myCard = driver.putCardInHand(driver.player1, "Lightning Bolt")
        val theirCard = driver.putCardInHand(driver.player2, "Lightning Bolt")

        driver.isColorless(mine) shouldBe true
        driver.isColorless(theirs) shouldBe false
        driver.isColorless(myCard) shouldBe true
        driver.isRed(theirCard) shouldBe true
    }

    test("a battlefield-only SetColors replaces colors there and leaves cards elsewhere alone") {
        val driver = newDriver()
        driver.putPermanentOnBattlefield(driver.player1, "Test Battlefield Blue")
        val courser = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        val inHand = driver.putCardInHand(driver.player1, "Centaur Courser")

        driver.state.projectedState.getColors(courser) shouldBe setOf(Color.BLUE.name)
        driver.state.projectedState.colorsOf(inHand) shouldBe setOf(Color.GREEN.name)
    }

    test("the colors come back when the source leaves the battlefield") {
        val driver = newDriver()
        val source = driver.putPermanentOnBattlefield(driver.player1, "Test Everything Colorless")
        val inHand = driver.putCardInHand(driver.player2, "Lightning Bolt")
        val courser = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.isColorless(inHand) shouldBe true

        driver.moveToGraveyard(source)
        driver.isRed(inHand) shouldBe true
        driver.state.projectedState.getColors(courser) shouldBe setOf(Color.GREEN.name)
    }

    test("a red-spell cost reduction doesn't apply to a spell the static made colorless") {
        val with = newDriver()
        with.putPermanentOnBattlefield(with.player1, "Test Red Medallion")
        with.putPermanentOnBattlefield(with.player1, "Test Everything Colorless")
        with.putLandOnBattlefield(with.player1, "Mountain")
        val colorless = with.putCardInHand(with.player1, "Test Red Cantrip")
        with.castSpell(with.player1, colorless).error shouldNotBe null

        val without = newDriver()
        without.putPermanentOnBattlefield(without.player1, "Test Red Medallion")
        without.putLandOnBattlefield(without.player1, "Mountain")
        val red = without.putCardInHand(without.player1, "Test Red Cantrip")
        without.castSpell(without.player1, red).error shouldBe null
    }
})
