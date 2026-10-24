package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Witherbloom Apprentice — Strixhaven: School of Mages #247 (canonical printing)
 * {B}{G} · Creature — Human Druid · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, each opponent loses 1 life
 * and you gain 1 life.
 *
 * The life gained is a printed 1, not the life lost — `LoseLife(EachOpponent) then GainLife(1)`
 * (Drana's Emissary's shape).
 */
val WitherbloomApprentice = card("Witherbloom Apprentice") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, each opponent " +
        "loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, each " +
            "opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Josh Hass"
        flavorText = "\"All we are—every feeling, every action—powered by a single, delicate little " +
            "organ. Life is so terrifyingly, beautifully fragile.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f80a11b-188b-464c-b00d-c9d1cfb8ddee.jpg?1783927284"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
