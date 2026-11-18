package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ensoul Artifact — Magic 2015 #54 (canonical printing)
 * {1}{U} · Enchantment — Aura
 * Enchant artifact
 * Enchanted artifact is a creature with base power and toughness 5/5 in addition to its other types.
 *
 * Mightstone's Animation's shape: the creature type is added in layer 4, the base P/T set in
 * layer 7b, so pumps and counters still apply on top.
 */
val EnsoulArtifact = card("Ensoul Artifact") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "Enchanted artifact is a creature with base power and toughness 5/5 in addition to its other types."

    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    staticAbility {
        ability = GrantCardType("CREATURE", filter = GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = SetBasePowerToughnessStatic(5, 5)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Jasper Sandner"
        flavorText = "Too often, some self-described genius comes up with a \"labor-saving device\" that ends up creating far more work."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8351efc5-a392-4ec8-877f-15d5b3dc0929.jpg?1783939194"
        ruling(
            "2014-07-18",
            "The artifact retains any types, subtypes, or supertypes it has. Notably, if an Equipment " +
                "becomes an artifact creature, it usually can't be attached to another creature. If it " +
                "was attached to a creature, it becomes unattached."
        )
        ruling(
            "2014-07-18",
            "If the artifact was already a creature, its base power and toughness will each become 5. " +
                "This overwrites any previous effects that set the creature's base power and toughness " +
                "to specific values. Any power- or toughness-setting effects that start to apply after " +
                "Ensoul Artifact becomes attached to the artifact will overwrite this effect."
        )
    }
}
