package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Prison Sentence
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, scry 2.
 * Enchanted creature can't attack or block, and its activated abilities can't be activated.
 *
 * Arrest's three-static lock (combat lock on the attached creature plus
 * [PreventActivatedAbilities] scoped by `attachedToBySource()`, mana abilities included) with
 * Gift of Strands' enters-scry trigger.
 */
val PrisonSentence = card("Prison Sentence") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, scry 2.\n" +
        "Enchanted creature can't attack or block, and its activated abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.scry(2)
    }

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Valera Lutfullina"
        flavorText = "War prisoners were ineffective political pawns, as neither brother cared " +
            "enough to trade for them."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76832ca8-95b2-4bc8-a191-9e2f50b77626.jpg?1783920126"
    }
}
