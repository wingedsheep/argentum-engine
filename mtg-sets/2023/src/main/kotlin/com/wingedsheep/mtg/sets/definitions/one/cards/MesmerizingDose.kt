package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mesmerizing Dose — Phyrexia: All Will Be One #62
 * {1}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature, then proliferate.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Mystic Restraints' enter-tap trigger and `DOESNT_UNTAP` grant, with a proliferate rider.
 */
val MesmerizingDose = card("Mesmerizing Dose") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature, then proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature) then Effects.Proliferate()
        description = "When this Aura enters, tap enchanted creature, then proliferate."
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dac28e27-78aa-48b1-96fa-edca1fbcfb77.jpg?1783918060"
    }
}
