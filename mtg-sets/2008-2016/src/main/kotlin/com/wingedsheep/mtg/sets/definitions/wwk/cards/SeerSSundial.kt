package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect

/**
 * Seer's Sundial
 * {4}
 * Artifact
 * Landfall — Whenever a land you control enters, you may pay {2}. If you do, draw a card.
 *
 * Landfall is [Triggers.LandYouControlEnters]. The "you may pay {2}. If you do, …" is an
 * [OptionalCostEffect] — a [com.wingedsheep.sdk.scripting.effects.Gate.MayPay] gate whose cost is
 * [PayManaCostEffect] and whose `ifPaid` branch is [Effects.DrawCards]. That is a different gate
 * from the bare "you may" of a card like Grazing Gladehart: the consent here is the payment, so the
 * draw is conditioned on the mana actually being spent rather than on a yes/no answer.
 */
val SeerSSundial = card("Seer's Sundial") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Landfall — Whenever a land you control enters, you may pay {2}. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = OptionalCostEffect(
            cost = PayManaCostEffect(ManaCost.parse("{2}")),
            ifPaid = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Franz Vohwinkel"
        flavorText = "\"The shadow travels toward the apex. I predict we will soon see the true measure of darkness.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/1/314cb009-bd0e-40eb-98ea-c6dea8d83dcf.jpg"
        ruling("2024-11-08", "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.")
        ruling("2024-11-08", "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.")
        ruling("2024-11-08", "Whenever a land you control enters, each landfall ability of the permanents you control will trigger. You can put them   on the stack in any order. The last ability you put on the stack will be the first one to resolve (As a result, you can have those abilities resolve in the order of your choosing.).")
        ruling("2010-03-01", "You choose whether to pay {2} as the ability resolves. You may pay {2} only once per resolution.")
    }
}
