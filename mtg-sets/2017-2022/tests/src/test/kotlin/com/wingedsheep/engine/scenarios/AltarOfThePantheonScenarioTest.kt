package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.thb.cards.AltarOfThePantheon
import com.wingedsheep.mtg.sets.definitions.ths.cards.NyleaGodOfTheHunt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Altar of the Pantheon (THB #231) — {3} Artifact.
 * "Your devotion to each color and each combination of colors is increased by one.
 * {T}: Add one mana of any color. If you control a God, a Demigod, or a legendary enchantment,
 * you gain 1 life."
 *
 * The mana ability gains life only with a God, Demigod or legendary enchantment out, once however
 * many there are (ruling); the devotion raise turns a god on one pip early.
 */
class AltarOfThePantheonScenarioTest : FunSpec({

    val relic = card("Test Legendary Relic") {
        manaCost = "{2}"
        typeLine = "Legendary Enchantment"
    }

    val devotee = card("Test Green Devotee") {
        manaCost = "{G}{G}{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AltarOfThePantheon, NyleaGodOfTheHunt, relic, devotee))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to you
    }

    fun GameTestDriver.tapAltar(you: EntityId, altar: EntityId) {
        submit(
            ActivateAbility(you, altar, AltarOfThePantheon.activatedAbilities.first().id, manaColorChoice = Color.BLUE)
        ).error shouldBe null
        (state.getEntity(you)?.get<ManaPoolComponent>()?.blue ?: 0) shouldBe 1
    }

    test("tapping for mana with no God, Demigod or legendary enchantment gains no life") {
        val (d, you) = setup()
        val altar = d.putPermanentOnBattlefield(you, "Altar of the Pantheon")
        d.tapAltar(you, altar)
        d.getLifeTotal(you) shouldBe 20
    }

    test("a legendary enchantment makes the mana ability gain 1 life") {
        val (d, you) = setup()
        d.putPermanentOnBattlefield(you, "Test Legendary Relic")
        val altar = d.putPermanentOnBattlefield(you, "Altar of the Pantheon")
        d.tapAltar(you, altar)
        d.getLifeTotal(you) shouldBe 21
    }

    test("a God and a legendary enchantment still gain just 1 life") {
        val (d, you) = setup()
        d.putPermanentOnBattlefield(you, "Nylea, God of the Hunt")
        d.putPermanentOnBattlefield(you, "Test Legendary Relic")
        val altar = d.putPermanentOnBattlefield(you, "Altar of the Pantheon")
        d.tapAltar(you, altar)
        d.getLifeTotal(you) shouldBe 21
    }

    test("an opponent's legendary enchantment doesn't count") {
        val (d, you) = setup()
        d.putPermanentOnBattlefield(d.getOpponent(you), "Test Legendary Relic")
        val altar = d.putPermanentOnBattlefield(you, "Altar of the Pantheon")
        d.tapAltar(you, altar)
        d.getLifeTotal(you) shouldBe 20
    }

    test("the devotion raise makes Nylea a creature at four green pips") {
        val (d, you) = setup()
        val nylea = d.putPermanentOnBattlefield(you, "Nylea, God of the Hunt")
        d.putCreatureOnBattlefield(you, "Test Green Devotee")
        d.state.projectedState.isCreature(nylea) shouldBe false
        d.putPermanentOnBattlefield(you, "Altar of the Pantheon")
        d.state.projectedState.isCreature(nylea) shouldBe true
    }
})
