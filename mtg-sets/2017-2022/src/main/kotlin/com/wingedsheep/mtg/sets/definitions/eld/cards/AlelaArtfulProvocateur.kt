package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Alela, Artful Provocateur
 * {1}{W}{U}{B}
 * Legendary Creature — Faerie Warlock
 * 2/3
 * Flying, deathtouch, lifelink
 * Other creatures you control with flying get +1/+0.
 * Whenever you cast an artifact or enchantment spell, create a 1/1 blue Faerie creature token with flying.
 */
val AlelaArtfulProvocateur = card("Alela, Artful Provocateur") {
    manaCost = "{1}{W}{U}{B}"
    typeLine = "Legendary Creature — Faerie Warlock"
    oracleText = "Flying, deathtouch, lifelink\n" +
        "Other creatures you control with flying get +1/+0.\n" +
        "Whenever you cast an artifact or enchantment spell, create a 1/1 blue Faerie creature token with flying."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH, Keyword.LIFELINK)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl(), excludeSelf = true)
        )
    }

    // A spell that's both an artifact and an enchantment is one cast event, so this triggers once.
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.ArtifactOrEnchantment)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Faerie"),
            keywords = setOf(Keyword.FLYING),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "324"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"Rankle's pranks are child's play. My games will topple kingdoms.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/726e7dc5-2089-4758-93e1-79212aedf75f.jpg?1783932550"
        ruling("2019-10-04", "Alela's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2019-10-04", "A spell that's both an artifact and an enchantment spell causes Alela's last ability to trigger only once.")
    }
}
