package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Immortal Sun — Rivals of Ixalan #180
 * {6} · Legendary Artifact
 *
 * Players can't activate planeswalkers' loyalty abilities.
 * At the beginning of your draw step, draw an additional card.
 * Spells you cast cost {1} less to cast.
 * Creatures you control get +1/+1.
 *
 * The loyalty lock is [PlayersCantActivateAbilities] narrowed to loyalty abilities of
 * planeswalkers, binding every player (its controller included).
 */
val TheImmortalSun = card("The Immortal Sun") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Players can't activate planeswalkers' loyalty abilities.\n" +
        "At the beginning of your draw step, draw an additional card.\n" +
        "Spells you cast cost {1} less to cast.\n" +
        "Creatures you control get +1/+1."

    staticAbility {
        ability = PlayersCantActivateAbilities(
            affected = Player.Each,
            permanentFilter = GameObjectFilter.Planeswalker,
            loyaltyAbilitiesOnly = true
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGeneric(1)
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "180"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adeccd88-8dc0-4cc1-943f-27d540d248bb.jpg?1783935265"
    }
}
