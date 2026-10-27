package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ala.cards.BoneSplinters
import com.wingedsheep.mtg.sets.definitions.atq.cards.AshnodsAltar
import com.wingedsheep.mtg.sets.definitions.avr.cards.AngelOfJubilation
import com.wingedsheep.mtg.sets.definitions.avr.cards.Griselbrand
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Angel of Jubilation (AVR #2) — Flying; "Other nonblack creatures you control get +1/+1";
 * "Players can't pay life or sacrifice creatures to cast spells or activate abilities."
 *
 * The prohibitions' edge cases are pinned in the engine's `PlayersCantPayLifeForCostsTest` and
 * `SacrificeForCostsProhibitionTest`; this covers the card as printed against the rulings' own
 * examples (Griselbrand's life cost, a sacrifice-a-creature additional cost, a sacrifice mana ability).
 */
class AngelOfJubilationScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AngelOfJubilation, Griselbrand, BoneSplinters, AshnodsAltar))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.power(id: EntityId) = state.projectedState.getPower(id)
    fun GameTestDriver.toughness(id: EntityId) = state.projectedState.getToughness(id)
    fun GameTestDriver.canUse(player: EntityId, sourceId: EntityId) =
        legalActions(player).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.affordable }

    test("other nonblack creatures you control get +1/+1 — not the Angel, black creatures, or an opponent's") {
        val d = driver()
        val angel = d.putCreatureOnBattlefield(d.player1, "Angel of Jubilation")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val black = d.putCreatureOnBattlefield(d.player1, "Black Creature")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        withClue("the Angel doesn't pump itself") { (d.power(angel) to d.toughness(angel)) shouldBe (3 to 3) }
        withClue("a 1/1 nonblack creature becomes 2/2") { (d.power(lions) to d.toughness(lions)) shouldBe (2 to 2) }
        withClue("black creatures are left out") { (d.power(black) to d.toughness(black)) shouldBe (2 to 2) }
        withClue("only creatures you control") { (d.power(theirs) to d.toughness(theirs)) shouldBe (3 to 3) }
    }

    test("no player can pay life to activate an ability — Griselbrand") {
        val d = driver()
        val owner = d.player1
        val gris = d.putCreatureOnBattlefield(owner, "Griselbrand")
        val abilityId = Griselbrand.activatedAbilities.single().id
        withClue("activatable before the Angel arrives") { d.canUse(owner, gris) shouldBe true }

        d.putCreatureOnBattlefield(d.getOpponent(owner), "Angel of Jubilation")
        d.canUse(owner, gris) shouldBe false
        d.submit(ActivateAbility(playerId = owner, sourceId = gris, abilityId = abilityId)).error shouldNotBe null
        d.getLifeTotal(owner) shouldBe 20
    }

    test("no player can sacrifice a creature to cast a spell — Bone Splinters") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, "Angel of Jubilation")
        d.putLandOnBattlefield(d.player1, "Swamp")
        val fodder = d.putCreatureOnBattlefield(d.player1, "Black Creature")
        val target = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val splinters = d.putCardInHand(d.player1, "Bone Splinters")

        d.legalActions(d.player1).any { (it.action as? CastSpell)?.cardId == splinters && it.affordable } shouldBe false
        d.submit(
            CastSpell(
                playerId = d.player1, cardId = splinters,
                targets = listOf(ChosenTarget.Permanent(target)),
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        ).error shouldNotBe null
        d.findPermanent(d.player1, "Black Creature") shouldBe fodder
    }

    test("no player can sacrifice a creature to a mana ability — Ashnod's Altar") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Angel of Jubilation")
        val altar = d.putPermanentOnBattlefield(d.player1, "Ashnod's Altar")
        val fodder = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val abilityId = AshnodsAltar.activatedAbilities.single().id

        d.canUse(d.player1, altar) shouldBe false
        d.submit(
            ActivateAbility(
                playerId = d.player1, sourceId = altar, abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        ).error shouldNotBe null
        withClue("not even the Angel itself") {
            d.submit(
                ActivateAbility(
                    playerId = d.player1, sourceId = altar, abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(d.findPermanent(d.player1, "Angel of Jubilation")!!)),
                )
            ).error shouldNotBe null
        }
        d.findPermanent(d.player1, "Savannah Lions") shouldBe fodder
    }

    test("once the Angel leaves, both costs can be paid again") {
        val d = driver()
        val angel = d.putCreatureOnBattlefield(d.player1, "Angel of Jubilation")
        val altar = d.putPermanentOnBattlefield(d.player1, "Ashnod's Altar")
        val fodder = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val gris = d.putCreatureOnBattlefield(d.player1, "Griselbrand")
        d.canUse(d.player1, altar) shouldBe false
        d.canUse(d.player1, gris) shouldBe false

        d.moveToGraveyard(angel)
        d.canUse(d.player1, altar) shouldBe true
        withClue("Griselbrand's life cost is payable again") { d.canUse(d.player1, gris) shouldBe true }
        d.submit(
            ActivateAbility(
                playerId = d.player1, sourceId = altar, abilityId = AshnodsAltar.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            )
        ).error shouldBe null
        d.getGraveyard(d.player1).contains(fodder) shouldBe true
        d.state.getEntity(d.player1)!!
            .get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!.colorless shouldBe 2
    }
})
