package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * The Gitrog Monster
 * {3}{B}{G}
 * Legendary Creature — Frog Horror
 * 6/6
 *
 * Deathtouch
 * At the beginning of your upkeep, sacrifice The Gitrog Monster unless you sacrifice a land.
 * You may play an additional land on each of your turns.
 * Whenever one or more land cards are put into your graveyard from anywhere, draw a card.
 *
 * Three existing primitives, no new vocabulary:
 * - The upkeep "unless" is [PayOrSufferEffect] with a sacrifice-a-land cost and
 *   [SacrificeSelfEffect] as the punisher (same shape as Endless Wurm) — declining, or controlling
 *   no land at all, sacrifices the Frog.
 * - The extra land drop is the static [GrantAdditionalLandDrop] (cumulative with other such effects).
 * - The draw trigger is the **batched** [Triggers.CardsPutIntoYourGraveyard], so per the 2025-01-24
 *   ruling several lands hitting the graveyard at once (a mill, or two land creatures dying together)
 *   draws one card, not one per land. "From anywhere" is intrinsic to that event — library, hand,
 *   and battlefield all count.
 */
val TheGitrogMonster = card("The Gitrog Monster") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Frog Horror"
    power = 6
    toughness = 6
    oracleText = "Deathtouch\n" +
        "At the beginning of your upkeep, sacrifice The Gitrog Monster unless you sacrifice a land.\n" +
        "You may play an additional land on each of your turns.\n" +
        "Whenever one or more land cards are put into your graveyard from anywhere, draw a card."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = PayOrSufferEffect(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Land),
            suffer = SacrificeSelfEffect
        )
        description = "At the beginning of your upkeep, sacrifice The Gitrog Monster unless you sacrifice a land."
    }

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    triggeredAbility {
        trigger = Triggers.CardsPutIntoYourGraveyard(GameObjectFilter.Land)
        effect = Effects.DrawCards(1)
        description = "Whenever one or more land cards are put into your graveyard from anywhere, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "245"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5790dd89-2be5-4a77-9450-2d3c1422bfc9.jpg?1783937714"

        ruling(
            "2025-01-24",
            "If multiple land cards are put into your graveyard at once, The Gitrog Monster's last " +
                "ability triggers only once. This could happen because an effect put them there from " +
                "your library at the same time (such as by a mill effect) or because they were " +
                "destroyed at the same time (such as two land creatures that were dealt lethal " +
                "combat damage)."
        )
    }
}
