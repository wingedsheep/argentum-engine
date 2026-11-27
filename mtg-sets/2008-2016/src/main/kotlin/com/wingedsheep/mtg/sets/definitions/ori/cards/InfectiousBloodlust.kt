package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Infectious Bloodlust
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+1, has haste, and attacks each combat if able.
 * When enchanted creature dies, you may search your library for a card named Infectious
 * Bloodlust, reveal it, put it into your hand, then shuffle.
 *
 * "You" is the Aura's controller (the trigger's controller), so enchanting an opponent's
 * creature still searches your own library. The "you may" wraps the whole search, so
 * declining skips the shuffle too.
 */
val InfectiousBloodlust = card("Infectious Bloodlust") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+1, has haste, and attacks each combat if able.\n" +
        "When enchanted creature dies, you may search your library for a card named Infectious " +
        "Bloodlust, reveal it, put it into your hand, then shuffle."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE)
    }

    staticAbility {
        ability = MustAttack(GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.named("Infectious Bloodlust"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee44df88-45a4-46ef-a8bd-f7fb7b9542b8.jpg?1783938329"
        ruling(
            "2015-06-22",
            "If, during its controller's declare attackers step, the enchanted creature is tapped or is " +
                "affected by a spell or ability that says it can't attack, then that creature doesn't " +
                "attack. If there's a cost associated with having that creature attack, its controller " +
                "isn't forced to pay that cost. If they don't, the creature doesn't have to attack."
        )
    }
}
