package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kraven's Last Hunt — Marvel's Spider-Man (SPM #105)
 * {3}{G} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Mill five cards. When you do, this Saga deals damage equal to the greatest power among
 *     creature cards in your graveyard to target creature.
 * II — Target creature you control gets +2/+2 until end of turn.
 * III — Return target creature card from your graveyard to your hand.
 *
 * Chapter I is a [ReflexiveTriggerEffect]: the mill is mandatory (`optional = false`), and "when
 * you do" queues a second, targeted ability that resolves after the mill. Choosing the target
 * only once the five cards have hit the graveyard is what lets the just-milled creatures count
 * toward "the greatest power among creature cards in your graveyard" — modeled with the
 * [DynamicAmounts.zone] MAX-power reducer over `GameObjectFilter.Creature` in your graveyard.
 * The Saga is the damage source (the reflexive effect's default source), matching "this Saga
 * deals damage."
 *
 * Chapter II is a plain end-of-turn [Effects.ModifyStats] pump on a creature you control.
 * Chapter III returns a targeted creature card from your graveyard to your hand
 * ([Effects.ReturnToHand]).
 */
val KravensLastHunt = card("Kraven's Last Hunt") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Mill five cards. When you do, this Saga deals damage equal to the greatest power among creature cards in your graveyard to target creature.\n" +
        "II — Target creature you control gets +2/+2 until end of turn.\n" +
        "III — Return target creature card from your graveyard to your hand."

    // I — Mill five cards. When you do, this Saga deals damage equal to the greatest power
    //     among creature cards in your graveyard to target creature.
    sagaChapter(1) {
        effect = ReflexiveTriggerEffect(
            action = Patterns.Library.mill(5),
            optional = false,
            reflexiveEffect = Effects.DealDamage(
                DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature).maxPower(),
                EffectTarget.ContextTarget(0)
            ),
            reflexiveTargetRequirements = listOf(Targets.Creature),
            descriptionOverride = "Mill five cards. When you do, this Saga deals damage equal to " +
                "the greatest power among creature cards in your graveyard to target creature."
        )
    }

    // II — Target creature you control gets +2/+2 until end of turn.
    sagaChapter(2) {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(2, 2, creature)
    }

    // III — Return target creature card from your graveyard to your hand.
    sagaChapter(3) {
        val creatureCard = target("target creature card from your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.ReturnToHand(creatureCard)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Bill Sienkiewicz"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0c18ffe-a2b9-40df-a6b4-a9381e6dc467.jpg?1783905327"
    }
}
