package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Death-Rattle Oni
 * {6}{B}
 * Creature — Demon Spirit
 * 5/4
 * Flash
 * This spell costs {2} less to cast for each creature that died this turn.
 * When this creature enters, destroy all other creatures that were dealt damage this turn.
 *
 * The discount counts every creature that died this turn under any player's control
 * (`creaturesDiedThisTurn(Player.Each)`), doubled, and only reduces the generic part.
 */
val DeathRattleOni = card("Death-Rattle Oni") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon Spirit"
    power = 5
    toughness = 4
    oracleText = "Flash\nThis spell costs {2} less to cast for each creature that died this turn.\n" +
        "When this creature enters, destroy all other creatures that were dealt damage this turn."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.creaturesDiedThisTurn(Player.Each) * 2),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DestroyAll(
            GameObjectFilter.Creature.wasDealtDamageThisTurn().notSourceItself(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5e0b5cf-39e8-454f-a942-f7865416ef81.jpg?1783916523"
    }
}
