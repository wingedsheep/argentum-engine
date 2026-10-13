package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zur, Eternal Schemer
 * {W}{U}{B}
 * Legendary Creature — Human Wizard
 * 1/4
 *
 * Flying
 * Enchantment creatures you control have deathtouch, lifelink, and hexproof.
 * {1}{W}: Target non-Aura enchantment you control becomes a creature in addition to its other types
 * and has base power and base toughness each equal to its mana value.
 *
 * - The animation has no duration (ruling) — `Duration.Permanent`. It keeps the enchantment's
 *   other types, so the animated enchantment is immediately an enchantment creature and picks up
 *   Zur's keyword grant.
 * - Base P/T is the animated permanent's own mana value ([Effects.BecomeCreatureWithManaValueStats]);
 *   an {X} in its cost counts as 0 on the battlefield (ruling, CR 202.3).
 */
val ZurEternalSchemer = card("Zur, Eternal Schemer") {
    manaCost = "{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 4
    oracleText = "Flying\n" +
        "Enchantment creatures you control have deathtouch, lifelink, and hexproof.\n" +
        "{1}{W}: Target non-Aura enchantment you control becomes a creature in addition to its other types " +
        "and has base power and base toughness each equal to its mana value."

    keywords(Keyword.FLYING)

    val enchantmentCreaturesYouControl =
        GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Enchantment).youControl())

    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH, enchantmentCreaturesYouControl)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, enchantmentCreaturesYouControl)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, enchantmentCreaturesYouControl)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val enchantment = target(
            TargetFilter(GameObjectFilter.Enchantment.notSubtype(Subtype.AURA).youControl())
        )
        effect = Effects.BecomeCreatureWithManaValueStats(
            target = enchantment,
            duration = Duration.Permanent,
        )
        description = "Target non-Aura enchantment you control becomes a creature in addition to its other " +
            "types and has base power and base toughness each equal to its mana value."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "228"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d987435-2403-4e0f-b19d-693da923ba50.jpg?1783921271"
        ruling("2022-09-09", "Zur, Eternal Schemer's last ability does not have a duration. Once it resolves the target enchantment will be a creature until it leaves the battlefield or another effect causes it to stop being a creature.")
        ruling("2022-09-09", "If a permanent on the battlefield has an {X} in its mana cost, X is zero when determining its mana value.")
    }
}
