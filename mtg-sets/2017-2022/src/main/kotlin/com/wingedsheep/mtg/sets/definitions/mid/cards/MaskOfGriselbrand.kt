package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Mask of Griselbrand — Innistrad: Midnight Hunt #111
 * {1}{B}{B} · Legendary Artifact — Equipment · Rare
 *
 * Equipped creature has flying and lifelink.
 * Whenever equipped creature dies, you may pay X life, where X is its power. If you do, draw X cards.
 * Equip {3}
 *
 * X is the dead creature's last-known power ([DynamicAmounts.triggeringPower]), read both for the
 * life payment and for the draw. The [Effects.MayPay] gate offers the payment only when the
 * controller can pay exactly X life (CR 119.4) — there is no paying less to draw fewer.
 */
val MaskOfGriselbrand = card("Mask of Griselbrand") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature has flying and lifelink.\n" +
        "Whenever equipped creature dies, you may pay X life, where X is its power. If you do, draw X cards.\n" +
        "Equip {3}"

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.MayPay(
            cost = Effects.PayDynamicLife(DynamicAmounts.triggeringPower()),
            then = Effects.DrawCards(DynamicAmounts.triggeringPower()),
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Jason Felix"
        flavorText = "The legacy of evil lives on."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/829786e2-260b-4bc6-935b-1868b37e2d33.jpg?1783925614"

        ruling(
            "2021-09-24",
            "Use the equipped creature's power as it last existed on the battlefield to determine the value of X."
        )
        ruling(
            "2021-09-24",
            "You must pay exactly X life or nothing. You can't pay less life to draw fewer cards."
        )
    }
}
