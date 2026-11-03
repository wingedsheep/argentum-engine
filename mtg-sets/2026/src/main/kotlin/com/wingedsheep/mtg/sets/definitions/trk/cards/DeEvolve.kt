package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * De-evolve
 * {1}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -4/-0, can't block, and is a Salamander. (It loses all other creature types.)
 * Threshold — At the beginning of each upkeep, if there are seven or more cards in your graveyard,
 * exile enchanted creature.
 *
 * Statics over the attached creature: -4/-0 (Torpor Dust), can't block (Pacifism), and Lignify's
 * Layer 4 [TransformPermanent] that replaces only the creature subtypes. The threshold clause is an
 * any-player upkeep trigger with an intervening-if on the Aura controller's graveyard, exiling the
 * enchanted creature as Weight of Conscience does.
 */
val DeEvolve = card("De-evolve") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -4/-0, can't block, and is a Salamander. (It loses all other creature types.)\n" +
        "Threshold — At the beginning of each upkeep, if there are seven or more cards in your graveyard, " +
        "exile enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-4, 0, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = TransformPermanent(setCreatureSubtypes = setOf("Salamander"))
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CardsInGraveyardAtLeast(7)
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81d798f1-07dd-46b1-b0b3-411280a2e257.jpg?1793101370"
    }
}
