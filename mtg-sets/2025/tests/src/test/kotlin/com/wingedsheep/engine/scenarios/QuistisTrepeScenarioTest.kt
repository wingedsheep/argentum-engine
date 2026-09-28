package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.QuistisTrepe
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Quistis Trepe (FIN) — {2}{U} Legendary Creature — Human Wizard 2/2.
 *
 *  "Blue Magic — When Quistis Trepe enters, you may cast target instant or sorcery card from a
 *   graveyard, and mana of any type can be spent to cast that spell. If that spell would be put
 *   into a graveyard, exile it instead."
 *
 * Exercises the ETB borrow-and-cast: Quistis targets an instant in a graveyard, exiles it, and
 * grants a may-play permission with `withAnyManaType` so it can be paid with off-color mana, plus
 * `insteadOfGraveyard = EXILE` so the cast spell ends up in exile rather than the graveyard.
 */
class QuistisTrepeScenarioTest : FunSpec({

    // An off-color instant ({R}) the opponent owns. Casting it from a {U}-only board with white/blue
    // mana proves "mana of any type can be spent".
    val borrowedInstant = card("Quistis Test Spark") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Quistis Test Spark deals 2 damage to any target."
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(QuistisTrepe)
        driver.registerCard(borrowedInstant)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("ETB grants a may-play permission for an instant in a graveyard with any-mana + exile-after-resolve") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        // The instant sits in the opponent's graveyard — "from a graveyard" reaches any graveyard.
        val instant = driver.putCardInGraveyard(opponent, "Quistis Test Spark")

        // Cast Quistis from hand so her "When ~ enters" trigger genuinely fires (a direct
        // putCreatureOnBattlefield would skip the enters event).
        val quistis = driver.putCardInHand(me, "Quistis Trepe")
        repeat(3) { driver.putLandOnBattlefield(me, "Island") } // {2}{U}
        driver.castSpell(me, quistis, emptyList())

        // Resolve the cast + the ETB trigger, targeting the instant when prompted.
        run {
            repeat(40) {
                if (driver.state.mayPlayPermissions.any { instant in it.cardIds }) return@run
                when (val pending = driver.pendingDecision) {
                    is ChooseTargetsDecision -> driver.submitTargetSelection(pending.playerId, listOf(instant))
                    null -> driver.bothPass()
                    else -> driver.autoResolveDecision()
                }
            }
        }

        // The instant left the opponent's graveyard for exile and is castable by me with any mana.
        driver.getGraveyard(opponent).contains(instant) shouldBe false
        val perm = driver.state.mayPlayPermissions.single { instant in it.cardIds }
        withClue("Permission allows spending mana of any type") {
            perm.withAnyManaType shouldBe true
        }

        // Cast the borrowed instant with blue mana (off-color), targeting the opponent.
        repeat(2) { driver.putLandOnBattlefield(me, "Island") }
        driver.castSpell(me, instant, listOf(opponent))
        run {
            repeat(25) {
                if (driver.state.getZone(opponent, Zone.EXILE).contains(instant)) return@run
                if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
            }
        }

        withClue("After resolving, the borrowed spell is exiled, not in the owner's graveyard") {
            driver.state.getZone(opponent, Zone.EXILE).contains(instant).shouldBeTrue()
            driver.getGraveyard(opponent).contains(instant) shouldBe false
        }
    }
})
