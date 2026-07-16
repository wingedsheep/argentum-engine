package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.HiveheartShaman
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hiveheart Shaman (VOW #202) — {3}{G} Creature — Human Shaman 3/5.
 *
 * "Whenever this creature attacks, you may search your library for a basic land card that
 * doesn't share a land type with a land you control, put that card onto the battlefield, then
 * shuffle. {5}{G}: Create a 1/1 green Insect creature token. Put X +1/+1 counters on it, where
 * X is the number of basic land types among lands you control. Activate only as a sorcery."
 *
 * Exercises the two fidelity fixes over the mtgish draft:
 *  1. The attack-trigger search filter must exclude basic lands sharing a type with a land you
 *     already control — not just "any basic land" (`notSharingLandTypeWithPermanentYouControl`).
 *  2. The activated ability's counter count is the number of *distinct* basic land types among
 *     lands you control (`DynamicAmounts.domain`), not the raw number of lands.
 */
class HiveheartShamanScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HiveheartShaman)
        return driver
    }

    fun libraryNames(driver: GameTestDriver, player: EntityId): List<String> =
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).mapNotNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name
        }

    fun battlefieldLandNames(driver: GameTestDriver, player: EntityId): List<String> =
        driver.getLands(player).mapNotNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name
        }

    /** The optional ("you may") search surfaces a yes/no gate before the card selection. */
    fun GameTestDriver.acceptOptionalSearch(player: EntityId) {
        if (pendingDecision is YesNoDecision) {
            submitYesNo(player, true)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attack trigger: only a basic land NOT sharing a type with a controlled land is offered.
    // ─────────────────────────────────────────────────────────────────────────
    test("attacking offers only a basic land that doesn't share a type with a land you control") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val shaman = driver.putCreatureOnBattlefield(me, "Hiveheart Shaman")
        driver.removeSummoningSickness(shaman)
        // We already control a Forest, so a Forest in the library must NOT be offered — only
        // Island/Swamp/Mountain/Plains are legal (no shared land type).
        driver.putLandOnBattlefield(me, "Forest")

        val forestInLibrary = driver.putCardOnTopOfLibrary(me, "Forest")
        val islandInLibrary = driver.putCardOnTopOfLibrary(me, "Island")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(shaman), opponent)

        // Resolve the attack trigger.
        driver.bothPass()
        driver.acceptOptionalSearch(me)

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.maxSelections shouldBe 1
        decision.options shouldContain islandInLibrary
        decision.options shouldNotContain forestInLibrary

        driver.submitCardSelection(me, listOf(islandInLibrary))

        battlefieldLandNames(driver, me) shouldContain "Island"
        libraryNames(driver, me) shouldNotContain "Island"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // A land with no land type of its own (trivial "shares none") — sanity check that the
    // predicate's own-side check doesn't crash and basics still compose correctly. Covered
    // implicitly above (basics always have exactly one land type); this test instead confirms
    // that with NO lands controlled, every basic land in the library is offered.
    // ─────────────────────────────────────────────────────────────────────────
    test("with no lands controlled, any basic land in the library is offered") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val shaman = driver.putCreatureOnBattlefield(me, "Hiveheart Shaman")
        driver.removeSummoningSickness(shaman)
        // No lands controlled at all.

        val mountainInLibrary = driver.putCardOnTopOfLibrary(me, "Mountain")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(shaman), opponent)
        driver.bothPass()
        driver.acceptOptionalSearch(me)

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain mountainInLibrary
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activated ability: counters = number of DISTINCT basic land types, not raw land count.
    // ─────────────────────────────────────────────────────────────────────────
    test("activated ability puts counters equal to distinct basic land types, not land count") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val shaman = driver.putCreatureOnBattlefield(me, "Hiveheart Shaman")

        // Four lands controlled, but only TWO distinct basic land types (Forest, Forest, Forest,
        // Island). A buggy "count lands" implementation would put 4 counters; the correct
        // "count distinct basic land types" implementation puts 2.
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(me, "Island")

        driver.giveMana(me, Color.GREEN, 6)
        val abilityId = HiveheartShaman.activatedAbilities.first().id
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = shaman,
                abilityId = abilityId,
                targets = emptyList()
            )
        )
        driver.bothPass()

        val battlefield = driver.getPermanents(me)
        val insectToken = battlefield.firstOrNull {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Insect Token"
        }
        val insect = requireNotNull(insectToken) { "Insect token was not created" }

        val counters = driver.state.getEntity(insect)
            ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            ?.getCount(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        counters shouldBe 2
    }
})
