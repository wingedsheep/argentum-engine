package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gift of Wrath — Kamigawa: Neon Dynasty #143
 * {3}{R} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * As long as enchanted permanent is a creature, it gets +2/+2 and has menace.
 * When this Aura leaves the battlefield, create a 2/2 red Spirit creature token with menace.
 *
 * Akki War Paint's shape: both halves of the bonus are gated by
 * [Conditions.EnchantedPermanentMatches] on the host's current (projected) type, so an uncrewed
 * Vehicle gets nothing. The leaves trigger is Goblin Shrine's self-leaves trigger.
 */
val GiftOfWrath = card("Gift of Wrath") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "As long as enchanted permanent is a creature, it gets +2/+2 and has menace. " +
        "(It can't be blocked except by two or more creatures.)\n" +
        "When this Aura leaves the battlefield, create a 2/2 red Spirit creature token with menace."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature)
        ability = GrantKeyword(Keyword.MENACE, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/c/2/c20e427c-efa5-476c-aa33-e3149648d207.jpg?1783923714"
        )
        description = "When this Aura leaves the battlefield, create a 2/2 red Spirit creature token with menace."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Isis"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c33f6d9-be29-4698-96f9-d393a2c8ad68.jpg?1783923867"
    }
}
