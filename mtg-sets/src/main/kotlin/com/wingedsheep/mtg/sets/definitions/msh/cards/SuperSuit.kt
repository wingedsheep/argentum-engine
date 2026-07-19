package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Super Suit
 * {1}{U}
 * Artifact — Equipment
 *
 * Flash
 * When this Equipment enters, attach it to target creature you control. Untap that creature.
 * Equipped creature gets +1/+2.
 * Equip {2}
 *
 * The ETB is the standard Sovereign's Macuahuitl / Malamet Scythe idiom — an
 * [Effects.AttachEquipment] on a "target creature you control" — with the untap chained onto the
 * *same* bound target so "that creature" always refers to the attach target (and still untaps if
 * the attach is redundant). Stats and the equip ability are the canonical Equipment primitives.
 */
val SuperSuit = card("Super Suit") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, attach it to target creature you control. Untap that creature.\n" +
        "Equipped creature gets +1/+2.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.Creature.youControl())
        )
        effect = Effects.AttachEquipment(creature) then Effects.Untap(creature)
    }

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Smirtouille"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e87f3a26-c2e5-47c7-b19d-bc01cb794805.jpg?1783902950"
    }
}
