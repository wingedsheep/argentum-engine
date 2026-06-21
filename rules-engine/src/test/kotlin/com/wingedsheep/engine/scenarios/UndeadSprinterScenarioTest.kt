package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.CreatureSubtypesDiedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Undead Sprinter (DSK #237) — {B}{R} 2/2 Creature — Zombie.
 *
 * "Trample, haste
 *  You may cast this card from your graveyard if a non-Zombie creature died this turn.
 *  If you do, this creature enters with a +1/+1 counter on it."
 *
 * Exercises:
 *  - the conditional self cast-from-graveyard permission
 *    (`MayCastSelfFromZones(GRAVEYARD, condition = NonSubtypeCreatureDiedThisTurn(ZOMBIE))`),
 *    gated on the global subtype-filtered death tracker
 *    ([CreatureSubtypesDiedThisTurnComponent]); and
 *  - the graveyard-cast-linked +1/+1 counter rider
 *    (`EntersWithCounters(selfOnly, condition = WasCastFromGraveyard)`).
 */
class UndeadSprinterScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun canCastFromGraveyard(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean {
        val enumerator = LegalActionEnumerator.create(driver.cardRegistry)
        val actions = enumerator.enumerate(driver.state, player, EnumerationMode.FULL)
        return actions.any { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Record a single non-Zombie creature death (a Human) under [player]'s control this turn. */
    fun GameTestDriver.recordNonZombieDeath(player: EntityId) =
        addComponent(player, CreatureSubtypesDiedThisTurnComponent(listOf(setOf("Human"))))

    /** Record a single Zombie creature death under [player]'s control this turn. */
    fun GameTestDriver.recordZombieDeath(player: EntityId) =
        addComponent(player, CreatureSubtypesDiedThisTurnComponent(listOf(setOf("Zombie"))))

    test("a non-Zombie creature died: castable from graveyard, enters 3/3 with a +1/+1 counter, trample+haste") {
        val d = newDriver()
        val you = d.player1

        val sprinter = d.putCardInGraveyard(you, "Undead Sprinter")
        d.recordNonZombieDeath(you)
        d.giveMana(you, Color.BLACK, 1)
        d.giveMana(you, Color.RED, 1)

        canCastFromGraveyard(d, you, sprinter) shouldBe true

        d.castSpell(you, sprinter)
        d.bothPass() // resolve onto the battlefield

        val perm = d.findPermanent(you, "Undead Sprinter")!!
        d.plusOneCounters(perm) shouldBe 1 // 2/2 base + counter = 3/3
        d.state.projectedState.getPower(perm) shouldBe 3
        d.state.projectedState.getToughness(perm) shouldBe 3
        d.state.projectedState.hasKeyword(perm, Keyword.TRAMPLE) shouldBe true
        d.state.projectedState.hasKeyword(perm, Keyword.HASTE) shouldBe true
    }

    test("only a Zombie died: NOT castable from graveyard") {
        val d = newDriver()
        val you = d.player1

        val sprinter = d.putCardInGraveyard(you, "Undead Sprinter")
        d.recordZombieDeath(you)
        d.giveMana(you, Color.BLACK, 1)
        d.giveMana(you, Color.RED, 1)

        canCastFromGraveyard(d, you, sprinter) shouldBe false
    }

    test("nothing died: NOT castable from graveyard") {
        val d = newDriver()
        val you = d.player1

        val sprinter = d.putCardInGraveyard(you, "Undead Sprinter")
        d.giveMana(you, Color.BLACK, 1)
        d.giveMana(you, Color.RED, 1)

        canCastFromGraveyard(d, you, sprinter) shouldBe false
    }

    test("an opponent's non-Zombie creature dying also enables the global permission") {
        val d = newDriver()
        val you = d.player1
        val opponent = d.getOpponent(you)

        val sprinter = d.putCardInGraveyard(you, "Undead Sprinter")
        // Death recorded on the opponent — the condition is global, so it still enables the cast.
        d.recordNonZombieDeath(opponent)
        d.giveMana(you, Color.BLACK, 1)
        d.giveMana(you, Color.RED, 1)

        canCastFromGraveyard(d, you, sprinter) shouldBe true
    }

    test("end-to-end: destroying a real non-Zombie creature records its subtypes and enables the cast") {
        val d = newDriver()
        val you = d.player1

        val sprinter = d.putCardInGraveyard(you, "Undead Sprinter")
        // A real Goblin (Goblin Guide) on the battlefield, killed by Doom Blade → graveyard.
        val goblin = d.putPermanentOnBattlefield(you, "Goblin Guide")
        canCastFromGraveyard(d, you, sprinter) shouldBe false // nothing dead yet

        val doomBlade = d.putCardInHand(you, "Doom Blade")
        d.giveMana(you, Color.BLACK, 2) // {1}{B}
        d.castSpell(you, doomBlade, targets = listOf(goblin))
        d.bothPass() // resolve — Goblin Guide is destroyed

        // ZoneTransitionService recorded the dying creature's last-known subtypes.
        val recorded = d.state.getEntity(you)
            ?.get<CreatureSubtypesDiedThisTurnComponent>()?.diedSubtypeSets
        recorded shouldBe listOf(setOf("Goblin", "Scout"))

        // A non-Zombie (Goblin) died, so the graveyard cast is now legal.
        canCastFromGraveyard(d, you, sprinter) shouldBe true
    }

    test("cast normally from hand: no +1/+1 counter (stays 2/2)") {
        val d = newDriver()
        val you = d.player1

        val sprinter = d.putCardInHand(you, "Undead Sprinter")
        // A non-Zombie died, proving the counter is tied to the *graveyard* cast, not the death.
        d.recordNonZombieDeath(you)
        d.giveMana(you, Color.BLACK, 1)
        d.giveMana(you, Color.RED, 1)

        d.castSpell(you, sprinter)
        d.bothPass()

        val perm = d.findPermanent(you, "Undead Sprinter")!!
        d.plusOneCounters(perm) shouldBe 0
        d.state.projectedState.getPower(perm) shouldBe 2
        d.state.projectedState.getToughness(perm) shouldBe 2
    }
})
