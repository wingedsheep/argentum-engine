package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Garruk's Uprising
 * {2}{G}
 * Enchantment
 *
 * When this enchantment enters, if you control a creature with power 4 or greater, draw a card.
 * Creatures you control have trample.
 * Whenever a creature you control with power 4 or greater enters, draw a card.
 */
val GarruksUprising = card("Garruk's Uprising") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, if you control a creature with power 4 or greater, draw a card.\n" +
        "Creatures you control have trample. (Each of those creatures can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "Whenever a creature you control with power 4 or greater enters, draw a card."

    // ETB intervening-if: draw a card if you already control a power-4+ creature
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4))
        effect = Effects.DrawCards(1)
    }

    // Creatures you control have trample.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    // Whenever a creature you control with power 4 or greater enters, draw a card.
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerAtLeast(4),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71a4860a-8bb6-45c0-b00a-b4a42da33ab9.jpg?1594737017"
        ruling("2024-11-08", "The first ability of Garruk's Uprising has you draw just one card, no matter how many creatures you control with power 4 or greater.")
        ruling("2024-11-08", "If one or more static abilities that apply to a creature entering change its power, those abilities are considered when determining whether Garruk's Uprising's last ability triggers. The same is true for replacement effects that apply to it, such as entering with one or more +1/+1 counters or entering as a copy of another creature.")
        ruling("2024-11-08", "If you don't control a creature with power 4 or greater immediately after Garruk's Uprising enters, its first ability won't trigger. If you don't control one as the ability resolves, you don't draw a card. They don't have to be the same creature both times, however.")
        ruling("2024-11-08", "Once the last ability of Garruk's Uprising has triggered, lowering the power of the creature or removing it from the battlefield won't stop you from drawing a card.")
    }
}
