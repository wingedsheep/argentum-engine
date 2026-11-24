package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Righteous Authority
 * {3}{W}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +1/+1 for each card in its controller's hand.
 * At the beginning of the draw step of enchanted creature's controller, that player draws an
 * additional card.
 *
 * "Its controller" is the enchanted creature's controller, so the hand count reads
 * [Player.ControllerOfAffectedEntity] (as Death's Approach does). The draw trigger is Paralyze's
 * ATTACHED-bound step trigger with Kami of the Crescent Moon's extra draw: the enchanted creature's
 * controller is bound as the triggering player.
 */
val RighteousAuthority = card("Righteous Authority") {
    manaCost = "{3}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 for each card in its controller's hand.\n" +
        "At the beginning of the draw step of enchanted creature's controller, that player draws an additional card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        val cardsInHand = DynamicAmounts.count(Player.ControllerOfAffectedEntity, Zone.HAND)
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = cardsInHand,
            toughnessBonus = cardsInHand
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "At the beginning of the draw step of enchanted creature's controller, that player draws an additional card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Scott Chou"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6695e5bb-56a9-49ab-8940-72336e845875.jpg?1783940334"
        ruling("2013-04-15", "The triggered ability is put onto the stack after you have already drawn your card for the turn.")
    }
}
