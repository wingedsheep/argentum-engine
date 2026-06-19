package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.NitaForumConciliator
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Nita, Forum Conciliator (SOS) — {1}{W}{B} Legendary Creature — Human Advisor 2/3.
 *
 * "Whenever you cast a spell you don't own, put a +1/+1 counter on each creature you control.
 *  {2}, Sacrifice another creature: Exile target instant or sorcery card from an opponent's
 *  graveyard. You may cast it this turn, and mana of any type can be spent to cast that spell.
 *  If that spell would be put into a graveyard, exile it instead. Activate only as a sorcery."
 *
 * Exercises the full borrow-and-cast loop end-to-end: the activated ability steals an opponent's
 * instant/sorcery into exile and grants a may-play permission; casting that "spell you don't own"
 * fires Nita's +1/+1 trigger; and after resolving, the borrowed card lands in exile instead of the
 * opponent's graveyard (the exile-after-resolve rider).
 */
class NitaForumConciliatorTest : FunSpec({

    // An off-color instant ({R}) the opponent owns. Casting it via Nita with white/black mana
    // proves "mana of any type can be spent".
    val borrowedInstant = card("Nita Test Spark") {
        manaCost = "{R}"
        typeLine = "Instant"
        oracleText = "Nita Test Spark deals 2 damage to any target."
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NitaForumConciliator)
        driver.registerCard(borrowedInstant)
        driver.initMirrorMatch(Deck.of("Plains" to 20, "Swamp" to 20), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.counters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("steal an opponent's instant, cast it with any mana → triggers +1/+1, then exiles instead of graveyard") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2

        val nita = driver.putCreatureOnBattlefield(me, "Nita, Forum Conciliator")
        driver.removeSummoningSickness(nita)
        // Another creature to feed the sacrifice cost.
        val fodder = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        // The opponent's instant in their graveyard.
        val instant = driver.putCardInGraveyard(opponent, "Nita Test Spark")
        // White/black mana to pay {2} for the ability and {R} for the borrowed spell with any mana.
        repeat(4) { driver.putLandOnBattlefield(me, "Plains") }

        val abilityId = driver.cardRegistry.getCard("Nita, Forum Conciliator")!!
            .activatedAbilities.first().id

        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = nita,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Card(instant, opponent, Zone.GRAVEYARD)),
            )
        )

        // Resolve any sacrifice / payment decisions, then the ability itself.
        run {
            repeat(25) {
                if (driver.state.mayPlayPermissions.any { instant in it.cardIds }) return@run
                when (val pending = driver.pendingDecision) {
                    null -> driver.bothPass()
                    is ChooseTargetsDecision -> driver.submitTargetSelection(pending.playerId, listOf(instant))
                    else -> driver.autoResolveDecision()
                }
            }
        }
        withClue("Activation should not error: ${result.error}") { result.error shouldBe null }

        // The instant left the opponent's graveyard for exile and is castable by me with any mana.
        driver.getGraveyard(opponent).contains(instant) shouldBe false
        val perm = driver.state.mayPlayPermissions.single { instant in it.cardIds }
        perm.withAnyManaType shouldBe true

        // Fodder was sacrificed.
        driver.findPermanent(me, "Savannah Lions") shouldBe null

        // Cast the borrowed instant (owner = opponent, controller = me) → Nita's trigger fires.
        driver.castSpell(me, instant, listOf(opponent))
        run {
            repeat(25) {
                val onBattlefield = driver.findPermanent(me, "Nita, Forum Conciliator")
                val nitaCounters = onBattlefield?.let { driver.counters(it) } ?: 0
                if (nitaCounters > 0 &&
                    driver.state.getZone(opponent, Zone.EXILE).contains(instant)
                ) return@run
                if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
            }
        }

        // Nita got a +1/+1 counter from casting "a spell you don't own".
        driver.counters(nita) shouldBe 1

        // The borrowed spell went to exile (its owner's exile), not the opponent's graveyard.
        driver.state.getZone(opponent, Zone.EXILE).contains(instant).shouldBeTrue()
        driver.getGraveyard(opponent).contains(instant) shouldBe false
    }
})
