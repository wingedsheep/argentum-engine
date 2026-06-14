package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sting, the Glinting Dagger
 * {2}
 * Legendary Artifact — Equipment
 *
 * Equipped creature gets +1/+1 and has haste.
 * At the beginning of each combat, untap equipped creature.
 * Equipped creature has first strike as long as it's blocking or blocked by a Goblin or Orc.
 * Equip {2}
 *
 * The conditional first strike is a [ConditionalStaticAbility] granting first strike to the
 * equipped creature, gated on [Conditions.SourceIsBlockingOrBlockedBySubtype]. The condition reads
 * the equipped creature (through the Equipment's attachment) and checks both combat directions —
 * the attackers it blocks and the creatures blocking it — against the Goblin/Orc subtypes via
 * projected state, so the keyword is honored when first-strike combat damage is assigned.
 */
val StingTheGlintingDagger = card("Sting, the Glinting Dagger") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has haste.\n" +
        "At the beginning of each combat, untap equipped creature.\n" +
        "Equipped creature has first strike as long as it's blocking or blocked by a Goblin or Orc.\n" +
        "Equip {2}"

    // Equipped creature gets +1/+1...
    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    // ...and has haste.
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)
    }

    // At the beginning of each combat, untap equipped creature.
    triggeredAbility {
        trigger = Triggers.EachCombat
        effect = Effects.Untap(EffectTarget.EquippedCreature)
    }

    // Equipped creature has first strike as long as it's blocking or blocked by a Goblin or Orc.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature),
            condition = Conditions.SourceIsBlockingOrBlockedBySubtype(listOf(Subtype.GOBLIN, Subtype.ORC))
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afbec7e7-f5b9-407e-bf96-2e088710e791.jpg?1686970284"
    }
}
