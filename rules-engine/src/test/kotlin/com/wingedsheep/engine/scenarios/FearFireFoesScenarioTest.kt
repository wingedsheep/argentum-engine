package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.FearFireFoes
import com.wingedsheep.mtg.sets.definitions.ons.cards.BattlefieldMedic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fear, Fire, Foes! — {X}{R} sorcery.
 *
 * "Damage can't be prevented this turn. Fear, Fire, Foes! deals X damage to target creature
 *  and 1 damage to each other creature with the same controller."
 *
 * Exercises the two new building blocks:
 *  - GroupFilter.excludeTarget + ControlledByReferencedPlayer(TargetController): the 1-damage
 *    sweep hits only OTHER creatures controlled by the target creature's controller.
 *  - DamageCantBePreventedThisTurnEffect: the turn-scoped prevention shutoff ignores a
 *    prevention shield set up by Battlefield Medic.
 */
class FearFireFoesScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(FearFireFoes)
        d.registerCard(BattlefieldMedic)
        return d
    }

    fun damageOn(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun named(d: GameTestDriver, creatures: List<EntityId>, name: String): EntityId =
        creatures.first { d.state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("X damage to target, 1 to each OTHER creature of the target's controller; other player's creatures untouched") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P2 (the target's controller) has the target plus two other creatures.
        val target = d.putCreatureOnBattlefield(p2, "Force of Nature")   // 5/5, the target
        d.putCreatureOnBattlefield(p2, "Centaur Courser")                 // 3/3, other p2 creature
        d.putCreatureOnBattlefield(p2, "Gurmag Angler")                   // 5/5, other p2 creature

        // P1 controls a creature that must NOT be hit (different controller).
        val p1Bystander = d.putCreatureOnBattlefield(p1, "Centaur Courser") // 3/3

        val p2Creatures = d.getCreatures(p2)
        val p2Centaur = named(d, p2Creatures, "Centaur Courser")
        val p2Angler = named(d, p2Creatures, "Gurmag Angler")

        // Cast Fear, Fire, Foes! with X=2 at the Force of Nature.
        d.giveColorlessMana(p1, 2)  // X
        d.giveMana(p1, Color.RED, 1)
        val spell = d.putCardInHand(p1, "Fear, Fire, Foes!")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(target)),
            xValue = 2
        ))
        if (!result.isSuccess) throw AssertionError("cast failed: ${result.error}")
        d.bothPass()

        // Target took X = 2.
        damageOn(d, target) shouldBe 2
        // Each OTHER creature with the same controller (P2) took 1.
        damageOn(d, p2Centaur) shouldBe 1
        damageOn(d, p2Angler) shouldBe 1
        // P1's creature (different controller) took nothing.
        damageOn(d, p1Bystander) shouldBe 0
    }

    test("damage can't be prevented this turn — a prevention shield is ignored") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Plains" to 20), startingLife = 20)
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P1 (the active player, who holds priority) controls the target (Force of Nature 5/5),
        // a Battlefield Medic (1 Cleric → shield of 1) and another sturdy creature to read the
        // 1-damage sweep on. The spell can target any creature.
        val target = d.putCreatureOnBattlefield(p1, "Force of Nature")
        val medic = d.putCreatureOnBattlefield(p1, "Battlefield Medic")
        val bystander = d.putCreatureOnBattlefield(p1, "Gurmag Angler") // 5/5, survives the sweep
        d.removeSummoningSickness(medic)

        // Put a "prevent the next 1 damage" shield on the Force of Nature.
        val medicAbilityId = BattlefieldMedic.activatedAbilities.first().id
        d.submit(ActivateAbility(
            playerId = p1,
            sourceId = medic,
            abilityId = medicAbilityId,
            targets = listOf(ChosenTarget.Permanent(target))
        )).isSuccess shouldBe true
        d.bothPass()

        // Sanity: the prevention shield is live.
        d.state.floatingEffects.any {
            it.effect.modification is com.wingedsheep.engine.mechanics.layers.SerializableModification.PreventNextDamage
        } shouldBe true

        // P1 casts Fear, Fire, Foes! X=3 at the Force of Nature. "Damage can't be prevented this
        // turn" shuts off the shield, so the full 3 lands (shield would otherwise reduce it to 2).
        d.giveColorlessMana(p1, 3)
        d.giveMana(p1, Color.RED, 1)
        val spell = d.putCardInHand(p1, "Fear, Fire, Foes!")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(target)),
            xValue = 3
        ))
        if (!result.isSuccess) throw AssertionError("cast failed: ${result.error}")
        d.bothPass()

        // Full 3 damage applied despite the prevent-1 shield (it would otherwise reduce it to 2).
        damageOn(d, target) shouldBe 3
        // The 1-damage sweep also can't be prevented: it hit each other creature P1 controls. The
        // 1/1 medic took 1 (lethal) and was destroyed; the 5/5 bystander shows 1 marked damage.
        d.findPermanent(p1, "Battlefield Medic") shouldBe null
        damageOn(d, bystander) shouldBe 1
        // The turn-scoped flag is set.
        d.state.damageCantBePreventedThisTurn shouldBe true
    }
})
