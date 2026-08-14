package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.dsl.teamworkModal
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Murdock's Crusade — Marvel Super Heroes #24
 * {1}{W} · Sorcery · Common
 *
 * Teamwork 4 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 4 or more.)
 * Choose one. If this spell was cast using teamwork, choose both instead.
 * • Street Justice — Exile target creature with toughness 4 or greater.
 * • Legal Justice — Exile target enchantment with mana value 4 or greater.
 *
 * The modal shape of teamwork — [teamworkModal] narrows the printed "choose both" to one mode
 * unless the teamwork cost was declared. CR 700.2 governs the mode count; the declaration it
 * branches on is made under CR 601.2b (*not* CR 702.194c, which is about targets).
 *
 * The printed mode names are part of each mode's description, which is what the client shows on
 * the mode buttons.
 *
 * Both restrictions are ordinary target filters — toughness is read from **projected** state, so a
 * creature pumped to toughness 4 is a legal target and one shrunk below 4 stops being one; mana
 * value is the enchantment card's own, unaffected by cost reduction.
 */
val MurdocksCrusade = card("Murdock's Crusade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Teamwork 4 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 4 or more.)\n" +
        "Choose one. If this spell was cast using teamwork, choose both instead.\n" +
        "• Street Justice — Exile target creature with toughness 4 or greater.\n" +
        "• Legal Justice — Exile target enchantment with mana value 4 or greater."

    teamwork(4)

    spell {
        teamworkModal {
            mode("Street Justice — Exile target creature with toughness 4 or greater") {
                val creature = target(
                    "target creature with toughness 4 or greater",
                    TargetCreature(filter = TargetFilter.Creature.toughnessAtLeast(4)),
                )
                effect = Effects.Exile(creature)
            }
            mode("Legal Justice — Exile target enchantment with mana value 4 or greater") {
                val enchantment = target(
                    "target enchantment with mana value 4 or greater",
                    TargetPermanent(filter = TargetFilter.Enchantment.manaValueAtLeast(4)),
                )
                effect = Effects.Exile(enchantment)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Gal Or"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98df64ca-39c3-47e6-8143-4106c8e9cf59.jpg?1783902970"
    }
}
