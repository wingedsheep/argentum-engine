package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eager First-Year — Strixhaven: School of Mages #16 (canonical printing)
 * {1}{W} · Creature — Human Wizard · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature gets +1/+0
 * until end of turn.
 */
val EagerFirstYear = card("Eager First-Year") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature " +
        "gets +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this " +
            "creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Cristi Balanescu"
        flavorText = "Today, she learns the basics. She'll knock out advanced theory later this week."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a83543b-3b6f-4e28-96f9-007b814bcfd6.jpg?1783927392"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
    }
}
