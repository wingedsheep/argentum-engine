package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mightstone's Animation
 * {3}{U}
 * Enchantment — Aura
 * Enchant artifact
 * When this Aura enters, draw a card.
 * Enchanted artifact is a creature with base power and toughness 4/4 in addition to its other types.
 *
 * Zoetic Glyph's shape without the subtype: the creature type is added in layer 4, the base
 * P/T set in layer 7b, so pumps and counters still apply on top.
 */
val MightstonesAnimation = card("Mightstone's Animation") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted artifact is a creature with base power and toughness 4/4 in addition to its other types."

    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantCardType("CREATURE", filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = SetBasePowerToughnessStatic(4, 4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c540e42-22e2-4127-bbd9-2e9200023fec.jpg?1783920107"
        ruling(
            "2022-10-14",
            "The artifact retains any types, subtypes, or supertypes it has. Notably, an Equipment that " +
                "becomes an artifact creature usually can't be attached to another creature. If it was " +
                "attached to a creature, it becomes unattached."
        )
        ruling(
            "2022-10-14",
            "If the artifact was already a creature, its base power and toughness will each become 4. " +
                "This overwrites any previous effects that set the creature's base power and toughness " +
                "to specific values. Any power- or toughness-setting effects that start to apply after " +
                "Mightstone's Animation becomes attached to the artifact will overwrite this effect."
        )
    }
}
