package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Silken Strength — Aetherdrift #180
 * {1}{G} · Enchantment — Aura
 *
 * Flash
 * Enchant creature or Vehicle
 * When this Aura enters, untap enchanted permanent.
 * Enchanted permanent gets +1/+2 and has reach.
 *
 * The Aura attaches to a creature or Vehicle ([GameObjectFilter.CreatureOrVehicle], a Vehicle
 * matched by its subtype). The static +1/+2 and reach apply to the enchanted permanent via the
 * usual Aura static-ability auto-targeting (as on Armadillo Cloak). The one-shot "untap enchanted
 * permanent" is an enters-the-battlefield trigger pointed at [EffectTarget.EnchantedPermanent], so
 * it works whether the host is a creature or a (non-creature) Vehicle.
 */
val SilkenStrength = card("Silken Strength") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature or Vehicle\n" +
        "When this Aura enters, untap enchanted permanent.\n" +
        "Enchanted permanent gets +1/+2 and has reach."

    keywords(Keyword.FLASH)

    auraTarget = TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Untap(EffectTarget.EnchantedPermanent)
        description = "When this Aura enters, untap enchanted permanent."
    }

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce0e1ded-9b00-4d7b-884c-70a429783b1f.jpg?1782687820"
    }
}
