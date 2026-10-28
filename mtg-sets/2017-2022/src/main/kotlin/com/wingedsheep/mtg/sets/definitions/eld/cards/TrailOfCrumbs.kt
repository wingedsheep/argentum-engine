package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Trail of Crumbs — Throne of Eldraine #179 (canonical printing)
 * {1}{G} · Enchantment
 *
 * When this enchantment enters, create a Food token.
 * Whenever you sacrifice a Food, you may pay {1}. If you do, look at the top two cards of your
 * library. You may reveal a permanent card from among them and put it into your hand. Put the
 * rest on the bottom of your library in any order.
 *
 * The {1} is paid once per trigger (per the ruling, it can't be paid repeatedly to look deeper).
 */
val TrailOfCrumbs = card("Trail of Crumbs") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a Food token. (It's an artifact with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you sacrifice a Food, you may pay {1}. If you do, look at the top two cards of your " +
        "library. You may reveal a permanent card from among them and put it into your hand. Put the " +
        "rest on the bottom of your library in any order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact.withSubtype("Food"))
        effect = Effects.MayPay(
            ManaCost.parse("{1}"),
            Patterns.Library.lookAtTopRevealMatchingToHand(
                count = 2,
                filter = GameObjectFilter.Permanent,
                prompt = "You may reveal a permanent card from among them and put it into your hand",
                restOrder = CardOrder.ControllerChooses
            )
        )
        description = "Whenever you sacrifice a Food, you may pay {1}. If you do, look at the top two cards " +
            "of your library. You may reveal a permanent card from among them and put it into your hand. " +
            "Put the rest on the bottom of your library in any order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2688ac16-4ed6-4e79-815a-be173deb4603.jpg?1783932603"
        ruling("2019-10-04", "While resolving the second triggered ability, you can't pay {1} multiple times to look at more cards.")
        ruling("2024-11-08", "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token.")
    }
}
