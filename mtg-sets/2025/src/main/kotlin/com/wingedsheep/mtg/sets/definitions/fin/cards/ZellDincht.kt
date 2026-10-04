package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Zell Dincht — Final Fantasy #170
 * {2}{R} · Legendary Creature — Human Monk · 0/3
 *
 * You may play an additional land on each of your turns.
 * Zell Dincht gets +1/+0 for each land you control.
 * At the beginning of your end step, return a land you control to its owner's hand.
 *
 * - Extra land drop: [GrantAdditionalLandDrop] (cumulative with similar effects).
 * - The +1/+0 buff is a continuous self-buff whose power bonus is the number of lands you control,
 *   read through projected control via [GrantDynamicStats]. Not "other" lands — Zell isn't a
 *   land, so every land you control counts.
 * - The end-step bounce is a forced (non-"may") triggered ability that doesn't target (CR 115.10a):
 *   you choose a land you control as it resolves and return it to its owner's hand.
 */
val ZellDincht = card("Zell Dincht") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Monk"
    power = 0
    toughness = 3
    oracleText = "You may play an additional land on each of your turns.\n" +
        "Zell Dincht gets +1/+0 for each land you control.\n" +
        "At the beginning of your end step, return a land you control to its owner's hand."

    // You may play an additional land on each of your turns.
    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    // Zell Dincht gets +1/+0 for each land you control.
    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.landsYouControl(),
            toughnessBonus = DynamicAmounts.fixed(0),
        )
    }

    // At the beginning of your end step, return a land you control to its owner's hand.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
            val returned = chooseExactly(
                1,
                from = lands,
                prompt = "Return a land you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
        description = "At the beginning of your end step, return a land you control to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Kevin Sidharta"
        flavorText = "\"My weapons are these fists of mine!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/135d6b27-9168-4513-9d7d-56edae048857.jpg?1748706397"
    }
}
