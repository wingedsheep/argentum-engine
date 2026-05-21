package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stop Cold
 * {3}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant artifact or creature
 * When this Aura enters, tap enchanted permanent.
 * Enchanted permanent loses all abilities and doesn't untap during its controller's untap step.
 */
val StopCold = card("Stop Cold") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant artifact or creature\nWhen this Aura enters, tap enchanted permanent.\nEnchanted permanent loses all abilities and doesn't untap during its controller's untap step."

    keywords(Keyword.FLASH)

    auraTarget = Targets.CreatureOrArtifact

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9ff9c158-0080-427c-8cf9-0289011ea63e.jpg?1712355518"
        ruling("2024-04-12", "If the affected permanent gains an ability after Stop Cold becomes attached to it, it will keep that ability.")
    }
}
