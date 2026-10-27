package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.MishrasFactory
import com.wingedsheep.mtg.sets.definitions.khm.cards.MoritteOfTheFrost
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Moritte of the Frost (KHM #223) — {2}{G}{U}{U} Legendary Snow Creature — Shapeshifter 0/0.
 * "Changeling. You may have Moritte enter as a copy of a permanent you control, except it's legendary
 * and snow in addition to its other types and, if it's a creature, it enters with two additional
 * +1/+1 counters on it and has changeling."
 *
 * "If it's a creature" asks about Moritte as it enters — its copiable values — so a land animated
 * by a non-copy effect is copied as a noncreature with no counters and no changeling (ruling).
 */
class MoritteOfTheFrostScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MoritteOfTheFrost, MishrasFactory))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.card(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!
    fun GameTestDriver.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.castMoritte(p: EntityId, copy: EntityId?, offered: (List<EntityId>) -> Unit = {}): EntityId {
        val moritte = putCardInHand(p, "Moritte of the Frost")
        giveMana(p, Color.GREEN, 2)
        giveMana(p, Color.BLUE, 3)
        castSpell(p, moritte).error shouldBe null
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        offered(decision.options)
        submitCardSelection(p, listOfNotNull(copy)).error shouldBe null
        return moritte
    }

    test("copying a creature: legendary snow copy with two +1/+1 counters and changeling; the original stays") {
        val d = driver()
        val p = d.activePlayer!!
        val giant = d.putCreatureOnBattlefield(p, "Hill Giant")
        val theirs = d.putCreatureOnBattlefield(d.getOpponent(p), "Hill Giant")

        val moritte = d.castMoritte(p, giant) { options ->
            options shouldContain giant
            withClue("only permanents you control are offered") { options shouldNotContain theirs }
        }

        val projected = d.state.projectedState
        d.card(moritte).name shouldBe "Hill Giant"
        projected.isLegendary(moritte) shouldBe true
        projected.getSupertypes(moritte) shouldContain "SNOW"
        d.plusOnes(moritte) shouldBe 2
        projected.getPower(moritte) shouldBe 5
        projected.getToughness(moritte) shouldBe 5
        projected.hasKeyword(moritte, Keyword.CHANGELING) shouldBe true
        withClue("changeling makes the copy every creature type") {
            projected.hasSubtype(moritte, "Elf") shouldBe true
            projected.hasSubtype(moritte, "Giant") shouldBe true
        }
        withClue("only Moritte is legendary, so the legend rule binds neither") {
            d.state.getBattlefield() shouldContain giant
            d.state.getBattlefield() shouldContain moritte
            projected.isLegendary(giant) shouldBe false
        }
    }

    test("copying a noncreature land: legendary snow land with no counters and no changeling") {
        val d = driver()
        val p = d.activePlayer!!
        val island = d.putLandOnBattlefield(p, "Island")

        val moritte = d.castMoritte(p, island)

        val projected = d.state.projectedState
        d.state.getBattlefield() shouldContain moritte
        d.card(moritte).name shouldBe "Island"
        projected.isCreature(moritte) shouldBe false
        projected.isLegendary(moritte) shouldBe true
        projected.getSupertypes(moritte) shouldContain "SNOW"
        d.plusOnes(moritte) shouldBe 0
        projected.hasKeyword(moritte, Keyword.CHANGELING) shouldBe false
    }

    test("copying a land animated by a non-copy effect gives a noncreature Moritte (ruling)") {
        val d = driver()
        val p = d.activePlayer!!
        val factory = d.putLandOnBattlefield(p, "Mishra's Factory")
        d.giveColorlessMana(p, 1)
        d.submitSuccess(ActivateAbility(p, factory, MishrasFactory.activatedAbilities[1].id))
        d.bothPass()
        d.state.projectedState.isCreature(factory) shouldBe true

        val moritte = d.castMoritte(p, factory)

        val projected = d.state.projectedState
        d.state.getBattlefield() shouldContain moritte
        d.card(moritte).name shouldBe "Mishra's Factory"
        projected.isCreature(moritte) shouldBe false
        d.plusOnes(moritte) shouldBe 0
        projected.hasKeyword(moritte, Keyword.CHANGELING) shouldBe false
    }

    test("declining the copy leaves a 0/0 that dies to state-based actions") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, "Hill Giant")

        val moritte = d.castMoritte(p, null)

        d.state.getBattlefield() shouldNotContain moritte
        d.state.getGraveyard(p) shouldContain moritte
    }
})
