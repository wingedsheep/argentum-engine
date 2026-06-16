package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bewitching Leechcraft
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature has "If this creature would untap during your untap step,
 * remove a +1/+1 counter from it instead. If you do, untap it." (Otherwise, it
 * doesn't untap.)
 *
 * Modeled with [AbilityFlag.REMOVE_COUNTER_TO_UNTAP], a granted untap-step
 * replacement (CR 614, applied during the untap step CR 502): during the
 * enchanted creature's controller's untap step the engine tries to remove a
 * +1/+1 counter; the creature untaps only if a counter was removed, otherwise it
 * stays tapped. Granted via projection (a [GrantKeyword] static on the Aura) so
 * it disappears when the Aura leaves play.
 */
val BewitchingLeechcraft = card("Bewitching Leechcraft") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen this Aura enters, tap enchanted creature.\nEnchanted creature has \"If this creature would untap during your untap step, remove a +1/+1 counter from it instead. If you do, untap it.\" (Otherwise, it doesn't untap.)"

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.REMOVE_COUNTER_TO_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Wei Guan"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b612452e-b8a8-4a5a-82a8-01fd463cfc77.jpg?1686968013"
    }
}
