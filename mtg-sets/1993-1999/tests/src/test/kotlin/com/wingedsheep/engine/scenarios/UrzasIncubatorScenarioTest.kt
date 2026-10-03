package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.uds.cards.UrzasIncubator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Urza's Incubator {3} — Artifact.
 *
 * "As this artifact enters, choose a creature type.
 *  Creature spells of the chosen type cost {2} less to cast."
 *
 * Pins: the as-enters choice drives the filter; the discount applies to *every* player's creature
 * spells of that type (not only the controller's); other types are untouched; only generic mana is
 * reduced; and two Incubators stack (Scryfall ruling).
 */
class UrzasIncubatorScenarioTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(TestCards.all)
        register(UrzasIncubator)
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + UrzasIncubator)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Cast the Incubator and answer its as-enters creature-type choice with [creatureType]. */
    fun GameTestDriver.castIncubatorChoosing(player: EntityId, creatureType: String) {
        giveColorlessMana(player, 3)
        val card = putCardInHand(player, "Urza's Incubator")
        castSpell(player, card).error shouldBe null
        bothPass()

        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(creatureType)
        (index >= 0) shouldBe true
        submitDecision(player, OptionChosenResponse(decision.id, index))
    }

    fun GameTestDriver.costFor(caster: EntityId, cardName: String) =
        registry().let { reg ->
            CostCalculator(reg, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
                .calculateEffectiveCost(state, reg.requireCard(cardName), caster)
        }

    test("creature spells of the chosen type cost {2} less for every player; other types are untouched") {
        val d = createDriver()
        val you = d.player1
        val opponent = d.getOpponent(you)
        d.castIncubatorChoosing(you, "Zombie")
        d.findPermanent(you, "Urza's Incubator").shouldNotBeNull()

        withClue("Gurmag Angler {6}{B} (Zombie Fish) costs {4}{B} for its controller") {
            val cost = d.costFor(you, "Gurmag Angler")
            cost.genericAmount shouldBe 4
            cost.cmc shouldBe 5
        }
        withClue("...and for the opponent too — the Incubator is not limited to its controller") {
            d.costFor(opponent, "Gurmag Angler").genericAmount shouldBe 4
        }
        withClue("Centaur Courser {2}{G} is not a Zombie, so it stays {2}{G}") {
            d.costFor(you, "Centaur Courser").genericAmount shouldBe 2
        }
        withClue("Black Creature {1}{B}: only generic mana is reduced, so it costs {B}") {
            val cost = d.costFor(you, "Black Creature")
            cost.genericAmount shouldBe 0
            cost.cmc shouldBe 1
        }
    }

    test("a creature spell of the chosen type can actually be cast for the reduced cost") {
        val d = createDriver()
        val you = d.player1
        d.castIncubatorChoosing(you, "Zombie")

        d.giveColorlessMana(you, 4)
        d.giveMana(you, Color.BLACK, 1)
        val angler = d.putCardInHand(you, "Gurmag Angler")
        d.castSpell(you, angler).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Gurmag Angler").shouldNotBeNull()
    }

    test("multiple Incubators are cumulative") {
        val d = createDriver()
        val you = d.player1
        d.castIncubatorChoosing(you, "Zombie")
        d.castIncubatorChoosing(you, "Zombie")

        withClue("two Incubators naming Zombie take {4} off Gurmag Angler's {6}{B}") {
            d.costFor(you, "Gurmag Angler").genericAmount shouldBe 2
        }
    }
})
