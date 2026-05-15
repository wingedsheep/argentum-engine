package com.wingedsheep.engine.handlers.effects.staticabilities

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * BDD test for the layer-7b characteristic-defining ability:
 *   "This creature's power is equal to the greatest mana value among permanents you control."
 *
 * Scenario S1 (from plans/1.json):
 *   GIVEN  A creature with printed power '*' has the CDA described above
 *   AND    The CDA creature (MV 3) is on the battlefield under player A
 *   AND    Player A also controls permanents with mana values 2, 4, and 1
 *   AND    Player B controls a permanent with mana value 7 (must be ignored)
 *   WHEN   The engine computes projected power via the layer-7b CDA
 *   THEN   Projected power equals 4 (max MV among player A's permanents, incl. the CDA creature)
 *   AND    Player B's higher-MV permanent does not influence the result
 */
class CharacteristicDefiningPowerEqualToGreatestManaValueAmongPermanentsYouControlTest : FunSpec({

    val projector = StateProjector()

    // CDA creature: power = greatest MV among permanents you control. MV 3 ({2}{G}).
    // max(3, 2, 4, 1) = 4 — does NOT exceed MV4 permanent, so expected power = 4.
    val CdaCreature = card("CDA Creature") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Test"
        dynamicStats(DynamicAmounts.battlefield(Player.You).maxManaValue())
    }

    // Player A's supporting permanents
    val PermanentMv2 = CardDefinition.creature(
        name = "Permanent MV2",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 1, toughness = 1
    )
    val PermanentMv4 = CardDefinition.creature(
        name = "Permanent MV4",
        manaCost = ManaCost.parse("{2}{G}{G}"),
        subtypes = emptySet(),
        power = 2, toughness = 2
    )
    val PermanentMv1 = CardDefinition.creature(
        name = "Permanent MV1",
        manaCost = ManaCost.parse("{G}"),
        subtypes = emptySet(),
        power = 1, toughness = 1
    )

    // Player B's permanent — mana value 7, must NOT influence player A's CDA
    val PermanentMv7 = CardDefinition.creature(
        name = "Permanent MV7",
        manaCost = ManaCost.parse("{4}{B}{B}{B}"),
        subtypes = emptySet(),
        power = 3, toughness = 3
    )

    fun createDriver(vararg extraCards: CardDefinition): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + extraCards.toList())
        return driver
    }

    fun GameTestDriver.init() {
        initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    context("Layer 7b CDA: power equals greatest mana value among permanents you control") {

        test("projected power equals the greatest mana value (4) among controller's permanents") {
            val driver = createDriver(CdaCreature, PermanentMv2, PermanentMv4, PermanentMv1, PermanentMv7)
            driver.init()

            val playerA = driver.activePlayer!!
            val playerB = driver.getOpponent(playerA)

            // Player A: CDA creature (MV 3) + permanents with MV 2, 4, 1
            val cdaCreature = driver.putCreatureOnBattlefield(playerA, "CDA Creature")
            driver.putCreatureOnBattlefield(playerA, "Permanent MV2")
            driver.putCreatureOnBattlefield(playerA, "Permanent MV4")
            driver.putCreatureOnBattlefield(playerA, "Permanent MV1")

            // Player B: permanent with MV 7 — must not raise player A's CDA power
            driver.putCreatureOnBattlefield(playerB, "Permanent MV7")

            val projected = projector.project(driver.state)

            // max(3, 2, 4, 1) = 4; player B's MV 7 is excluded
            projected.getPower(cdaCreature) shouldBe 4
        }

        test("player B's MV-7 permanent does not influence projected power") {
            val driver = createDriver(CdaCreature, PermanentMv2, PermanentMv4, PermanentMv1, PermanentMv7)
            driver.init()

            val playerA = driver.activePlayer!!
            val playerB = driver.getOpponent(playerA)

            val cdaCreature = driver.putCreatureOnBattlefield(playerA, "CDA Creature")
            driver.putCreatureOnBattlefield(playerA, "Permanent MV2")
            driver.putCreatureOnBattlefield(playerA, "Permanent MV4")
            driver.putCreatureOnBattlefield(playerA, "Permanent MV1")
            driver.putCreatureOnBattlefield(playerB, "Permanent MV7")

            val projected = projector.project(driver.state)

            // Must be 4, not 7
            projected.getPower(cdaCreature) shouldBe 4
        }
    }
})
