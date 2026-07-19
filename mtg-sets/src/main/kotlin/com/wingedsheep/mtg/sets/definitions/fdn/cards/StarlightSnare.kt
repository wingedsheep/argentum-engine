package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Starlight Snare
 * {2}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Functionally a Claustrophobia variant (cf. Charmed Sleep): the ETB taps the enchanted
 * creature, and the DOESNT_UNTAP static keeps it tapped through its controller's untap step.
 */
val StarlightSnare = card("Starlight Snare") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, tap enchanted creature.\nEnchanted creature doesn't untap during its controller's untap step."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "514"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74fb19b2-4f6c-4cbd-8756-a7eb5c7c9ef6.jpg?1783908961"
    }
}
