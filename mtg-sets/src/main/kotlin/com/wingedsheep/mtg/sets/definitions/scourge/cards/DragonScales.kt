package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Dragon Scales
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+2 and has vigilance.
 * When a creature with mana value 6 or greater enters, you may return Dragon Scales
 * from your graveyard to the battlefield attached to that creature.
 */
val DragonScales = card("Dragon Scales") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+2 and has vigilance.\nWhen a creature with mana value 6 or greater enters, you may return Dragon Scales from your graveyard to the battlefield attached to that creature."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 2, StaticTarget.AttachedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, StaticTarget.AttachedCreature)
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.manaValueAtLeast(6),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        triggerZone = Zone.GRAVEYARD
        effect = MayEffect(Effects.ReturnSelfToBattlefieldAttached())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e78b364-015d-4074-ad9e-55c973ce2f4b.jpg?1562532079"
    }
}
