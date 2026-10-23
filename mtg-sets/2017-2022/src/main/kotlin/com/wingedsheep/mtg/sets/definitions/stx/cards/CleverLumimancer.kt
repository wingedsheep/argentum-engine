package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clever Lumimancer — Strixhaven: School of Mages #10 (canonical printing)
 * {W} · Creature — Human Wizard · 0/1
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature gets +2/+2
 * until end of turn.
 */
val CleverLumimancer = card("Clever Lumimancer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 1
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature " +
        "gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this " +
            "creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Lie Setiawan"
        flavorText = "\"You're too dim to realize it, but you've already lost.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69957656-cfdf-4001-8e84-0ef29e4a468d.jpg?1783927394"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "Some effects instruct you to copy an instant or sorcery card in a zone other than the stack. These copies do not cause magecraft abilities to trigger. However, most effects that do this also allow you to cast the copy, and casting the copy will cause magecraft abilities to trigger.")
    }
}
