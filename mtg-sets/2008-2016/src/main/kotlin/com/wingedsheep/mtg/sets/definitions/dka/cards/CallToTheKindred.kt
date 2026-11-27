package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Call to the Kindred
 * {3}{U}
 * Enchantment — Aura
 * Enchant creature
 * At the beginning of your upkeep, you may look at the top five cards of your library. If you do,
 * you may put a creature card that shares a creature type with enchanted creature from among them
 * onto the battlefield, then you put the rest of those cards on the bottom of your library in any
 * order.
 *
 * The "you may look … If you do" is the trigger's optional gate (declining leaves the library
 * untouched). All five looked-at cards are shown; only creature cards sharing a creature type with
 * the enchanted creature are selectable, and choosing none is allowed ("you may put"). The rest go
 * to the bottom in an order the controller chooses. "Enchanted creature" is read at resolution, off
 * the Aura's host (last-known if the Aura has left).
 */
val CallToTheKindred = card("Call to the Kindred") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "At the beginning of your upkeep, you may look at the top five cards of your library. If you do, " +
        "you may put a creature card that shares a creature type with enchanted creature from among them " +
        "onto the battlefield, then you put the rest of those cards on the bottom of your library in any order."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.EnchantedCreature),
            prompt = "You may put a creature card that shares a creature type with enchanted creature onto the battlefield",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(1)),
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee35f96c-6060-4456-897e-27b74c8c2137.jpg?1783940846"
        ruling(
            "2011-01-22",
            "The creature need only share one creature type with enchanted creature, not necessarily all of " +
                "them. For example, if the enchanted creature was a Human Soldier, you could put a Human Wizard " +
                "creature card, a Goblin Soldier creature card, or a Human Soldier Ally creature card onto the battlefield."
        )
    }
}
