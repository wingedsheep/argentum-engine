package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.WhiteLotusTile
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * White Lotus Tile — "{4} Artifact. This artifact enters tapped. {T}: Add X mana of any one color,
 * where X is the greatest number of creatures you control that have a creature type in common."
 *
 * Covers (a) the enters-tapped replacement and (b) the dynamic mana ability: X is the largest
 * shared-creature-type tribe, with a multi-type creature counting toward each of its tribes.
 */
class WhiteLotusTileScenarioTest : FunSpec({

    val manaAbilityId = WhiteLotusTile.activatedAbilities[0].id

    // A Bird Soldier proves a multi-type creature feeds both the Bird tally AND the Soldier tally.
    val BirdSoldier = card("Lotus Test Bird Soldier") {
        manaCost = "{1}"
        typeLine = "Creature — Bird Soldier"
        power = 1
        toughness = 1
    }
    val Bird = card("Lotus Test Bird") {
        manaCost = "{1}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
    }
    val Soldier = card("Lotus Test Soldier") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BirdSoldier, Bird, Soldier))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    test("the artifact enters tapped") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tile = d.putCardInHand(you, "White Lotus Tile")
        d.giveColorlessMana(you, 4)
        d.castSpell(you, tile).error shouldBe null
        d.bothPass() // resolve the artifact spell

        val onBattlefield = d.findPermanent(you, "White Lotus Tile")!!
        d.isTapped(onBattlefield) shouldBe true
    }

    test("adds X mana of one chosen color where X is the largest shared creature type") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Board: Bird Soldier, Bird, Soldier, Soldier.
        //   Bird tally    = {Bird Soldier, Bird}             = 2
        //   Soldier tally = {Bird Soldier, Soldier, Soldier} = 3   <- largest shared type
        // The Bird Soldier counts toward both tribes, so X = 3.
        d.putCreatureOnBattlefield(you, "Lotus Test Bird Soldier")
        d.putCreatureOnBattlefield(you, "Lotus Test Bird")
        d.putCreatureOnBattlefield(you, "Lotus Test Soldier")
        d.putCreatureOnBattlefield(you, "Lotus Test Soldier")

        // White Lotus Tile entered via direct placement (untapped) so we can tap it.
        val tile = d.putPermanentOnBattlefield(you, "White Lotus Tile")

        val result = d.submit(ActivateAbility(playerId = you, sourceId = tile, abilityId = manaAbilityId))
        result.isPaused shouldBe true
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.WHITE))

        val pool = d.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.getAmount(Color.WHITE) shouldBe 3
        // The X copies are all the single chosen color — no other color was produced.
        pool.getAmount(Color.GREEN) shouldBe 0
    }
})
