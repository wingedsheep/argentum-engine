package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cavern-Hoard Dragon
 * {7}{R}{R}
 * Creature — Dragon
 * 6/6
 *
 * This spell costs {X} less to cast, where X is the greatest number of artifacts an opponent controls.
 * Flying, trample, haste
 * Whenever this creature deals combat damage to a player, you create a Treasure token for each
 * artifact that player controls.
 *
 * - The cost reduction is [DynamicAmount.GreatestAmongPlayers] around a per-opponent artifact count:
 *   only the single opponent with the most artifacts counts, never the sum across opponents.
 * - The Treasure count is read as the trigger resolves ([Player.TriggeringPlayer] is the player the
 *   Dragon hit), so the Treasure it already made doesn't change it and artifacts that left in
 *   between don't count.
 */
val CavernHoardDragon = card("Cavern-Hoard Dragon") {
    manaCost = "{7}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "This spell costs {X} less to cast, where X is the greatest number of artifacts an " +
        "opponent controls.\n" +
        "Flying, trample, haste\n" +
        "Whenever this creature deals combat damage to a player, you create a Treasure token for " +
        "each artifact that player controls."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(
                    DynamicAmounts.greatestControlledBySinglePlayer(
                        GameObjectFilter.Artifact,
                        Player.EachOpponent,
                    )
                )
            )
        )
    }

    keywords(Keyword.FLYING, Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTreasure(
            count = DynamicAmounts.battlefield(Player.TriggeringPlayer, GameObjectFilter.Artifact).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c67e509e-a219-4f7f-b7eb-8c446cc0620a.jpg?1783916028"
        ruling(
            "2023-06-16",
            "Cavern-Hoard Dragon's last ability cares about the number of artifacts that player controls as the ability resolves."
        )
        ruling(
            "2023-06-16",
            "Once a player has announced that they are casting Cavern-Hoard Dragon, no player may take actions to try and change the greatest number of artifacts an opponent controls before Cavern-Hoard Dragon's cost is locked in."
        )
        ruling(
            "2023-06-16",
            "If the greatest number of artifacts an opponent controls is seven or more, Cavern-Hoard Dragon costs {R}{R} to cast."
        )
        ruling(
            "2023-06-16",
            "Cavern-Hoard Dragon's first ability counts only the artifacts controlled by the opponent who controls the greatest number of artifacts among your opponents. If you have an opponent who controls two artifacts and another opponent who controls three artifacts, Cavern-Hoard Dragon's mana cost would be reduced by {3}."
        )
    }
}
