package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thorin Oakenshield — "As long as you have an enduring story, artifacts and creatures you control
 * have ward {1}."
 *
 * Two things are worth pinning beyond the gate itself. First, the union filter: "artifacts **and**
 * creatures" is one set containing both, so a noncreature artifact is warded just as a nonartifact
 * creature is — a filter that quietly kept only the creature branch would pass a creature-only test.
 * Second, the ward is granted through the layer system inside a `ConditionalStaticAbility`, so the
 * trigger the engine generates from the [com.wingedsheep.sdk.scripting.effects.WardCost] has to
 * appear and disappear with the designation.
 *
 * The mechanic's own rules live in [StoriedEnduringStoryTest].
 */
class ThorinOakenshieldScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    /** Three legendary permanents, one of them Thorin — the storied threshold, exactly met. */
    fun giveEnduringStory(d: GameTestDriver, playerId: EntityId): EntityId {
        val thorin = d.putCreatureOnBattlefield(playerId, "Thorin Oakenshield")
        d.putCreatureOnBattlefield(playerId, "Ori, Keeper of Songs")
        d.putCreatureOnBattlefield(playerId, "Óin the Brave")
        return thorin
    }

    test("without an enduring story nothing is warded and the removal spell just resolves") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Thorin alone is one qualifying permanent, not three.
        val thorin = d.putCreatureOnBattlefield(opponent, "Thorin Oakenshield")
        EnduringStoryService.has(d.state, opponent) shouldBe false
        d.state.projectedState.hasKeyword(thorin, Keyword.WARD) shouldBe false

        repeat(3) { d.putLandOnBattlefield(active, "Mountain") }
        d.giveMana(active, Color.RED, 1)
        val bolt = d.putCardInHand(active, "Lightning Bolt")
        d.castSpellWithTargets(active, bolt, listOf(ChosenTarget.Permanent(thorin))).isSuccess shouldBe true

        // No ward trigger to resolve first — the Bolt itself is the only thing on the stack.
        d.bothPass()
        d.pendingDecision shouldBe null

        // 3 damage on a 3/2 kills him.
        repeat(3) { if (d.state.priorityPlayerId != null) d.bothPass() }
        d.findPermanent(opponent, "Thorin Oakenshield") shouldBe null
    }

    test("with an enduring story a creature you control has ward {1}") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        giveEnduringStory(d, opponent)
        EnduringStoryService.has(d.state, opponent) shouldBe true

        // Target Ori rather than Thorin: the ward covers the whole group, not just its source.
        val ori = d.findPermanent(opponent, "Ori, Keeper of Songs")!!
        d.state.projectedState.hasKeyword(ori, Keyword.WARD) shouldBe true

        repeat(3) { d.putLandOnBattlefield(active, "Mountain") }
        d.giveMana(active, Color.RED, 1)
        val bolt = d.putCardInHand(active, "Lightning Bolt")
        d.castSpellWithTargets(active, bolt, listOf(ChosenTarget.Permanent(ori)))

        d.bothPass()
        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.playerId shouldBe active
        decision.requiredCost shouldBe "{1}"

        // Decline: the Bolt is countered and Ori — a 4/3 with the enduring story live — survives.
        d.submitManaAutoPayOrDecline(active, autoPay = false)
        repeat(3) { if (d.state.priorityPlayerId != null) d.bothPass() }
        d.findPermanent(opponent, "Ori, Keeper of Songs") shouldNotBe null
    }

    test("with an enduring story a noncreature artifact you control also has ward {1}") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        giveEnduringStory(d, opponent)
        val solRing = d.putPermanentOnBattlefield(opponent, "Sol Ring")
        d.state.projectedState.hasKeyword(solRing, Keyword.WARD) shouldBe true

        // Shatter ({1}{R}) destroys target artifact, so the ward has to be paid or the spell dies.
        repeat(4) { d.putLandOnBattlefield(active, "Mountain") }
        d.giveMana(active, Color.RED, 2)
        val shatter = d.putCardInHand(active, "Shatter")
        d.castSpellWithTargets(active, shatter, listOf(ChosenTarget.Permanent(solRing)))

        d.bothPass()
        val decision = d.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.requiredCost shouldBe "{1}"

        d.submitManaAutoPayOrDecline(active, autoPay = false)
        repeat(3) { if (d.state.priorityPlayerId != null) d.bothPass() }
        d.findPermanent(opponent, "Sol Ring") shouldNotBe null
    }

    test("the ward doesn't reach permanents an opponent controls") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        giveEnduringStory(d, opponent)

        // A creature the *active* player controls: "you control" is the storied player, so this one
        // is untouched even though the designation is live across the table.
        val bear = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        d.state.projectedState.hasKeyword(bear, Keyword.WARD) shouldBe false
    }
})
