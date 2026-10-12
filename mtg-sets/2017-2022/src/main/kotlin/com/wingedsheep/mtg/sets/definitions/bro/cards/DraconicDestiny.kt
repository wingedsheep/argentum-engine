package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Draconic Destiny
 * {1}{R}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has flying, haste, and "{1}: This creature gets +1/+0 until
 * end of turn." It's a Dragon in addition to its other types.
 * When enchanted creature dies, return this card to its owner's hand.
 *
 * Same shape as Angelic Destiny plus Midnight Covenant's granted firebreathing. The Dragon type is
 * granted by the Aura itself, independent of the granted ability (per the ruling).
 */
val DraconicDestiny = card("Draconic Destiny") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has flying, haste, and \"{1}: This creature gets +1/+0 until end of turn.\" " +
        "It's a Dragon in addition to its other types.\n" +
        "When enchanted creature dies, return this card to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Mana("{1}"),
                effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
            )
        )
    }

    staticAbility {
        // GrantSubtype defaults its filter to the source — an Aura must name the enchanted creature.
        ability = GrantSubtype("Dragon", Filters.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "130"
        artist = "Justin Hernandez & Alexis Hernandez"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f71240ad-1daf-419a-aaeb-5f9d5079c3dd.jpg?1783920074"
        ruling(
            "2022-10-14",
            "Being a Dragon isn't part of the activated ability the enchanted creature has. It's a Dragon " +
                "in addition to its other types whether or not that ability has been activated."
        )
    }
}
