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
 * Dragon Shadow
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+0 and has fear.
 * When a creature with mana value 6 or greater enters, you may return Dragon Shadow
 * from your graveyard to the battlefield attached to that creature.
 */
val DragonShadow = card("Dragon Shadow") {
    manaCost = "{1}{B}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+0 and has fear. (It can't be blocked except by artifact creatures and/or black creatures.)\nWhen a creature with mana value 6 or greater enters, you may return Dragon Shadow from your graveyard to the battlefield attached to that creature."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 0, StaticTarget.AttachedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FEAR, StaticTarget.AttachedCreature)
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
        collectorNumber = "65"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ec35e03-022b-417c-9987-7379cf3956f9.jpg?1562525428"
    }
}
