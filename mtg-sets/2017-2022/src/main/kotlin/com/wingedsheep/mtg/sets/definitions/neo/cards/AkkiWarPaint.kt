package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Akki War Paint — Kamigawa: Neon Dynasty #132
 * {R} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * As long as enchanted permanent is a creature, it gets +2/+1.
 *
 * Nature's Embrace's shape: a [ModifyStats] over the enchanted permanent, gated by
 * [Conditions.EnchantedPermanentMatches] on the host's current (projected) type, so an uncrewed
 * Vehicle gets nothing and a crewed one gets the bonus while it's a creature.
 */
val AkkiWarPaint = card("Akki War Paint") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "As long as enchanted permanent is a creature, it gets +2/+1."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(2, 1, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Jeremy Wilson"
        flavorText = "Akki paint bright, bold designs on their salvaged Imperial mechs, leaving no " +
            "doubt as to who they belong to now."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f110698e-a343-4eff-ba23-ed7ac5a3f87b.jpg?1783923871"
    }
}
