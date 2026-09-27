package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Oni Possession
 * {2}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * At the beginning of your upkeep, sacrifice a creature.
 * Enchanted creature gets +3/+3 and has trample.
 * Enchanted creature is a Demon Spirit.
 *
 * "Your upkeep" is the Aura controller's; the sacrifice is unconditional and any creature they
 * control is a legal choice, including the enchanted one. "Is a Demon Spirit" replaces every
 * creature type (Layer 4 `setSubtypes`) and leaves card types, colours and abilities alone.
 */
val OniPossession = card("Oni Possession") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nAt the beginning of your upkeep, sacrifice a creature.\n" +
        "Enchanted creature gets +3/+3 and has trample.\nEnchanted creature is a Demon Spirit."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.SacrificeOwn(GameObjectFilter.Creature)
    }

    staticAbility {
        ability = CompositeStaticAbility(
            listOf(
                ModifyStats(3, 3, GroupFilter.attachedCreature()),
                GrantKeyword(Keyword.TRAMPLE, GroupFilter.attachedCreature())
            )
        )
    }

    staticAbility {
        ability = TransformPermanent(
            setSubtypes = setOf("Demon", "Spirit"),
            filter = GroupFilter.attachedCreature()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/695f122d-e1b7-4dd4-a770-a8f206ba42da.jpg?1783944309"
    }
}
