package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.MorophonTheBoundless
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Morophon, the Boundless {7} — "Spells of the chosen type you cast cost {W}{U}{B}{R}{G} less to
 * cast. This effect reduces only the amount of colored mana you pay. Other creatures you control
 * of the chosen type get +1/+1."
 *
 * Pins the reduction against plain, hybrid ({W/U}: the payer picks the reduced half, per the
 * Scryfall ruling; CR 601.2b), monocolor-hybrid and Phyrexian pips (CR 107.4e-f), that it never touches
 * generic mana, that it is the caster's own spells only, and the lord.
 */
class MorophonTheBoundlessScenarioTest : FunSpec({

    val hybridElf = card("Hybrid Elf") {
        manaCost = "{2}{W/U}{W/U}"
        typeLine = "Creature — Elf"
        power = 2
        toughness = 2
    }
    val phyrexianElf = card("Phyrexian Elf") {
        manaCost = "{1}{G/P}{G/P}"
        typeLine = "Creature — Elf"
        power = 2
        toughness = 2
    }
    val monoHybridElf = card("Monohybrid Elf") {
        manaCost = "{2/R}{2/R}"
        typeLine = "Creature — Elf"
        power = 2
        toughness = 2
    }
    val bigElf = card("Big Elf") {
        manaCost = "{4}{R}{W}{W}"
        typeLine = "Creature — Elf"
        power = 5
        toughness = 5
    }
    val hybridGoblin = card("Hybrid Goblin") {
        manaCost = "{2}{W/U}{W/U}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
    }
    val extras = listOf(MorophonTheBoundless, hybridElf, phyrexianElf, monoHybridElf, bigElf, hybridGoblin)

    fun registry(): CardRegistry = CardRegistry().apply {
        register(TestCards.all)
        extras.forEach { register(it) }
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + extras)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castMorophonChoosing(player: EntityId, creatureType: String): EntityId {
        giveColorlessMana(player, 7)
        val morophon = putCardInHand(player, "Morophon, the Boundless")
        castSpell(player, morophon).error shouldBe null
        bothPass()
        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(creatureType)
        (index >= 0) shouldBe true
        submitDecision(player, OptionChosenResponse(decision.id, index))
        return findPermanent(player, "Morophon, the Boundless").shouldNotBeNull()
    }

    fun GameTestDriver.costFor(caster: EntityId, cardName: String): String =
        registry().let { reg ->
            CostCalculator(reg, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
                .calculateEffectiveCost(state, reg.requireCard(cardName), caster).toString()
        }

    test("reduces one mana of each color, including hybrid and Phyrexian pips, never generic") {
        val d = createDriver()
        val you = d.player1
        d.castMorophonChoosing(you, "Elf")

        withClue("{4}{R}{W}{W} -> {4}{W} (ruling)") {
            d.costFor(you, "Big Elf") shouldBe ManaCost.parse("{4}{W}").toString()
        }
        withClue("{2}{W/U}{W/U} -> {2}: one hybrid pip paid as W, the other as U (ruling)") {
            d.costFor(you, "Hybrid Elf") shouldBe ManaCost.parse("{2}").toString()
        }
        withClue("{1}{G/P}{G/P} -> {1}{G/P}: a Phyrexian pip is reduced by its color, once per color") {
            d.costFor(you, "Phyrexian Elf") shouldBe ManaCost.parse("{1}{G/P}").toString()
        }
        withClue("{2/R}{2/R} -> {2/R}: the red half of one monocolor-hybrid pip is reduced") {
            d.costFor(you, "Monohybrid Elf") shouldBe ManaCost.parse("{2/R}").toString()
        }
        withClue("a non-Elf keeps its full cost") {
            d.costFor(you, "Hybrid Goblin") shouldBe ManaCost.parse("{2}{W/U}{W/U}").toString()
        }
        withClue("only spells you cast — the opponent's Elves are untouched") {
            d.costFor(d.getOpponent(you), "Hybrid Elf") shouldBe ManaCost.parse("{2}{W/U}{W/U}").toString()
        }
    }

    test("an Elf with hybrid pips can actually be cast for its generic remainder") {
        val d = createDriver()
        val you = d.player1
        d.castMorophonChoosing(you, "Elf")

        d.giveColorlessMana(you, 2)
        val elf = d.putCardInHand(you, "Hybrid Elf")
        d.castSpell(you, elf).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Hybrid Elf").shouldNotBeNull()
    }

    test("other creatures you control of the chosen type get +1/+1; Morophon itself does not") {
        val d = createDriver()
        val you = d.player1
        val morophon = d.castMorophonChoosing(you, "Elf")
        val elves = d.putCreatureOnBattlefield(you, "Llanowar Elves")
        val goblin = d.putCreatureOnBattlefield(you, "Goblin Guide")
        val opposingElves = d.putCreatureOnBattlefield(d.getOpponent(you), "Llanowar Elves")

        val projected = d.state.projectedState
        projected.getPower(morophon) shouldBe 6
        projected.getPower(elves) shouldBe 2
        projected.getToughness(elves) shouldBe 2
        projected.getPower(goblin) shouldBe 2
        projected.getPower(opposingElves) shouldBe 1
    }
})
