package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantForetellToCardsInHand
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayForetellOnAnyTurn
import com.wingedsheep.sdk.scripting.ModifySpecialActionCost
import com.wingedsheep.sdk.scripting.SpecialActionKind
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Foretell modifiers (CR 702.143, CR 116.2h):
 *  - `GrantForetellToCardsInHand` — grants foretell to matching cards in hand; the cost is the card's
 *    mana cost reduced in its generic part only, fixed as the card is foretold, and it outlives the
 *    grant (Dream Devourer rulings). A printed foretell keeps its own cost.
 *  - `EventPattern.ForetoldEvent` — "whenever you foretell a card": the special action, not the
 *    later cast.
 *  - `ModifySpecialActionCost(FORETELL)` — the {2} setup cost, stacking, floored at {0}.
 *  - `MayForetellOnAnyTurn` — foretell any time you have priority; without it, any time you have
 *    priority during your own turn (not only at sorcery speed).
 */
class ForetellModifiersTest : FunSpec({

    val granter = card("Test Foretell Granter") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Demon"
        power = 0
        toughness = 3
        oracleText = "Each nonland card in your hand without foretell has foretell. Its foretell cost is equal to its mana cost reduced by {2}.\n" +
            "Whenever you foretell a card, this creature gets +2/+0 until end of turn."
        staticAbility { ability = GrantForetellToCardsInHand(GameObjectFilter.Nonland, costReduction = 2) }
        triggeredAbility {
            trigger = Triggers.you.foretells()
            effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        }
    }

    val discounter = card("Test Foretell Discounter") {
        manaCost = "{3}{U}"
        typeLine = "Creature — Spirit"
        power = 3
        toughness = 3
        oracleText = "Foretelling cards from your hand costs {1} less and can be done on any player's turn."
        staticAbility { ability = ModifySpecialActionCost(SpecialActionKind.FORETELL, CostModification.ReduceGeneric(1)) }
        staticAbility { ability = MayForetellOnAnyTurn }
    }

    val printedForetell = card("Test Printed Foretell") {
        manaCost = "{2}{W}"
        typeLine = "Instant"
        oracleText = "You gain 1 life.\nForetell {W}"
        spell { effect = Effects.GainLife(1) }
        keywordAbility(KeywordAbility.foretell("{W}"))
    }

    val neutralize = card("Test Neutralize") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Target creature loses all abilities until end of turn."
        spell {
            val creature = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(creature)
        }
    }

    val shock = card("Test Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "You gain 1 life."
        spell { effect = Effects.GainLife(1) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(granter, discounter, printedForetell, neutralize, shock))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.foretellAction(p: EntityId, card: EntityId) =
        legalActions(p).singleOrNull { it.action == ForetellCard(p, card) }

    fun GameTestDriver.stampedCost(card: EntityId): ManaCost? =
        state.getEntity(card)?.get<PlayWithFixedAlternativeManaCostComponent>()?.fixedCost

    context("GrantForetellToCardsInHand") {
        test("a nonland card without foretell can be foretold only while the granter is out") {
            val d = driver()
            val p = d.activePlayer!!
            val giant = d.putCardInHand(p, "Hill Giant")
            d.foretellAction(p, giant) shouldBe null
            d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            d.foretellAction(p, giant) shouldNotBe null
        }

        test("a land card in hand doesn't gain foretell") {
            val d = driver()
            val p = d.activePlayer!!
            d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            val forest = d.putCardInHand(p, "Forest")
            d.foretellAction(p, forest) shouldBe null
        }

        test("a card with no mana cost doesn't gain foretell (its foretell cost would be unpayable)") {
            val d = driver()
            val p = d.activePlayer!!
            d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            val vision = d.putCardInHand(p, "Ancestral Vision")
            d.foretellAction(p, vision) shouldBe null
            d.submitExpectFailure(ForetellCard(p, vision))
        }

        test("a granter that has lost all its abilities grants nothing") {
            val d = driver()
            val p = d.activePlayer!!
            val granterId = d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            val giant = d.putCardInHand(p, "Hill Giant")
            val neutralizeId = d.putCardInHand(p, "Test Neutralize")
            d.giveMana(p, Color.BLUE, 1)
            d.castSpell(p, neutralizeId, listOf(granterId)).error shouldBe null
            d.bothPass()
            d.foretellAction(p, giant) shouldBe null
        }

        test("the granted cost is the mana cost reduced in its generic part only") {
            val d = driver()
            val p = d.activePlayer!!
            d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            val giant = d.putCardInHand(p, "Hill Giant") // {3}{R}
            val elves = d.putCardInHand(p, "Llanowar Elves") // {G}
            d.giveColorlessMana(p, 4)
            d.submitSuccess(ForetellCard(p, giant))
            d.submitSuccess(ForetellCard(p, elves))
            d.stampedCost(giant) shouldBe ManaCost.parse("{1}{R}")
            d.stampedCost(elves) shouldBe ManaCost.parse("{G}")
        }

        test("a card with printed foretell keeps its own foretell cost") {
            val d = driver()
            val p = d.activePlayer!!
            d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            d.giveColorlessMana(p, 2)
            d.submitSuccess(ForetellCard(p, instant))
            d.stampedCost(instant) shouldBe ManaCost.parse("{W}")
        }

        test("the foretold card is cast later for the granted cost even after the granter is gone") {
            val d = driver()
            val p = d.activePlayer!!
            val granterId = d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            val bears = d.putCardInHand(p, "Grizzly Bears") // {1}{G} -> foretell {G}
            d.giveColorlessMana(p, 2)
            d.submitSuccess(ForetellCard(p, bears))
            d.bothPass() // resolve the foretell trigger
            d.moveToGraveyard(granterId)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(p))
            d.passPriorityUntil(Step.PRECOMBAT_MAIN, p)
            d.giveMana(p, Color.GREEN, 1)
            d.castSpell(p, bears).error shouldBe null
            d.bothPass()
            d.getCreatures(p).contains(bears) shouldBe true
        }
    }

    context("whenever you foretell a card") {
        test("foretelling triggers it; casting the foretold card later doesn't") {
            val d = driver()
            val p = d.activePlayer!!
            val granterId = d.putCreatureOnBattlefield(p, "Test Foretell Granter")
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            d.giveColorlessMana(p, 2)
            d.submitSuccess(ForetellCard(p, instant))
            d.state.stack.size shouldBe 1
            d.bothPass()
            d.state.projectedState.getPower(granterId) shouldBe 2

            // An instant: cast it from exile on the opponent's turn — no new trigger.
            d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(p))
            d.state.projectedState.getPower(granterId) shouldBe 0
            d.passPriority(d.getOpponent(p))
            d.giveMana(p, Color.WHITE, 1)
            d.castSpell(p, instant).error shouldBe null
            d.state.stack.size shouldBe 1
        }

        test("an opponent foretelling doesn't trigger it") {
            val d = driver()
            val p = d.activePlayer!!
            val opp = d.getOpponent(p)
            d.putCreatureOnBattlefield(opp, "Test Foretell Granter")
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            d.giveColorlessMana(p, 2)
            d.submitSuccess(ForetellCard(p, instant))
            d.state.stack.size shouldBe 0
        }
    }

    context("foretell timing and cost") {
        test("without a modifier, foretell is offered any time you have priority on your turn, never on an opponent's") {
            val d = driver()
            val p = d.activePlayer!!
            val opp = d.getOpponent(p)
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            d.passPriorityUntil(Step.BEGIN_COMBAT)
            d.giveColorlessMana(p, 2)
            d.foretellAction(p, instant)?.affordable shouldBe true

            d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
            d.passPriority(opp)
            d.state.priorityPlayerId shouldBe p
            d.foretellAction(p, instant) shouldBe null
            d.submitExpectFailure(ForetellCard(p, instant))
        }

        test("on your turn you may foretell with a spell on the stack, in response") {
            val d = driver()
            val p = d.activePlayer!!
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            val ping = d.putCardInHand(p, "Test Ping")
            d.giveMana(p, Color.RED, 1)
            d.castSpell(p, ping).error shouldBe null
            d.state.stack.size shouldBe 1
            d.state.priorityPlayerId shouldBe p
            d.giveColorlessMana(p, 2)
            d.foretellAction(p, instant)?.affordable shouldBe true
            d.submitSuccess(ForetellCard(p, instant))
            d.getExile(p).contains(instant) shouldBe true
            d.state.stack.size shouldBe 1
        }

        test("MayForetellOnAnyTurn lets you foretell on an opponent's turn") {
            val d = driver()
            val p = d.activePlayer!!
            val opp = d.getOpponent(p)
            d.putCreatureOnBattlefield(p, "Test Foretell Discounter")
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
            d.passPriority(opp)
            d.giveColorlessMana(p, 1)
            d.submitSuccess(ForetellCard(p, instant))
            d.getExile(p).contains(instant) shouldBe true
        }

        test("ModifySpecialActionCost reduces the {2}, stacks, and floors at {0}") {
            val d = driver()
            val p = d.activePlayer!!
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            d.foretellAction(p, instant)!!.manaCostString shouldBe "{2}"
            d.putCreatureOnBattlefield(p, "Test Foretell Discounter")
            d.foretellAction(p, instant)!!.manaCostString shouldBe "{1}"
            d.putCreatureOnBattlefield(p, "Test Foretell Discounter")
            d.putCreatureOnBattlefield(p, "Test Foretell Discounter")
            val free = d.foretellAction(p, instant)!!
            free.manaCostString shouldBe "{0}"
            free.affordable shouldBe true
            d.submitSuccess(ForetellCard(p, instant))
            d.getExile(p).contains(instant) shouldBe true
        }

        test("an opponent's ModifySpecialActionCost doesn't reduce your foretell") {
            val d = driver()
            val p = d.activePlayer!!
            d.putCreatureOnBattlefield(d.getOpponent(p), "Test Foretell Discounter")
            val instant = d.putCardInHand(p, "Test Printed Foretell")
            d.foretellAction(p, instant)!!.manaCostString shouldBe "{2}"
        }
    }
})
