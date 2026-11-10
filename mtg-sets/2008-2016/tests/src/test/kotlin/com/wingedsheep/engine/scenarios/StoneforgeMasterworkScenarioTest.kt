package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.lrw.cards.WoodlandChangeling
import com.wingedsheep.mtg.sets.definitions.ogw.cards.StoneforgeMasterwork
import com.wingedsheep.mtg.sets.definitions.p02.cards.GoblinPiker
import com.wingedsheep.mtg.sets.definitions.por.cards.RagingGoblin
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stoneforge Masterwork {1} — Artifact — Equipment
 *   Equipped creature gets +1/+1 for each other creature you control that shares a creature type
 *   with it.
 *   Equip {2}
 *
 * The count is relative to the *equipped* creature (not the Equipment), excludes that creature,
 * only looks at creatures the Equipment's controller controls, and reads projected types — so a
 * changeling shares a type with everything.
 */
class StoneforgeMasterworkScenarioTest : FunSpec({

    fun GameTestDriver.attach(equipment: EntityId, creature: EntityId) {
        replaceState(
            state
                .updateEntity(equipment) { it.with(AttachedToComponent(creature)) }
                .updateEntity(creature) { it.with(AttachmentsComponent(listOf(equipment))) }
        )
    }

    fun GameTestDriver.pt(id: EntityId) = state.projectedState.let { it.getPower(id) to it.getToughness(id) }

    fun setup(): Pair<GameTestDriver, Pair<EntityId, EntityId>> {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all + listOf(StoneforgeMasterwork, GoblinPiker, RagingGoblin, WoodlandChangeling, GrizzlyBears)
        )
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to (p1 to p2)
    }

    test("counts other creatures you control sharing a type with the equipped creature, changelings included") {
        val (d, players) = setup()
        val (p1, p2) = players
        val piker = d.putCreatureOnBattlefield(p1, "Goblin Piker") // Goblin Warrior 2/1
        d.putCreatureOnBattlefield(p1, "Raging Goblin") // shares Goblin
        d.putCreatureOnBattlefield(p1, "Woodland Changeling") // every creature type
        d.putCreatureOnBattlefield(p1, "Grizzly Bears") // Bear: shares nothing with the Piker
        d.putCreatureOnBattlefield(p2, "Raging Goblin") // shares Goblin, but an opponent controls it

        val masterwork = d.putPermanentOnBattlefield(p1, "Stoneforge Masterwork")
        withClue("unattached, nothing is boosted") { d.pt(piker) shouldBe (2 to 1) }

        d.attach(masterwork, piker)
        withClue("Raging Goblin + Woodland Changeling → +2/+2; the Piker itself and the opponent's Goblin don't count") {
            d.pt(piker) shouldBe (4 to 3)
        }
    }

    test("re-reads the count against whichever creature it is attached to") {
        val (d, players) = setup()
        val (p1, _) = players
        val piker = d.putCreatureOnBattlefield(p1, "Goblin Piker")
        d.putCreatureOnBattlefield(p1, "Raging Goblin")
        val changeling = d.putCreatureOnBattlefield(p1, "Woodland Changeling")
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val masterwork = d.putPermanentOnBattlefield(p1, "Stoneforge Masterwork")

        d.attach(masterwork, bears)
        withClue("only the changeling shares a type with the Bear") { d.pt(bears) shouldBe (3 to 3) }
        d.pt(piker) shouldBe (2 to 1)

        d.replaceState(d.state.updateEntity(bears) { it.without<AttachmentsComponent>() })
        d.attach(masterwork, changeling)
        withClue("the changeling shares a type with all three others") { d.pt(changeling) shouldBe (5 to 5) }
        d.pt(bears) shouldBe (2 to 2)
    }
})
