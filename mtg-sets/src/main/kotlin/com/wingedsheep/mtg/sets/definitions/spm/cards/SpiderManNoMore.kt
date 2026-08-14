package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent

/**
 * Spider-Man No More
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature is a Citizen with base power and toughness 1/1. It has defender and loses
 * all other abilities. (It also loses all other creature types.)
 *
 * Modeled as a stack of statics on the enchanted creature, the same "becomes a different thing"
 * shape as Witness Protection — but keyed to the Aura remaining attached, so the whole transform
 * reverts the moment the Aura leaves the battlefield (these are continuous static abilities on
 * the Aura, not a one-shot resolution effect):
 *  - [TransformPermanent] Layer 4 (TYPE) keeps the CREATURE card type and replaces all creature
 *    subtypes with Citizen. Colors and name are deliberately left unchanged — unlike Witness
 *    Protection, this card does NOT recolor or rename the creature (the reminder text only calls
 *    out losing other creature types).
 *  - [SetBasePowerToughnessStatic] 1/1 (Layer 7b).
 *  - [LoseAllAbilities] (Layer 6) strips every other ability.
 *  - [GrantKeyword] DEFENDER (Layer 6, applied after the loss so "It has defender" survives the
 *    "loses all other abilities" — the granted defender is one of "its own" abilities, not an
 *    "other" ability).
 *
 * "Loses all other creature types" does not touch supertypes (CR 205.4b) — a Legendary creature
 * enchanted by this stays Legendary.
 */
val SpiderManNoMore = card("Spider-Man No More") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature is a Citizen with base power and toughness " +
        "1/1. It has defender and loses all other abilities. (It also loses all other creature " +
        "types.)"

    auraTarget = Targets.Creature

    // "is a Citizen" — Layer 4: keep the creature card type, replace all subtypes with Citizen.
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("CREATURE"),
            setSubtypes = setOf("Citizen")
        )
    }

    // "with base power and toughness 1/1" — Layer 7b.
    staticAbility {
        ability = SetBasePowerToughnessStatic(1, 1)
    }

    // "loses all other abilities" — Layer 6.
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "It has defender" — Layer 6, granted after the loss so it is retained.
    staticAbility {
        ability = GrantKeyword(Keyword.DEFENDER)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Aniekan Udofia"
        flavorText = "\"Being Spider-Man has brought me nothing but unhappiness! And for what?\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72dbab11-96ed-43db-8b59-ceca47c8cd22.jpg?1783905348"
    }
}
