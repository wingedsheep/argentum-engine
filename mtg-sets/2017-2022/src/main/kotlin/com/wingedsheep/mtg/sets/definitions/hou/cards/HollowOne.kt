package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Hollow One — Hour of Devastation #163 (canonical printing)
 * {5} · Artifact Creature — Golem 4/4
 *
 * This spell costs {2} less to cast for each card you've cycled or discarded this turn.
 * Cycling {2}
 *
 * - Cycling a card discards it (CR 702.29a), so "cycled or discarded" counts each card once: the
 *   per-player discarded-this-turn tracker ([DynamicAmounts.cardsDiscardedThisTurn], defaulting to
 *   you) records every discard you made this turn, from any source, cycling included — and none
 *   of an opponent's. The cards needn't still be in your graveyard (ruling).
 * - The discount is a self-cast [CostModification.ReduceGenericBy], so it reduces generic mana
 *   only and bottoms out at {0} after three discards (ruling).
 */
val HollowOne = card("Hollow One") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Golem"
    power = 4
    toughness = 4
    oracleText = "This spell costs {2} less to cast for each card you've cycled or discarded this turn.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.cardsDiscardedThisTurn() * 2),
            ),
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Anthony Palumbo"
        flavorText = "The embodiment of yearning, forever drifting the deserts to find what had once made it whole."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43fe9c6d-4fa2-4f9a-9025-1bf12fe7ed9f.jpg?1783936002"
        ruling("2021-03-19", "Hollow One's cost is reduced even if the cards you've cycled or discarded aren't in your graveyard.")
        ruling("2021-03-19", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Hollow One). The mana value of the spell is determined by only its mana cost, no matter what the total cost to cast that spell was.")
        ruling("2021-03-19", "Once you've discarded three cards during one turn, Hollow One costs {0} to cast. It won't stop at {1} or cost negative amounts of mana.")
        ruling("2021-03-19", "Hollow One's first ability doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them, such as a cycling ability.")
    }
}
