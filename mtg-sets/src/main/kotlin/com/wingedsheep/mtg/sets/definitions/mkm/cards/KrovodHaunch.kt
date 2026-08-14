package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Krovod Haunch — Murders at Karlov Manor #21
 * {W} · Artifact — Food Equipment
 *
 * Equipped creature gets +2/+0.
 * {2}, {T}, Sacrifice this Equipment: You gain 3 life.
 * When this Equipment is put into a graveyard from the battlefield, you may pay {1}{W}. If you do,
 * create two 1/1 white Dog creature tokens.
 * Equip {2}
 *
 * A Food that is also an Equipment. The Food subtype is on the type line, so "sacrifice a Food" /
 * "tap a Food" payoffs see it without anything extra (Scryfall's ruling: "If an effect refers to a
 * Food, it means any Food artifact, not just a Food artifact token" — you can tap Krovod Haunch to
 * pay for Apothecary White). Note the printed sacrifice ability is *not* the Food token's ability:
 * it costs an extra {T} and gains 3 life rather than the token's plain "{2}, {T}, Sacrifice: You
 * gain 3 life" — same shape here, but it is printed on the card rather than conferred by the
 * subtype, which is why it's written out.
 *
 * The leaves-the-battlefield trigger is [Triggers.Dies] (battlefield → graveyard, SELF binding),
 * which is a superset of the sacrifice ability's own path: cashing the Haunch in for 3 life *also*
 * offers the Dogs, since sacrificing puts it into the graveyard from the battlefield. The
 * [MayPayManaEffect] models "you may pay {1}{W}. If you do" — a resolution-time optional mana
 * payment, not an additional cost — and nothing downstream reads the source, so the Equipment
 * already being in the graveyard when the trigger resolves is harmless.
 *
 * The Dog token takes its art from the MKM `tokenArt` layer (as Dog Walker's does), so no
 * `imageUri` is baked in here.
 */
val KrovodHaunch = card("Krovod Haunch") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Food Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "{2}, {T}, Sacrifice this Equipment: You gain 3 life.\n" +
        "When this Equipment is put into a graveyard from the battlefield, you may pay {1}{W}. " +
        "If you do, create two 1/1 white Dog creature tokens.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(+2, +0, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.GainLife(3)
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}{W}"),
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Dog"),
                count = 2
            )
        )
        description = "When this Equipment is put into a graveyard from the battlefield, you may " +
            "pay {1}{W}. If you do, create two 1/1 white Dog creature tokens."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f663cd86-39e6-467b-85b7-dd27536251a6.jpg?1783912923"

        ruling(
            "2024-02-02",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact " +
                "token. For example, you can tap Krovod Haunch to pay for Apothecary White's " +
                "activated ability."
        )
    }
}
