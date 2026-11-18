package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hot Soup
 * {1} · Artifact — Equipment
 * Equipped creature can't be blocked.
 * Whenever equipped creature is dealt damage, destroy it.
 * Equip {3}
 *
 * "It" is the creature that was dealt damage — the trigger's damaged permanent
 * ([EffectTarget.TriggeringEntity]) — not whatever Hot Soup is attached to when the trigger
 * resolves, so the creature is still destroyed if Hot Soup is destroyed or moved in response.
 */
val HotSoup = card("Hot Soup") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature can't be blocked.\n" +
        "Whenever equipped creature is dealt damage, destroy it.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = CantBeBlocked(GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.isDealtDamage()
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "David Palumbo"
        flavorText = "\"Comin' through!\"\nDesigned by James Ernest"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc46f4b0-1992-4060-8f21-643698d476f7.jpg?1783939158"
    }
}
