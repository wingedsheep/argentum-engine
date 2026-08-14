package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.dft.cards.GontiNightMinister
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gonti, Night Minister (DFT #87) — {2}{B}{B} Legendary Creature — Aetherborn Rogue 3/4.
 *
 *   Whenever a player casts a spell they don't own, that player creates a Treasure token.
 *   Whenever a creature deals combat damage to one of your opponents, its controller looks at the
 *   top card of that opponent's library and exiles it face down. They may play that card for as
 *   long as it remains exiled. Mana of any type can be spent to cast a spell this way.
 *
 * Neither ability necessarily acts on Gonti's controller, and that is what these tests pin down.
 * Both need three seats to show it: with only two players, any creature dealing combat damage to
 * an opponent of Gonti's controller is *itself* controlled by Gonti's controller, so a payoff
 * mis-wired to "you" would look correct. In a pod, the thief and the victim can both be Gonti's
 * opponents — the ability's controller sits out entirely and gets nothing.
 */
class GontiNightMinisterScenarioTest : FunSpec({

    // The card stolen off the top of the victim's library. Its {G}{G} cost is paid from Swamps,
    // which only works because the grant carries "mana of any type can be spent".
    val stolenCreature = card("Gonti Test Sprout") {
        manaCost = "{G}{G}"
        typeLine = "Creature — Plant"
        power = 1
        toughness = 1
    }

    // A black creature the caster owns outright — the control for the Treasure trigger.
    val ownedCreature = card("Gonti Test Thug") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Rogue"
        power = 2
        toughness = 2
    }

    fun newDriver(seats: Int, startingPlayer: Int): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PredefinedTokens.Treasure)
        driver.registerCard(GontiNightMinister)
        driver.registerCard(stolenCreature)
        driver.registerCard(ownedCreature)
        val players = driver.initMultiplayer(
            decks = List(seats) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = startingPlayer,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to players
    }

    /** Pass priority once for whoever holds it, resolving any decision that surfaces first. */
    fun GameTestDriver.passOnce() {
        if (pendingDecision != null) autoResolveDecision()
        else state.priorityPlayerId?.let { passPriority(it) }
    }

    fun GameTestDriver.permanentNames(playerId: EntityId): List<String> =
        getPermanents(playerId).mapNotNull { getCardName(it) }

    /**
     * Three seats: [gontiController] watches while [thief] attacks [victim]. Returns the card that
     * was on top of the victim's library, driven all the way through the theft trigger.
     */
    fun stealScenario(): Triple<GameTestDriver, List<EntityId>, EntityId> {
        // Seat 1 (index 1) starts, so the thief is the active player and can attack immediately.
        val (driver, players) = newDriver(seats = 3, startingPlayer = 1)
        val (gontiController, thief, victim) = players

        driver.putPermanentOnBattlefield(gontiController, "Gonti, Night Minister")
        val bear = driver.putCreatureOnBattlefield(thief, "Grizzly Bears")
        driver.removeSummoningSickness(bear)
        val stolen = driver.putCardOnTopOfLibrary(victim, "Gonti Test Sprout")
        // Off-color sources for the {G}{G} cost — the whole point of `withAnyManaType`.
        repeat(2) { driver.putLandOnBattlefield(thief, "Swamp") }

        val turnBefore = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("Combat must happen on the thief's own turn — the step must not have been skipped") {
            driver.state.turnNumber shouldBe turnBefore
        }
        driver.declareAttackers(thief, listOf(bear), victim)
        var blocksDeclared = false
        run {
            repeat(40) {
                if (stolen !in driver.state.getZone(victim, Zone.LIBRARY)) return@run
                // Only the defending player declares blockers; the pod's third seat just passes.
                if (!blocksDeclared &&
                    driver.state.step == Step.DECLARE_BLOCKERS &&
                    driver.state.priorityPlayerId == victim
                ) {
                    driver.declareNoBlockers(victim)
                    blocksDeclared = true
                } else {
                    driver.passOnce()
                }
            }
        }
        return Triple(driver, players, stolen)
    }

    test("the damaging creature's controller — not Gonti's controller — exiles the card face down and may cast it with any mana") {
        val (driver, players, stolen) = stealScenario()
        val (gontiController, thief, victim) = players

        withClue("The top card of the victim's library is exiled face down, into their own exile") {
            driver.getExile(victim) shouldContain stolen
            driver.state.getEntity(stolen)?.get<FaceDownComponent>().shouldNotBeNull()
        }

        val permission = driver.state.mayPlayPermissions.single { stolen in it.cardIds }
        withClue("'its controller' is the attacking creature's controller, not the player who controls Gonti") {
            permission.controllerId shouldBe thief
        }
        withClue("'for as long as it remains exiled' is a permanent grant, with the any-mana relaxation") {
            permission.permanent shouldBe true
            permission.withAnyManaType shouldBe true
        }

        // Cast the stolen card in the thief's postcombat main, paying {G}{G} from two Swamps.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        withClue("The stolen card is castable out of the victim's exile by the thief") {
            driver.castSpell(thief, stolen).isSuccess shouldBe true
        }
        repeat(8) { driver.passOnce() }

        withClue("The stolen creature resolves under the thief's control") {
            driver.findPermanent(thief, "Gonti Test Sprout").shouldNotBeNull()
        }

        // Gonti's other half: the thief just cast a spell they don't own, so *they* get the
        // Treasure — even though the trigger belongs to a player who took no part in it.
        withClue("The Treasure goes to the caster of the not-owned spell") {
            driver.permanentNames(thief) shouldContain "Treasure"
            driver.permanentNames(gontiController) shouldNotContain "Treasure"
            driver.permanentNames(victim) shouldNotContain "Treasure"
        }
    }

    test("the exiled card is visible only to the player who may play it") {
        val (driver, players, stolen) = stealScenario()
        val (gontiController, thief, victim) = players

        val transformer = ClientStateTransformer(driver.cardRegistry)
        fun viewOf(playerId: EntityId) =
            transformer.transform(driver.state, viewingPlayerId = playerId).cards[stolen].shouldNotBeNull()

        withClue("The thief holds the play permission, so they see the real card") {
            viewOf(thief).name shouldBe "Gonti Test Sprout"
            viewOf(thief).isFaceDown shouldBe false
        }
        withClue("The victim it was taken from must not see it") {
            viewOf(victim).isFaceDown shouldBe true
        }
        withClue("Controlling the ability does not let Gonti's controller peek at someone else's spoils") {
            viewOf(gontiController).isFaceDown shouldBe true
        }
    }

    test("casting a spell you do own makes no Treasure") {
        val (driver, players) = newDriver(seats = 2, startingPlayer = 0)
        val (caster, gontiController) = players

        driver.putPermanentOnBattlefield(gontiController, "Gonti, Night Minister")
        val ownCard = driver.putCardInHand(caster, "Gonti Test Thug")
        repeat(2) { driver.putLandOnBattlefield(caster, "Swamp") }

        driver.castSpell(caster, ownCard).isSuccess shouldBe true
        repeat(8) { driver.passOnce() }

        withClue("The creature resolved, so the cast really happened") {
            driver.findPermanent(caster, "Gonti Test Thug").shouldNotBeNull()
        }
        withClue("Owner == caster, so the 'a spell they don't own' trigger never fires") {
            driver.permanentNames(caster) shouldNotContain "Treasure"
            driver.permanentNames(gontiController) shouldNotContain "Treasure"
        }
    }
})
