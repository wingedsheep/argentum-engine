package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grim Reaper's Sprint — Fallout #58
 * {4}{R} · Enchantment — Aura
 *
 * Morbid — This spell costs {3} less to cast if a creature died this turn.
 * Enchant creature
 * When this Aura enters, untap each creature you control. If it's your main phase, there is an
 * additional combat phase after this phase.
 * Enchanted creature gets +2/+2 and has haste.
 *
 * Morbid is the table-wide died-this-turn tally ([CostReductionSource.FixedIfCreatureDiedThisTurn]).
 * The extra combat is [Effects.AddCombatPhase] behind [Conditions.IsYourMainPhase]: it is inserted
 * directly after the main phase the Aura entered in, so cast precombat it comes *before* the natural
 * combat phase (the turn then has its own combat and postcombat main as usual), and it adds no main
 * phase of its own.
 */
val GrimReapersSprint = card("Grim Reaper's Sprint") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Morbid — This spell costs {3} less to cast if a creature died this turn.\n" +
        "Enchant creature\n" +
        "When this Aura enters, untap each creature you control. If it's your main phase, there is an " +
        "additional combat phase after this phase.\n" +
        "Enchanted creature gets +2/+2 and has haste."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfCreatureDiedThisTurn(3)
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Untap(EffectTarget.IterationEntity)
        ) then Effects.If(
            condition = Conditions.IsYourMainPhase,
            then = Effects.AddCombatPhase,
        )
    }

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18e76286-9e06-42de-b322-eb8aa2cdca3a.jpg?1783912404"
        ruling(
            "2024-03-08",
            "If the creature Grim Reaper's Sprint would enchant is an illegal target by the time Grim Reaper's " +
                "Sprint would resolve, the entire spell doesn't resolve. It's put into the graveyard from the stack, " +
                "so its triggered ability won't trigger, and you won't untap your creatures or get an additional " +
                "combat phase.",
        )
    }
}
