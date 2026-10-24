package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lorehold Pledgemage — Strixhaven: School of Mages #201 (canonical printing)
 * {1}{R/W}{R/W} · Creature — Kor Shaman · 2/2
 *
 * First strike
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature gets +1/+0
 * until end of turn.
 */
val LoreholdPledgemage = card("Lorehold Pledgemage") {
    manaCost = "{1}{R/W}{R/W}"
    colorIdentity = "RW"
    typeLine = "Creature — Kor Shaman"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature gets " +
        "+1/+0 until end of turn."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this " +
            "creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Jake Murray"
        flavorText = "\"The weight of history can empower you or it can crush you. It's your choice.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79b88b38-6b5a-4a89-80ca-add79c11e8b9.jpg?1783927308"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
