package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.PaladinDanseSteelMaverick
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Paladin Danse, Steel Maverick (PIP #20).
 *
 * "Exile Paladin Danse: Each creature you control that's an artifact or Human gains indestructible
 *  until end of turn."
 *
 * The group is Creature AND (Artifact OR Human), restricted to your side: a plain non-Human
 * creature, a noncreature artifact and an opponent's Human are all left out.
 */
class PaladinDanseSteelMaverickScenarioTest : FunSpec({

    val abilityId = PaladinDanseSteelMaverick.activatedAbilities.first().id

    val humanSoldier = card("Test Human Soldier") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }
    val artifactConstruct = card("Test Artifact Construct") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        power = 2
        toughness = 2
    }
    val elfWarrior = card("Test Elf Warrior") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Warrior"
        power = 1
        toughness = 1
    }
    val trinket = card("Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    test("exiles Danse and grants indestructible only to your artifact or Human creatures") {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(PaladinDanseSteelMaverick, humanSoldier, artifactConstruct, elfWarrior, trinket)
        )
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1
        val opp = driver.player2

        val danse = driver.putCreatureOnBattlefield(me, "Paladin Danse, Steel Maverick")
        val human = driver.putCreatureOnBattlefield(me, "Test Human Soldier")
        val construct = driver.putCreatureOnBattlefield(me, "Test Artifact Construct")
        val elf = driver.putCreatureOnBattlefield(me, "Test Elf Warrior")
        val trinketId = driver.putPermanentOnBattlefield(me, "Test Trinket")
        val oppHuman = driver.putCreatureOnBattlefield(opp, "Test Human Soldier")

        driver.submit(ActivateAbility(playerId = me, sourceId = danse, abilityId = abilityId)).outcome shouldBe Outcome.Done
        // Exiled as the cost, before the ability resolves.
        driver.state.getZone(ZoneKey(me, Zone.EXILE)).contains(danse) shouldBe true

        driver.bothPass()
        driver.state.stack.size shouldBe 0

        val projected = driver.state.projectedState
        projected.hasKeyword(human, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.hasKeyword(construct, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.hasKeyword(elf, Keyword.INDESTRUCTIBLE) shouldBe false
        projected.hasKeyword(trinketId, Keyword.INDESTRUCTIBLE) shouldBe false
        projected.hasKeyword(oppHuman, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
