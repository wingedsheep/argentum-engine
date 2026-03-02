package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Breath
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has haste.
 * {R}: Enchanted creature gets +1/+0 until end of turn.
 * When a creature with mana value 6 or greater enters, you may return Dragon Breath
 * from your graveyard to the battlefield attached to that creature.
 */
val DragonBreath = card("Dragon Breath") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has haste.\n{R}: Enchanted creature gets +1/+0 until end of turn.\nWhen a creature with mana value 6 or greater enters, you may return Dragon Breath from your graveyard to the battlefield attached to that creature."

    auraTarget = Targets.Creature

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, StaticTarget.AttachedCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.EnchantedCreature)
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
        effect = MayEffect(
            effect = Effects.ReturnSelfToBattlefieldAttached(),
            description_override = "Attach Dragon Breath to this creature?",
            sourceRequiredZone = Zone.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1832aaed-e164-4f78-9bc9-ec6c015835f5.jpg?1562526058"
    }
}
