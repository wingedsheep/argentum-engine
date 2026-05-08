package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Temur Ascendancy
 * {G}{U}{R}
 * Enchantment
 * Creatures you control have haste.
 * Whenever a creature with power 4 or greater enters the battlefield under your control,
 * you may draw a card.
 */
val TemurAscendancy = card("Temur Ascendancy") {
    manaCost = "{G}{U}{R}"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have haste.\nWhenever a creature with power 4 or greater enters under your control, you may draw a card."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerAtLeast(4),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = MayEffect(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11746bf1-d813-4ade-8ce4-9935cebef856.jpg?1562782711"
    }
}
