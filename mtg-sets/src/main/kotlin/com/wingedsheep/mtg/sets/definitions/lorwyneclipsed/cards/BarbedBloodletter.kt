package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Barbed Bloodletter
 * {1}{B}
 * Artifact — Equipment
 *
 * Flash
 * When this Equipment enters, attach it to target creature you control.
 * That creature gains wither until end of turn.
 * Equipped creature gets +1/+2.
 * Equip {2}
 */
val BarbedBloodletter = card("Barbed Bloodletter") {
    manaCost = "{1}{B}"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\nWhen this Equipment enters, attach it to target creature you control. That creature gains wither until end of turn. (It deals damage to creatures in the form of -1/-1 counters.)\nEquipped creature gets +1/+2.\nEquip {2}"

    keywords(Keyword.FLASH)

    // ETB: attach to target creature you control, grant wither until end of turn
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
            .then(Effects.GrantKeyword(Keyword.WITHER, creature))
    }

    // Equipped creature gets +1/+2
    staticAbility {
        effect = Effects.ModifyStats(+1, +2)
        filter = Filters.EquippedCreature
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8ff1a13c-e338-42e9-8eb3-b303fabd67de.jpg?1767957075"
        ruling("2025-11-17", "Wither applies to any damage dealt to creatures by the affected creature. This includes combat damage as well as anything that causes that creature to deal noncombat damage, such as the effect of Assert Perfection.")
    }
}
