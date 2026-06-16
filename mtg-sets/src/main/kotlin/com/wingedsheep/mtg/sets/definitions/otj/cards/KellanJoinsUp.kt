package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MakePlottedEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kellan Joins Up
 * {G}{W}{U}
 * Legendary Enchantment
 *
 * When Kellan Joins Up enters, you may exile a nonland card with mana value 3 or less from
 * your hand. If you do, it becomes plotted.
 * Whenever a legendary creature you control enters, put a +1/+1 counter on each creature you
 * control.
 *
 * Part of the OTJ "Joins Up" cycle of Legendary Enchantments. The ETB is the same
 * gather → choose-up-to → exile → plot pipeline used by Make Your Own Luck, but sourced from
 * hand and filtered to nonland cards with mana value ≤ 3. `ChooseUpTo(1)` is the optional
 * "you may exile" fork; [MakePlottedEffect] (CR 718) no-ops on an empty selection, so declining
 * is safe. The legendary-enters trigger distributes a +1/+1 counter over every creature you
 * control via `ForEachInGroup` + `AddCounters(Self)`.
 */
val KellanJoinsUp = card("Kellan Joins Up") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GUW"
    typeLine = "Legendary Enchantment"
    oracleText = "When Kellan Joins Up enters, you may exile a nonland card with mana value 3 or less from your hand. If you do, it becomes plotted. (You may cast it as a sorcery on a later turn without paying its mana cost.)\n" +
        "Whenever a legendary creature you control enters, put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "kju_hand"
            ),
            SelectFromCollectionEffect(
                from = "kju_hand",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Nonland.manaValueAtMost(3),
                storeSelected = "kju_toPlot",
                selectedLabel = "Exile and plot"
            ),
            MoveCollectionEffect(from = "kju_toPlot", destination = CardDestination.ToZone(Zone.EXILE)),
            MakePlottedEffect(from = "kju_toPlot")
        )
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.legendary().youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e7f95d5-b279-4469-9c89-1e02630d61e6.jpg?1712356126"
    }
}
