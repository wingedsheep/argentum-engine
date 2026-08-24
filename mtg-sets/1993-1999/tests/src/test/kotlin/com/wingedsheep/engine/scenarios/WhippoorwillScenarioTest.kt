package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.Whippoorwill
import com.wingedsheep.mtg.sets.definitions.lea.cards.SamiteHealer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Whippoorwill.
 *
 * The middle clause is only observable through a creature that *would* have survived, so every case
 * here is the same board twice: a 3/3 with a 1-damage prevention shield, hit for 3.
 *
 *  - unmarked: 1 prevented, 2 marked, it lives — the control that proves the shield works at all;
 *  - marked:   the shield does nothing, 3 damage kills it;
 *  - and the bystander's shield still works, which is what separates this from the *global*
 *    "damage can't be prevented this turn" that would have blanked every shield on the table.
 *
 * The dies-clause is checked on the same kill: the marked creature must end in exile, not the
 * graveyard.
 */
class WhippoorwillScenarioTest : FunSpec({

    val whippoorwillAbility = Whippoorwill.activatedAbilities.first().id
    val healerAbility = SamiteHealer.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Whippoorwill)
        return driver
    }

    /**
     * Tap a Samite Healer to put a 1-damage prevention shield on [victim]. The Healer is controlled
     * by the active player throughout — activating an ability needs priority, and the opponent
     * never holds it in these scenarios.
     */
    fun shield(driver: GameTestDriver, healerController: EntityId, healer: EntityId, victim: EntityId) {
        driver.submit(
            ActivateAbility(
                playerId = healerController,
                sourceId = healer,
                abilityId = healerAbility,
                targets = listOf(entityIdToChosenTarget(driver.state, victim)),
            )
        ).error shouldBe null
        driver.bothPass()
    }

    /** Bolt [victim] for 3. */
    fun bolt(driver: GameTestDriver, caster: EntityId, victim: EntityId) {
        driver.giveMana(caster, Color.RED, 1)
        val boltId = driver.putCardInHand(caster, "Lightning Bolt")
        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = boltId,
                targets = listOf(entityIdToChosenTarget(driver.state, victim)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()
    }

    test("control: a shielded 3/3 survives 3 damage when nothing has marked it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val healer = driver.putCreatureOnBattlefield(me, "Samite Healer")
        driver.removeSummoningSickness(healer)

        shield(driver, me, healer, victim)
        bolt(driver, me, victim)

        withClue("1 of the 3 prevented leaves 2 marked on a 3/3") {
            (driver.findPermanent(opponent, "Centaur Courser") != null) shouldBe true
        }
    }

    test("a marked creature's shield does nothing, and it is exiled rather than buried") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bird = driver.putCreatureOnBattlefield(me, "Whippoorwill")
        driver.removeSummoningSickness(bird)
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val healer = driver.putCreatureOnBattlefield(me, "Samite Healer")
        driver.removeSummoningSickness(healer)

        driver.giveMana(me, Color.GREEN, 2)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = bird,
                abilityId = whippoorwillAbility,
                targets = listOf(entityIdToChosenTarget(driver.state, victim)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        shield(driver, me, healer, victim)
        bolt(driver, me, victim)

        withClue("the shield was ignored, so all 3 landed on a 3/3") {
            driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        }
        withClue("and it died to exile, not to the graveyard") {
            driver.getExileCardNames(opponent) shouldBe listOf("Centaur Courser")
            driver.getGraveyardCardNames(opponent).contains("Centaur Courser") shouldBe false
        }
    }

    test("a bystander's shield is untouched — the shutoff is per creature, not global") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bird = driver.putCreatureOnBattlefield(me, "Whippoorwill")
        driver.removeSummoningSickness(bird)
        val marked = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bystander = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val healer = driver.putCreatureOnBattlefield(me, "Samite Healer")
        driver.removeSummoningSickness(healer)

        driver.giveMana(me, Color.GREEN, 2)
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = bird,
                abilityId = whippoorwillAbility,
                targets = listOf(entityIdToChosenTarget(driver.state, marked)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // The shield goes on the *bystander*, who was never marked.
        shield(driver, me, healer, bystander)
        bolt(driver, me, bystander)

        withClue("marking one creature must not blank everyone else's prevention") {
            (driver.findPermanent(opponent, "Centaur Courser") != null) shouldBe true
        }
        withClue("and the global flag was never set") {
            driver.state.damageCantBePreventedThisTurn shouldBe false
        }
    }
})
