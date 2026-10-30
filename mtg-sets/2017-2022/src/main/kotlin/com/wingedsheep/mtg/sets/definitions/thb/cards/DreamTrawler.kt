package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dream Trawler
 * {2}{W}{W}{U}{U}
 * Creature — Sphinx
 * 3/5
 * Flying, lifelink
 * Whenever you draw a card, this creature gets +1/+0 until end of turn.
 * Whenever this creature attacks, draw a card.
 * Discard a card: This creature gains hexproof until end of turn. Tap it.
 *
 * `Triggers.you.draws()` fires once per card drawn (Niv-Mizzet's shape). The discard ability has
 * no tap symbol, so it can be activated while already tapped (ruling) — hexproof then a plain
 * self-tap.
 */
val DreamTrawler = card("Dream Trawler") {
    manaCost = "{2}{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Sphinx"
    power = 3
    toughness = 5
    oracleText = "Flying, lifelink\n" +
        "Whenever you draw a card, this creature gets +1/+0 until end of turn.\n" +
        "Whenever this creature attacks, draw a card.\n" +
        "Discard a card: This creature gains hexproof until end of turn. Tap it."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self) then
            Effects.Tap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d3e97d8-92c7-43c4-bdaf-7b0a6ce7cb5f.jpg?1783931523"
        ruling(
            "2020-01-24",
            "Dream Trawler's second triggered ability resolves before blockers are declared. This " +
                "normally causes its first triggered ability to trigger, which also resolves before " +
                "blockers are declared."
        )
        ruling(
            "2020-01-24",
            "You can activate Dream Trawler's last ability even if it's already tapped. It still " +
                "gains hexproof until end of turn."
        )
    }
}
