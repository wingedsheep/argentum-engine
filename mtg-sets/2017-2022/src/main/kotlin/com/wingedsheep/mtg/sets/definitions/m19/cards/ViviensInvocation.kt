package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vivien's Invocation — Core Set 2019 #209 (canonical printing)
 * {5}{G}{G} · Sorcery
 *
 * Look at the top seven cards of your library. You may put a creature card from among them onto
 * the battlefield. Put the rest on the bottom of your library in a random order. When a creature
 * is put onto the battlefield this way, it deals damage equal to its power to target creature an
 * opponent controls.
 *
 * The spell takes no targets. "When a creature is put onto the battlefield this way" is a
 * reflexive trigger (CR 603.12), gated on the move actually happening (`moveTracked` records what
 * landed), whose target is chosen as it goes on the stack. The creature is both the damage source
 * and the power reference — a pipeline target, read live while it is on the battlefield and by
 * last-known information once it has left (2018-07-13 ruling).
 */
val ViviensInvocation = card("Vivien's Invocation") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top seven cards of your library. You may put a creature card from among " +
        "them onto the battlefield. Put the rest on the bottom of your library in a random order. When " +
        "a creature is put onto the battlefield this way, it deals damage equal to its power to target " +
        "creature an opponent controls."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(7)))
            val (chosen, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card from among them onto the battlefield",
                showAllCards = true,
            )
            val invoked = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD), name = "vivienInvoked")
            move(
                rest,
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random,
            )
            run(Effects.If(
                condition = Conditions.CollectionContainsMatch(invoked),
                then = Effects.ReflexiveTrigger(
                    action = Effects.Nothing,
                    optional = false,
                    descriptionOverride = "It deals damage equal to its power to target creature an " +
                        "opponent controls.",
                ) {
                    val victim = target(TargetFilter.Creature.opponentControls())
                    effect = Effects.DealDamage(
                        DynamicAmounts.powerOf(invoked.asTarget),
                        victim,
                        damageSource = invoked.asTarget,
                    )
                },
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/784c4711-223d-4b95-a163-87e57f87b8db.jpg?1783934524"
        ruling("2018-07-13", "You don't choose any targets as you cast Vivien's Invocation. While that spell is resolving, you may put a creature card from among the cards you look at onto the battlefield. If you do, a separate ability triggers and you pick a target creature an opponent controls to be dealt damage.")
        ruling("2018-07-13", "If the creature you put onto the battlefield leaves the battlefield before damage is dealt, its last known existence on the battlefield is used to determine how much damage it deals to the target creature.")
    }
}
