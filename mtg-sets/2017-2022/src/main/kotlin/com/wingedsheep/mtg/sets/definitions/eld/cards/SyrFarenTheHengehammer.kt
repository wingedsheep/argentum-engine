package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Syr Faren, the Hengehammer (ELD #177)
 * {G}{G} · Legendary Creature — Human Knight · 2/2
 *
 * Whenever Syr Faren attacks, another target attacking creature gets +X/+X until end of turn, where X
 * is Syr Faren's power.
 *
 * X is read as the trigger resolves (`sourcePower` falls back to Syr Faren's last-known power if it
 * has left the battlefield), and a negative power counts as 0 — both per the 2019-10-04 rulings.
 */
val SyrFarenTheHengehammer = card("Syr Faren, the Hengehammer") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Whenever Syr Faren attacks, another target attacking creature gets +X/+X until end of turn, " +
        "where X is Syr Faren's power."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.AttackingCreature.other())
        val x = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower())
        effect = Effects.ModifyStats(x, x, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Chris Rallis"
        flavorText = "\"Words are pointless. It's what you do with your hammer that counts.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a8b1fea-3c5d-43d2-b4d2-e8938f3f7b9c.jpg?1783932602"
        ruling(
            "2019-10-04",
            "The value of X is determined only as Syr Faren's triggered ability resolves. Once that happens, " +
                "the value of X won't change later in the turn even if Syr Faren's power changes.",
        )
        ruling(
            "2019-10-04",
            "If Syr Faren leaves the battlefield before its triggered ability resolves, use its power as it " +
                "last existed on the battlefield to determine the value of X.",
        )
        ruling(
            "2019-10-04",
            "If Syr Faren's power is negative as its triggered ability resolves, X is considered to be 0.",
        )
    }
}
