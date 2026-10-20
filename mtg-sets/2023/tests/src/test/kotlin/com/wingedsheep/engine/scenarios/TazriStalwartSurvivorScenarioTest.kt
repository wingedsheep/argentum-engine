package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tazri, Stalwart Survivor — "Each creature you control has '{T}: Add one mana of any of this
 * creature's colors. Spend this mana only to activate an ability of a creature. Activate only if
 * this creature has another activated ability.' {W}{U}{B}{R}{G}, {T}: Mill five cards. Put all
 * creature cards with activated abilities that aren't mana abilities from among the milled cards
 * into your hand."
 */
class TazriStalwartSurvivorScenarioTest : ScenarioTestBase() {

    private val grantedManaId: AbilityId
        get() = (cardRegistry.requireCard("Tazri, Stalwart Survivor").staticAbilities.single() as GrantActivatedAbility).ability.id

    private fun board(vararg creatures: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Tazri, Stalwart Survivor")
        .apply { creatures.forEach { withCardOnBattlefield(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    private fun TestGame.pool(): ManaPoolComponent = state.getEntity(player1Id)!!.get<ManaPoolComponent>()!!

    init {
        test("a creature with no other activated ability can't use the granted mana ability") {
            val g = board("Grizzly Bears").build()
            val bears = g.findPermanent("Grizzly Bears")!!

            g.execute(ActivateAbility(g.player1Id, bears, grantedManaId, manaColorChoice = Color.GREEN)).error shouldNotBe null
        }

        test("cycling is another activated ability, even though it can't be activated on the battlefield") {
            val g = board("Disciple of Grace").build()
            val disciple = g.findPermanent("Disciple of Grace")!!

            g.execute(ActivateAbility(g.player1Id, disciple, grantedManaId, manaColorChoice = Color.WHITE)).error shouldBe null
            g.pool().restrictedMana.size shouldBe 1
        }

        test("Tazri herself qualifies through her mill ability") {
            val g = board().build()
            val tazri = g.findPermanent("Tazri, Stalwart Survivor")!!

            g.execute(ActivateAbility(g.player1Id, tazri, grantedManaId, manaColorChoice = Color.WHITE)).error shouldBe null
            g.pool().restrictedMana.size shouldBe 1
        }

        test("the mana pays a creature's ability but not a spell") {
            val g = board("Shivan Dragon")
                .withCardInHand(1, "Lightning Bolt")
                .build()
            val dragon = g.findPermanent("Shivan Dragon")!!
            g.execute(ActivateAbility(g.player1Id, dragon, grantedManaId, manaColorChoice = Color.RED)).error shouldBe null

            g.castSpell(1, "Lightning Bolt", g.player2Id).error shouldNotBe null

            val firebreathing = cardRegistry.requireCard("Shivan Dragon").activatedAbilities.single().id
            g.execute(
                ActivateAbility(g.player1Id, dragon, firebreathing, paymentStrategy = PaymentStrategy.FromPool)
            ).error shouldBe null
            g.pool().restrictedMana.size shouldBe 0
        }

        test("the mana can't pay a creature card's cycling from hand") {
            val g = board("Disciple of Grace", "Shivan Dragon")
                .withCardInHand(1, "Disciple of Grace")
                .build()
            g.execute(ActivateAbility(g.player1Id, g.findPermanent("Disciple of Grace")!!, grantedManaId, manaColorChoice = Color.WHITE)).error shouldBe null
            g.execute(ActivateAbility(g.player1Id, g.findPermanent("Shivan Dragon")!!, grantedManaId, manaColorChoice = Color.RED)).error shouldBe null
            g.pool().restrictedMana.size shouldBe 2

            val inHand = g.findCardsInHand(1, "Disciple of Grace").single()
            g.execute(CycleCard(g.player1Id, inHand, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
            g.isInHand(1, "Disciple of Grace") shouldBe true
            g.pool().restrictedMana.size shouldBe 2
        }

        test("mills five and returns only creature cards with non-mana activated abilities") {
            val g = board()
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Island")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Mountain")
                .withCardOnBattlefield(1, "Forest")
                .withCardInLibrary(1, "Shivan Dragon")
                .withCardInLibrary(1, "Llanowar Elves")
                .withCardInLibrary(1, "Disciple of Grace")
                .withCardInLibrary(1, "Icy Manipulator")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .build()
            val tazri = g.findPermanent("Tazri, Stalwart Survivor")!!
            val mill = cardRegistry.requireCard("Tazri, Stalwart Survivor").activatedAbilities.single().id

            g.execute(ActivateAbility(g.player1Id, tazri, mill)).error shouldBe null
            g.resolveStack()

            g.state.getLibrary(g.player1Id).size shouldBe 1
            g.isInHand(1, "Shivan Dragon") shouldBe true
            g.isInHand(1, "Disciple of Grace") shouldBe true
            g.findCardsInGraveyard(1, "Llanowar Elves").size shouldBe 1
            g.findCardsInGraveyard(1, "Icy Manipulator").size shouldBe 1
            g.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
        }
    }
}
