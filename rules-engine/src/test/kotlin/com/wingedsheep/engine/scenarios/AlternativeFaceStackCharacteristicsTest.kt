package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * An Adventure (CR 715.3b), an Omen (CR 720.3b) or a modal double-faced card's spell back
 * (CR 712.8f) is on the stack with only that face's characteristics, and the card has its normal
 * characteristics again in every other zone (CR 715.4, 712.8a). The stack object therefore reports the face's mana value, type line and colors to
 * every reader — the SpellCastEvent's mana value, cast-trigger filters, "spell with mana value N"
 * checks — and gets the creature back when it resolves, is countered, or is shuffled away.
 */
class AlternativeFaceStackCharacteristicsTest : FunSpec({

    // {3}{G}{G} green Giant whose Adventure is a {1}{U} blue instant.
    val storyGiant = card("Story Giant") {
        manaCost = "{3}{G}{G}"
        typeLine = "Creature — Giant"
        power = 5
        toughness = 5
        adventure("Tall Tale") {
            manaCost = "{1}{U}"
            typeLine = "Instant — Adventure"
            oracleText = "Draw a card."
            spell { effect = Effects.DrawCards(1) }
        }
    }

    // {4}{G} Dragon whose Omen is a {U} instant.
    val omenDragon = card("Omen Dragon") {
        manaCost = "{4}{G}"
        typeLine = "Creature — Dragon"
        power = 4
        toughness = 4
        omen("Quiet Portent") {
            manaCost = "{U}"
            typeLine = "Instant — Omen"
            oracleText = "Draw a card."
            spell { effect = Effects.DrawCards(1) }
        }
    }

    // {2}{G} modal double-faced creature whose back is a {U} instant.
    val modalTroll = card("Modal Troll") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Troll"
        power = 3
        toughness = 3
        modalBack("Troll Riddle") {
            manaCost = "{U}"
            typeLine = "Instant"
            oracleText = "Draw a card."
            spell { effect = Effects.DrawCards(1) }
        }
    }

    // Reads the cast spell's mana value; the filters tell an instant from a creature spell.
    val instantWatcher = card("Instant Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Instant)
            effect = Effects.GainLife(DynamicAmounts.triggeringSpellManaValue())
        }
    }
    val creatureWatcher = card("Creature Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Creature)
            effect = Effects.GainLife(10)
        }
    }
    val blueWatcher = card("Blue Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.BLUE))
            effect = Effects.GainLife(100)
        }
    }
    val counterspell = card("Test Counterspell") {
        manaCost = "{U}{U}"
        typeLine = "Instant"
        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.CounterSpell()
        }
    }

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(storyGiant, omenDragon, modalTroll, instantWatcher, creatureWatcher, blueWatcher, counterspell))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40, "Forest" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    fun GameTestDriver.castFace(player: EntityId, card: EntityId) = submitSuccess(
        CastSpell(player, card, faceIndex = 0, paymentStrategy = PaymentStrategy.FromPool)
    )

    fun GameTestDriver.card(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!

    test("an Adventure on the stack has the Adventure's mana value, type line and colors") {
        val (driver, player) = setUp()
        val giant = driver.putCardInHand(player, "Story Giant")
        driver.giveMana(player, Color.BLUE, 2)

        driver.castFace(player, giant)

        driver.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe 2
        val onStack = driver.card(giant)
        onStack.manaValue shouldBe 2
        onStack.typeLine.isInstant shouldBe true
        onStack.typeLine.isCreature shouldBe false
        onStack.colors shouldBe setOf(Color.BLUE)
    }

    test("cast triggers see an instant of the Adventure's mana value and color, not a creature") {
        val (driver, player) = setUp()
        driver.putCreatureOnBattlefield(player, "Instant Watcher")
        driver.putCreatureOnBattlefield(player, "Creature Watcher")
        driver.putCreatureOnBattlefield(player, "Blue Watcher")
        val giant = driver.putCardInHand(player, "Story Giant")
        driver.giveMana(player, Color.BLUE, 2)

        driver.castFace(player, giant)
        repeat(3) { if (driver.stackSize > 0) driver.bothPass() }

        // Instant Watcher +2 (the Adventure's mana value), Blue Watcher +100, Creature Watcher silent.
        driver.getLifeTotal(player) shouldBe 20 + 2 + 100
    }

    test("the creature half cast normally keeps the card's own characteristics") {
        val (driver, player) = setUp()
        driver.putCreatureOnBattlefield(player, "Instant Watcher")
        driver.putCreatureOnBattlefield(player, "Creature Watcher")
        val giant = driver.putCardInHand(player, "Story Giant")
        driver.giveMana(player, Color.GREEN, 5)

        driver.submitSuccess(CastSpell(player, giant, paymentStrategy = PaymentStrategy.FromPool))

        driver.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe 5
        driver.card(giant).typeLine.isCreature shouldBe true
        repeat(2) { if (driver.stackSize > 0) driver.bothPass() }
        driver.getLifeTotal(player) shouldBe 30
    }

    test("a resolved Adventure is a creature card again in exile (CR 715.4)") {
        val (driver, player) = setUp()
        val giant = driver.putCardInHand(player, "Story Giant")
        driver.giveMana(player, Color.BLUE, 2)

        driver.castFace(player, giant)
        driver.bothPass()

        driver.getExile(player) shouldContain giant
        val exiled = driver.card(giant)
        exiled.manaValue shouldBe 5
        exiled.typeLine.isCreature shouldBe true
        exiled.colors shouldBe setOf(Color.GREEN)
    }

    test("a countered Adventure goes to the graveyard as the creature card") {
        val (driver, player) = setUp()
        val opponent = driver.getOpponent(player)
        val giant = driver.putCardInHand(player, "Story Giant")
        val counter = driver.putCardInHand(opponent, "Test Counterspell")
        driver.giveMana(player, Color.BLUE, 2)
        driver.giveMana(opponent, Color.BLUE, 2)

        driver.castFace(player, giant)
        driver.passPriority(player)
        driver.submitSuccess(
            CastSpell(opponent, counter, targets = listOf(ChosenTarget.Spell(giant)), paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        driver.getGraveyard(player) shouldContain giant
        driver.card(giant).typeLine.isCreature shouldBe true
        driver.card(giant).manaValue shouldBe 5
    }

    test("an Omen on the stack has the Omen's characteristics, and the shuffled card is the creature") {
        val (driver, player) = setUp()
        driver.putCreatureOnBattlefield(player, "Instant Watcher")
        val dragon = driver.putCardInHand(player, "Omen Dragon")
        driver.giveMana(player, Color.BLUE, 1)

        driver.castFace(player, dragon)

        driver.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe 1
        driver.card(dragon).typeLine.isInstant shouldBe true
        driver.card(dragon).typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype("Dragon")) shouldBe false

        repeat(2) { if (driver.stackSize > 0) driver.bothPass() }
        driver.getLifeTotal(player) shouldBe 21
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)) shouldContain dragon
        driver.card(dragon).typeLine.isCreature shouldBe true
        driver.card(dragon).manaValue shouldBe 5
    }

    test("a modal double-faced card cast as its spell back has the back's characteristics (CR 712.8f)") {
        val (driver, player) = setUp()
        driver.putCreatureOnBattlefield(player, "Instant Watcher")
        val troll = driver.putCardInHand(player, "Modal Troll")
        driver.giveMana(player, Color.BLUE, 1)

        driver.castFace(player, troll)

        driver.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe 1
        driver.card(troll).typeLine.isInstant shouldBe true
        driver.card(troll).colors shouldBe setOf(Color.BLUE)

        repeat(2) { if (driver.stackSize > 0) driver.bothPass() }
        driver.getLifeTotal(player) shouldBe 21
        driver.getGraveyard(player) shouldContain troll
        driver.card(troll).typeLine.isCreature shouldBe true
        driver.card(troll).manaValue shouldBe 3
    }
})
