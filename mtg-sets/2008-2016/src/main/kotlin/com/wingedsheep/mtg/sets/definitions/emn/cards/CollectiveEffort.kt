package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Collective Effort — Eldritch Moon #17.
 *
 * Collective Brutality's non-mana escalate shape, with a tap cost instead of a discard:
 * [com.wingedsheep.sdk.scripting.effects.ModalEffect.additionalCostPerExtraMode] scales
 * `TapPermanents(1, Creature)` to one untapped creature per mode beyond the first. The tap is not
 * a {T} cost, so summoning-sick creatures can pay it (2016-07-13 ruling).
 *
 * Mode three is Contagion Engine's board-wide counter over the creatures the mode's target player
 * controls as the spell resolves.
 */
val CollectiveEffort = card("Collective Effort") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Escalate—Tap an untapped creature you control. (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or more —\n" +
        "• Destroy target creature with power 4 or greater.\n" +
        "• Destroy target enchantment.\n" +
        "• Put a +1/+1 counter on each creature target player controls."

    spell {
        modal(
            chooseCount = 3,
            minChooseCount = 1,
            additionalCostPerExtraMode = CostAtom.TapPermanents(1, GameObjectFilter.Creature),
        ) {
            mode("Destroy target creature with power 4 or greater.") {
                val creature = target(TargetFilter.Creature.powerAtLeast(4))
                effect = Effects.Destroy(creature)
            }
            mode("Destroy target enchantment.") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
            mode("Put a +1/+1 counter on each creature target player controls.") {
                val player = target(Targets.Player)
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d85a6369-c07f-47d5-8448-72d8ec7e7898.jpg?1783937522"
        ruling("2016-07-13", "You choose all of your modes at once. You can't wait to perform one mode's actions and then decide to choose more modes.")
        ruling("2016-07-13", "Effects that reduce the cost of spells reduce the total cost, including any escalate costs added.")
        ruling("2016-07-13", "You can tap any untapped creature you control to pay the escalate cost, including one you haven't controlled continuously since the beginning of the turn.")
        ruling("2016-07-13", "If an effect allows you to cast a spell that has escalate without paying its mana cost, you pay escalate costs for that spell if you choose more than one mode.")
        ruling("2016-07-13", "If two of the chosen modes of an escalate spell target a creature, you may choose the same creature for each mode's target, or choose different creatures. The same is true if the chosen modes target a player (or opponent).")
        ruling("2016-07-13", "You can't choose any one mode multiple times.")
        ruling("2016-07-13", "If one target of an escalate spell becomes illegal, the other targets will still be affected. If all of the targets become illegal, the spell won't resolve.")
    }
}
