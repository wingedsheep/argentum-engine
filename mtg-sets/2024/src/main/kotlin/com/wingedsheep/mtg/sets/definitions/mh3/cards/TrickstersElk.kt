package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent

/**
 * Trickster's Elk
 * {2}{G}
 * Enchantment Creature — Elk
 * 3/3
 *
 * Bestow {1}{G}
 * Enchanted creature loses all abilities and is a green Elk creature with base power and
 * toughness 3/3.
 *
 * The Witness Protection shape on a bestow body: three statics over the enchanted creature
 * (all default to `GroupFilter.attachedCreature()`, so they're inert while the Elk is an
 * unattached creature):
 *  - [TransformPermanent] — Layer 4 replaces every card type with Creature (supertypes such as
 *    legendary survive, CR 205.1a / 205.4b) and every subtype with Elk; Layer 5 sets green.
 *  - [LoseAllAbilities] — Layer 6, timestamped, so abilities granted later are kept.
 *  - [SetBasePowerToughnessStatic] 3/3 — Layer 7b; counters and +N/+N still apply on top.
 */
val TrickstersElk = card("Trickster's Elk") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Elk"
    power = 3
    toughness = 3
    oracleText = "Bestow {1}{G} (If you cast this card for its bestow cost, it's an Aura spell " +
        "with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Enchanted creature loses all abilities and is a green Elk creature with base power and " +
        "toughness 3/3."

    keywordAbility(KeywordAbility.bestow("{1}{G}"))

    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("CREATURE"),
            setSubtypes = setOf(Subtype.ELK.value),
            setColors = setOf(Color.GREEN)
        )
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = SetBasePowerToughnessStatic(3, 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Karl Kopinski"
        flavorText = "\"Wait. Is *that* one Dad?\"\n—Rowan Kenrith, to Will"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/812af562-853a-48a3-b428-ad891c54be5c.jpg?1783911255"
        ruling("2024-06-07", "If the enchanted creature gains an ability after Trickster's Elk " +
            "becomes attached to it, it will keep that ability.")
        ruling("2024-06-07", "Trickster's Elk overwrites all colors and creature types the " +
            "enchanted creature has. It's just a green Elk. The creature keeps any supertypes " +
            "(such as legendary) it has but loses any other card types it has (such as artifact).")
        ruling("2024-06-07", "Trickster's Elk overwrites all previous effects that set the " +
            "creature's base power and toughness to specific values. Any power- or " +
            "toughness-setting effects that start to apply afterward will overwrite this effect.")
        ruling("2024-06-07", "Trickster's Elk may enchant a permanent that is only temporarily a " +
            "creature, such as a Vehicle. If this happens, Trickster's Elk's effect causes the " +
            "enchanted permanent to remain a 3/3 green Elk creature even after the temporary " +
            "effect making it a creature expires.")
    }
}
