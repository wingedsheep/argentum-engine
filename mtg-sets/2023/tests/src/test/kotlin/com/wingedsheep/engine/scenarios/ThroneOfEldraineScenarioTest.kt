package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woc.cards.ThroneOfEldraine
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Throne of Eldraine — {5} Legendary Artifact.
 *   "As Throne of Eldraine enters, choose a color.
 *    {T}: Add four mana of the chosen color. Spend this mana only to cast monocolored spells of that color.
 *    {3}, {T}: Draw two cards. Spend only mana of the chosen color to activate this ability."
 *
 * Covers the two new axes end to end: the produced mana's [ManaRestriction.SpellsOfChosenColor]
 * (bound to the choice as the mana is made, pays only for monocolored spells of that color) and the
 * draw ability's `payOnlyWithManaOf` (its {3} can be paid only with mana of the chosen color).
 */
class ThroneOfEldraineScenarioTest : FunSpec({

    val manaAbilityId = ThroneOfEldraine.activatedAbilities[0].id
    val drawAbilityId = ThroneOfEldraine.activatedAbilities[1].id

    val monoBlue = CardDefinition.creature(
        name = "Mono Blue Test Bear", manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2,
    )
    val blueRed = CardDefinition.creature(
        name = "Blue Red Test Bear", manaCost = ManaCost.parse("{U}{R}"),
        subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2,
    )
    val colorlessArtifactCreature = TestCards.ArtifactCreature

    fun driver(chosen: Color = Color.BLUE): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(ThroneOfEldraine, monoBlue, blueRed))
            initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        }
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val throne = d.putCardInHand(you, "Throne of Eldraine")
        d.giveColorlessMana(you, 5)
        d.castSpell(you, throne)
        d.bothPass() // resolve it
        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        d.submitDecision(you, ColorChosenResponse(decision.id, chosen))
        return d to you
    }

    fun GameTestDriver.throne(you: EntityId) = findPermanent(you, "Throne of Eldraine")!!

    fun GameTestDriver.pool(you: EntityId) = state.getEntity(you)?.get<ManaPoolComponent>()!!

    fun GameTestDriver.tapForMana(you: EntityId) =
        submitSuccess(ActivateAbility(playerId = you, sourceId = throne(you), abilityId = manaAbilityId))

    fun GameTestDriver.castFromPool(you: EntityId, name: String) =
        submit(CastSpell(playerId = you, cardId = putCardInHand(you, name), paymentStrategy = PaymentStrategy.FromPool))

    fun GameTestDriver.activateDraw(you: EntityId, strategy: PaymentStrategy = PaymentStrategy.AutoPay) =
        submit(ActivateAbility(playerId = you, sourceId = throne(you), abilityId = drawAbilityId, paymentStrategy = strategy))

    context("the mana ability") {
        test("adds four mana of the chosen color, bound to monocolored spells of that color") {
            val (d, you) = driver(Color.BLUE)
            d.tapForMana(you)

            val pool = d.pool(you)
            pool.getAmount(Color.BLUE) shouldBe 0
            pool.restrictedMana.size shouldBe 4
            pool.restrictedMana.all { it.color == Color.BLUE } shouldBe true
            // The pool holds the self-contained bound form, never the unbound placeholder.
            pool.restrictedMana.all { it.restriction == ManaRestriction.SpellsWithExactColorCount(1, Color.BLUE) } shouldBe true
        }

        test("binds to whichever color was chosen") {
            val (d, you) = driver(Color.RED)
            d.tapForMana(you)
            d.pool(you).restrictedMana.all {
                it.color == Color.RED && it.restriction == ManaRestriction.SpellsWithExactColorCount(1, Color.RED)
            } shouldBe true
        }

        test("pays for a monocolored spell of the chosen color") {
            val (d, you) = driver(Color.BLUE)
            d.tapForMana(you)
            d.castFromPool(you, "Mono Blue Test Bear").error shouldBe null
            d.pool(you).restrictedMana.size shouldBe 2
        }

        test("cannot pay for a monocolored spell of another color") {
            val (d, you) = driver(Color.BLUE)
            d.tapForMana(you)
            d.castFromPool(you, "Goblin Guide").error?.isNotEmpty() shouldBe true
            d.pool(you).restrictedMana.size shouldBe 4
        }

        test("cannot pay for a multicolored spell that includes the chosen color") {
            val (d, you) = driver(Color.BLUE)
            d.tapForMana(you)
            d.castFromPool(you, "Blue Red Test Bear").error?.isNotEmpty() shouldBe true
            d.pool(you).restrictedMana.size shouldBe 4
        }

        test("cannot pay for a colorless spell") {
            val (d, you) = driver(Color.BLUE)
            d.tapForMana(you)
            d.castFromPool(you, colorlessArtifactCreature.name).error?.isNotEmpty() shouldBe true
        }

        test("auto-pay taps the Throne for a monocolored spell of its color, but not for another color's") {
            val (d, you) = driver(Color.BLUE)
            val bear = d.putCardInHand(you, "Mono Blue Test Bear")
            val guide = d.putCardInHand(you, "Goblin Guide")
            // Offered only when payable: the Throne's blue mana can't cast a red spell.
            d.legalActions(you).filter { (it.action as? CastSpell)?.cardId == guide }.none { it.affordable } shouldBe true
            d.legalActions(you).any { (it.action as? CastSpell)?.cardId == bear && it.affordable } shouldBe true

            d.submit(CastSpell(playerId = you, cardId = bear, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null
            d.isTapped(d.throne(you)) shouldBe true
            // Four mana made, a {1}{U} spell took two: two units of blue spell-only mana remain.
            d.pool(you).restrictedMana.size shouldBe 2
        }

        test("cannot pay for the draw ability — it is spell-only mana") {
            val (d, you) = driver(Color.BLUE)
            d.tapForMana(you)
            // Throne itself is tapped for its mana, so a second Throne would be needed for the cost;
            // the floating mana alone must not be able to pay {3} for an ability activation.
            d.untapPermanent(d.throne(you))
            d.activateDraw(you, PaymentStrategy.FromPool).error?.isNotEmpty() shouldBe true
        }
    }

    context("the draw ability") {
        test("is paid with three mana of the chosen color and draws two cards") {
            val (d, you) = driver(Color.BLUE)
            val islands = List(3) { d.putLandOnBattlefield(you, "Island") }
            val handBefore = d.getHand(you).size

            d.activateDraw(you).error shouldBe null

            d.isTapped(d.throne(you)) shouldBe true
            islands.all { d.isTapped(it) } shouldBe true
            // Resolve the ability from the stack.
            d.bothPass()
            d.getHand(you).size shouldBe handBefore + 2
        }

        test("cannot be paid with mana of other colors") {
            val (d, you) = driver(Color.BLUE)
            repeat(3) { d.putLandOnBattlefield(you, "Mountain") }
            d.activateDraw(you).error?.isNotEmpty() shouldBe true
            // The menu agrees with the handler: offered, but marked unaffordable.
            val offered = d.legalActions(you).filter { (it.action as? ActivateAbility)?.abilityId == drawAbilityId }
            offered.all { !it.affordable } shouldBe true
        }

        test("is offered as affordable with three lands of the chosen color") {
            val (d, you) = driver(Color.BLUE)
            repeat(3) { d.putLandOnBattlefield(you, "Island") }
            val offered = d.legalActions(you).filter { (it.action as? ActivateAbility)?.abilityId == drawAbilityId }
            offered.size shouldBe 1
            offered.single().affordable shouldBe true
            // The menu shows the price the player will actually pay.
            offered.single().description.contains("{U}{U}{U}") shouldBe true
        }

        test("cannot be paid with two of the chosen color and one of another") {
            val (d, you) = driver(Color.BLUE)
            repeat(2) { d.putLandOnBattlefield(you, "Island") }
            d.putLandOnBattlefield(you, "Mountain")
            d.activateDraw(you).error?.isNotEmpty() shouldBe true
        }

        test("cannot be paid with colorless mana") {
            val (d, you) = driver(Color.BLUE)
            d.giveColorlessMana(you, 3)
            d.activateDraw(you, PaymentStrategy.FromPool).error?.isNotEmpty() shouldBe true
        }

        test("floating mana of the chosen color pays; floating mana of another color does not") {
            val (d, you) = driver(Color.BLUE)
            d.giveMana(you, Color.RED, 3)
            d.activateDraw(you, PaymentStrategy.FromPool).error?.isNotEmpty() shouldBe true
            d.giveMana(you, Color.BLUE, 3)
            d.activateDraw(you, PaymentStrategy.FromPool).error shouldBe null
            // The red mana was left alone.
            d.pool(you).getAmount(Color.RED) shouldBe 3
        }

        test("follows the chosen color: with red chosen, Mountains pay and Islands do not") {
            val (d, you) = driver(Color.RED)
            repeat(3) { d.putLandOnBattlefield(you, "Island") }
            d.activateDraw(you).error?.isNotEmpty() shouldBe true
            repeat(3) { d.putLandOnBattlefield(you, "Mountain") }
            d.activateDraw(you).error shouldBe null
        }
    }
})
