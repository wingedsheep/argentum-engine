package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.MagdaTheHoardmaster
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Magda, the Hoardmaster {1}{R} — Dwarf Berserker 2/2.
 * "Whenever you commit a crime, create a tapped Treasure token. This ability triggers only
 *  once each turn.
 *  Sacrifice three Treasures: Create a 4/4 red Scorpion Dragon creature token with flying and
 *  haste. Activate only as a sorcery."
 */
class MagdaTheHoardmasterTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MagdaTheHoardmaster, PredefinedTokens.Treasure))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Swamp" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast a Lightning Bolt at the opponent — targeting an opponent is a crime — and resolve it. */
    fun GameTestDriver.commitCrime(caster: EntityId, opponent: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, targets = listOf(opponent))
        bothPass() // resolve Bolt -> commit-crime -> Treasure trigger on stack
        bothPass() // resolve Treasure trigger
    }

    fun GameTestDriver.treasures(player: EntityId): List<EntityId> =
        getPermanents(player).filter { getCardName(it) == "Treasure" }

    test("committing a crime creates a tapped Treasure, but only once each turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Magda, the Hoardmaster")

        driver.treasures(me).size shouldBe 0

        driver.commitCrime(me, opp)
        val treasures = driver.treasures(me)
        treasures.size shouldBe 1
        // The Treasure enters tapped.
        driver.isTapped(treasures.first()) shouldBe true

        // A second crime the same turn must NOT make another Treasure (once per turn).
        driver.commitCrime(me, opp)
        driver.treasures(me).size shouldBe 1
    }

    test("sacrifice three Treasures: create a 4/4 flying, haste Scorpion Dragon") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val magda = driver.putCreatureOnBattlefield(me, "Magda, the Hoardmaster")
        driver.removeSummoningSickness(magda)

        repeat(3) { driver.putPermanentOnBattlefield(me, "Treasure") }
        val treasures = driver.treasures(me)
        treasures.size shouldBe 3

        val abilityId = MagdaTheHoardmaster.activatedAbilities.first().id
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = magda,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = treasures)
            )
        ).error shouldBe null
        driver.bothPass()

        // All three Treasures were sacrificed.
        driver.treasures(me).size shouldBe 0

        // A 4/4 red Scorpion Dragon with flying and haste was created.
        val dragon = driver.getPermanents(me).firstOrNull { driver.getCardName(it) == "Scorpion Dragon Token" }
        dragon shouldNotBe null
        driver.state.projectedState.getPower(dragon!!) shouldBe 4
        driver.state.projectedState.getToughness(dragon) shouldBe 4
        driver.state.projectedState.hasKeyword(dragon, Keyword.FLYING) shouldBe true
        driver.state.projectedState.hasKeyword(dragon, Keyword.HASTE) shouldBe true
    }

    test("the activated ability is sorcery-speed only — not castable on the opponent's turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val magda = driver.putCreatureOnBattlefield(me, "Magda, the Hoardmaster")
        driver.removeSummoningSickness(magda)
        repeat(3) { driver.putPermanentOnBattlefield(me, "Treasure") }

        // Move to the opponent's turn.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe driver.getOpponent(me)

        val abilityId = MagdaTheHoardmaster.activatedAbilities.first().id
        // Sorcery-speed: cannot activate during another player's turn.
        driver.submit(ActivateAbility(playerId = me, sourceId = magda, abilityId = abilityId))
            .isSuccess shouldBe false
        // No dragon, treasures untouched.
        driver.getPermanents(me).none { driver.getCardName(it) == "Scorpion Dragon Token" } shouldBe true
        driver.treasures(me).size shouldBe 3
    }
})
