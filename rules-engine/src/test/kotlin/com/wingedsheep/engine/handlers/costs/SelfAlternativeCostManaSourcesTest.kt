package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [SelfAlternativeCost.manaSources] — "You may pay {R}{G} rather than pay this spell's mana cost.
 * Spend only mana produced by Treasures to cast it this way." (Security Rhox).
 *
 * The restriction scopes *which mana* may pay the alternative (CR 601.2h — the player pays the
 * total cost in any order, but only with mana the spell allows): mana abilities of other sources
 * can't be used, and floating mana counts only when a matching source produced it. The normal
 * mana cost is untouched.
 */
class SelfAlternativeCostManaSourcesTest : FunSpec({

    val rhino = card("Treasure-Bought Rhino") {
        manaCost = "{2}{R}{G}"
        typeLine = "Creature — Rhino Warrior"
        power = 5
        toughness = 4
        selfAlternativeCost = SelfAlternativeCost(
            manaCost = ManaCost.parse("{R}{G}"),
            manaSources = GameObjectFilter.Artifact.withSubtype("Treasure"),
        )
    }
    val redLand = card("Restricted Red Land") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.RED, 1); manaAbility = true }
    }
    val greenLand = card("Restricted Green Land") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
    }
    val redRock = card("Restricted Red Rock") {
        typeLine = "Artifact"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.RED, 1); manaAbility = true }
    }
    val greenRock = card("Restricted Green Rock") {
        typeLine = "Artifact"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
    }
    val treasureAbility = PredefinedTokens.Treasure.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(rhino, redLand, greenLand, redRock, greenRock, PredefinedTokens.Treasure))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun altCast(player: EntityId, card: EntityId, strategy: PaymentStrategy = PaymentStrategy.AutoPay) = CastSpell(
        player, card, useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE, paymentStrategy = strategy,
    )

    fun altOffered(d: GameTestDriver, player: EntityId, card: EntityId): Boolean = d.legalActions(player).any {
        val a = it.action
        a is CastSpell && a.cardId == card && a.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
    }

    fun normalOffered(d: GameTestDriver, player: EntityId, card: EntityId): Boolean = d.legalActions(player).any {
        val a = it.action
        a is CastSpell && a.cardId == card && !a.useAlternativeCost
    }

    fun pool(d: GameTestDriver, player: EntityId): ManaPoolComponent =
        d.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun crackTreasure(d: GameTestDriver, player: EntityId, color: Color) {
        val treasure = d.putPermanentOnBattlefield(player, "Treasure")
        d.submit(ActivateAbility(player, treasure, treasureAbility, manaColorChoice = color)).error shouldBe null
    }

    fun tap(d: GameTestDriver, player: EntityId, source: EntityId, def: com.wingedsheep.sdk.model.CardDefinition) {
        d.submit(ActivateAbility(player, source, def.activatedAbilities.single().id)).error shouldBe null
    }

    test("two Treasures pay the alternative cost: offered with the Treasures uncracked, cast from their floating mana") {
        val d = driver()
        val you = d.activePlayer!!
        val spell = d.putCardInHand(you, "Treasure-Bought Rhino")
        d.putPermanentOnBattlefield(you, "Treasure")
        d.putPermanentOnBattlefield(you, "Treasure")

        altOffered(d, you, spell) shouldBe true
        d.legalActions(you).first {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }.description shouldBe "Cast Treasure-Bought Rhino ({R}{G}, ${GameObjectFilter.Artifact.withSubtype("Treasure").description} mana only)"

        for (t in d.state.getBattlefield(you).filter { d.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Treasure" }) {
            val color = if (pool(d, you).red == 0) Color.RED else Color.GREEN
            d.submit(ActivateAbility(you, t, treasureAbility, manaColorChoice = color)).error shouldBe null
        }
        d.submit(altCast(you, spell, PaymentStrategy.FromPool)).error shouldBe null
        d.stackSize shouldBe 1
        pool(d, you).total shouldBe 0
    }

    test("lands alone never pay it, however many — but they still pay the normal mana cost") {
        val d = driver()
        val you = d.activePlayer!!
        val spell = d.putCardInHand(you, "Treasure-Bought Rhino")
        repeat(3) { d.putPermanentOnBattlefield(you, "Restricted Red Land") }
        repeat(3) { d.putPermanentOnBattlefield(you, "Restricted Green Land") }

        altOffered(d, you, spell) shouldBe false
        d.submit(altCast(you, spell)).error shouldNotBe null
        d.stackSize shouldBe 0

        normalOffered(d, you, spell) shouldBe true
        d.submit(CastSpell(you, spell)).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("a Treasure plus a land can't pay it: neither offered nor accepted, auto-pay or explicit") {
        val d = driver()
        val you = d.activePlayer!!
        val spell = d.putCardInHand(you, "Treasure-Bought Rhino")
        d.putPermanentOnBattlefield(you, "Treasure")
        val forest = d.putPermanentOnBattlefield(you, "Restricted Green Land")

        altOffered(d, you, spell) shouldBe false

        crackTreasure(d, you, Color.RED)
        d.submit(altCast(you, spell)).error shouldNotBe null
        d.submit(altCast(you, spell, PaymentStrategy.Explicit(listOf(forest)))).error shouldNotBe null
        d.stackSize shouldBe 0
        // The rejected casts left the Treasure's mana where it was.
        pool(d, you).red shouldBe 1
    }

    test("floating land mana can't pay it, floating Treasure mana can — and the land mana stays in the pool") {
        val d = driver()
        val you = d.activePlayer!!
        val spell = d.putCardInHand(you, "Treasure-Bought Rhino")
        val red = d.putPermanentOnBattlefield(you, "Restricted Red Land")
        val green = d.putPermanentOnBattlefield(you, "Restricted Green Land")
        tap(d, you, red, redLand)
        tap(d, you, green, greenLand)

        altOffered(d, you, spell) shouldBe false
        d.submit(altCast(you, spell, PaymentStrategy.FromPool)).error shouldNotBe null

        crackTreasure(d, you, Color.RED)
        crackTreasure(d, you, Color.GREEN)
        altOffered(d, you, spell) shouldBe true
        d.submit(altCast(you, spell, PaymentStrategy.FromPool)).error shouldBe null
        d.stackSize shouldBe 1

        // Only the Treasures' mana was spent; the lands' {R}{G} is still floating.
        val after = pool(d, you)
        after.red shouldBe 1
        after.green shouldBe 1
        after.manaUnits.map { it.source.sourceId }.toSet() shouldBe setOf(red, green)
    }

    test("a non-Treasure artifact mana source can't pay it, tapped live or floated first") {
        val d = driver()
        val you = d.activePlayer!!
        val spell = d.putCardInHand(you, "Treasure-Bought Rhino")
        val redRockId = d.putPermanentOnBattlefield(you, "Restricted Red Rock")
        val greenRockId = d.putPermanentOnBattlefield(you, "Restricted Green Rock")

        altOffered(d, you, spell) shouldBe false
        d.submit(altCast(you, spell)).error shouldNotBe null

        tap(d, you, redRockId, redRock)
        tap(d, you, greenRockId, greenRock)
        altOffered(d, you, spell) shouldBe false
        d.submit(altCast(you, spell, PaymentStrategy.FromPool)).error shouldNotBe null
        d.stackSize shouldBe 0
    }

    test("mana with no recorded source never pays the restricted alternative") {
        val d = driver()
        val you = d.activePlayer!!
        val spell = d.putCardInHand(you, "Treasure-Bought Rhino")
        d.giveMana(you, Color.RED, 2)
        d.giveMana(you, Color.GREEN, 2)

        altOffered(d, you, spell) shouldBe false
        d.submit(altCast(you, spell, PaymentStrategy.FromPool)).error shouldNotBe null

        // The same mana still pays the normal {2}{R}{G}.
        d.submit(CastSpell(you, spell, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("a sacrificed Treasure's mana still counts as Treasure mana once the token is gone") {
        val d = driver()
        val you = d.activePlayer!!
        val spell = d.putCardInHand(you, "Treasure-Bought Rhino")
        crackTreasure(d, you, Color.RED)
        crackTreasure(d, you, Color.GREEN)
        d.findPermanent(you, "Treasure") shouldBe null
        pool(d, you).manaUnits.size shouldBe 2

        val alt = d.legalActions(you).firstOrNull {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }
        alt.shouldNotBeNull()
        d.submit(altCast(you, spell)).error shouldBe null
        d.stackSize shouldBe 1
    }
})
