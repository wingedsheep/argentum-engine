package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Saving Grace — Hour of Devastation #21
 * {1}{W}
 * Enchantment — Aura
 * Flash
 * Enchant creature you control
 * When this Aura enters, all damage that would be dealt this turn to you and permanents you control
 * is dealt to enchanted creature instead.
 * Enchanted creature gets +0/+3.
 *
 * The enters trigger sets up the same turn-long shield as Gideon's Sacrifice: it lists you and
 * filters permanents you control (read at damage time), is never used up, and its recipient is
 * resolved to the creature enchanted when the trigger resolves (or, if the Aura has already left,
 * the creature it was last attached to). The recipient is locked in, so damage keeps going to that
 * creature for the rest of the turn even if Saving Grace leaves; once that creature has left the
 * battlefield nothing is redirected.
 */
val SavingGrace = card("Saving Grace") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature you control\n" +
        "When this Aura enters, all damage that would be dealt this turn to you and permanents you control " +
        "is dealt to enchanted creature instead.\n" +
        "Enchanted creature gets +0/+3."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature.youControl())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.RedirectNextDamage(
            protectedTargets = listOf(EffectTarget.Controller),
            redirectTo = EffectTarget.EnchantedCreature,
            scope = RedirectScope.CONTINUOUS,
            protectedFilter = GameObjectFilter.Permanent.youControl(),
        )
        description = "When this Aura enters, all damage that would be dealt this turn to you and " +
            "permanents you control is dealt to enchanted creature instead."
    }

    staticAbility {
        ability = ModifyStats(0, 3, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "McLean Kendree"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ac7b2d8-7702-4b8c-84ed-1d785dea5cc5.jpg?1783936060"
        ruling("2017-07-14", "Saving Grace's ability has no effect on damage already dealt earlier in the turn.")
        ruling(
            "2017-07-14",
            "If Saving Grace leaves the battlefield during the turn its triggered ability resolved, damage will " +
                "continue to be redirected to the creature it enchanted before it left the battlefield. If the " +
                "creature Saving Grace was last attached to isn't on the battlefield or isn't a creature at the " +
                "time damage would be dealt, it won't be redirected."
        )
        ruling(
            "2017-07-14",
            "More damage can be redirected to the enchanted creature than it has toughness, as long as that " +
                "damage is all dealt at once (like combat damage is)."
        )
        ruling(
            "2017-07-14",
            "Saving Grace's redirection effect doesn't change the source of the damage or whether the damage is combat damage."
        )
        ruling(
            "2017-07-14",
            "If you have more than one Saving Grace enter the battlefield in one turn, all damage that would be " +
                "dealt at once to you and/or permanents you control is dealt to one of the enchanted creatures of " +
                "your choice. It's not dealt to all of them, and you can't split the damage between them."
        )
    }
}
