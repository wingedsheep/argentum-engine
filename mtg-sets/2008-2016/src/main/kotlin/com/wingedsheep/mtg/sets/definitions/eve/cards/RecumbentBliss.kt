package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Recumbent Bliss
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block.
 * At the beginning of your upkeep, you may gain 1 life.
 */
val RecumbentBliss = card("Recumbent Bliss") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "At the beginning of your upkeep, you may gain 1 life."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(Effects.GainLife(1))
        description = "At the beginning of your upkeep, you may gain 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Todd Lockwood"
        flavorText = "Kithkin somnomancers enjoy the peaceful dreams wafting from their victims."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53c28634-337d-458a-bcf0-b7fbf65a41da.jpg?1783942693"
    }
}
