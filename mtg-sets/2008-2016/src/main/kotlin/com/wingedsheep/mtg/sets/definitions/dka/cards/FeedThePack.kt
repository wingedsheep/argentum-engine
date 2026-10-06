package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Feed the Pack
 * {5}{G}
 * Enchantment
 *
 * At the beginning of your end step, you may sacrifice a nontoken creature. If you do, create X
 * 2/2 green Wolf creature tokens, where X is the sacrificed creature's toughness.
 *
 * The optional sacrifice is a [Effects.MayPay] whose cost is a resolution-time
 * [Effects.SacrificeOwn]; both the "yes" and the choice of creature happen as the trigger resolves,
 * per the ruling. The sacrifice records the creature's last-known snapshot in the effect context
 * (the same path Tribute to Hunger reads), so [DynamicAmounts.sacrificedToughness] in the `then`
 * branch sees its toughness as it last existed on the battlefield. With no nontoken creature the
 * gate is unaffordable and the trigger does nothing.
 *
 * The Wolf carries no `imageUri`: its art comes from the printing's set `tokenArt` (or the generic
 * creature-type fallback), so the J22 reprint mints its own Wolf.
 */
val FeedThePack = card("Feed the Pack") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, you may sacrifice a nontoken creature. If you do, " +
        "create X 2/2 green Wolf creature tokens, where X is the sacrificed creature's toughness."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(GameObjectFilter.Creature.nontoken()),
            then = Effects.CreateToken(
                count = DynamicAmounts.sacrificedToughness(),
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Wolf"),
            ),
        )
        description = "At the beginning of your end step, you may sacrifice a nontoken creature. If you " +
            "do, create X 2/2 green Wolf creature tokens, where X is the sacrificed creature's toughness."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9831e3cc-659b-4408-b5d8-a27ae2738680.jpg?1783940809"

        ruling(
            "2011-01-22",
            "You choose whether to sacrifice a nontoken creature and which creature to sacrifice when " +
                "Feed the Pack's ability resolves."
        )
        ruling(
            "2011-01-22",
            "Check the sacrificed creature's toughness as it last existed on the battlefield to determine " +
                "how many Wolves you put onto the battlefield."
        )
    }
}
