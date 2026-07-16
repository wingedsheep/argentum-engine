package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bramble Armor
 * {1}{G}
 * Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+1.
 * Equip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)
 *
 * Canonical printing is Innistrad: Midnight Hunt (earliest real set); Innistrad: Crimson Vow
 * gets a reprint row. Per Scryfall ruling: casting doesn't require a creature (the attach
 * trigger simply does nothing if there's no legal target), and if the target becomes illegal
 * before the trigger resolves, the Equipment stays on the battlefield unattached.
 */
val BrambleArmor = card("Bramble Armor") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +2/+1.\n" +
        "Equip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.youControl()))
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Alessandra Pisano"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/621b58aa-c809-4baf-b2c8-3a46969598d7.jpg?1783925585"
    }
}
