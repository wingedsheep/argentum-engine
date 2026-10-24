package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silverquill Pledgemage — Strixhaven: School of Mages #233 (canonical printing)
 * {1}{W/B}{W/B} · Creature — Vampire Cleric · 3/1
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature gains your
 * choice of flying or lifelink until end of turn.
 *
 * "Your choice of" is made as the trigger resolves, not as it is put on the stack, so it is an
 * [Effects.ChooseAction] rather than a modal trigger (the Golem Artisan shape).
 */
val SilverquillPledgemage = card("Silverquill Pledgemage") {
    manaCost = "{1}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire Cleric"
    power = 3
    toughness = 1
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this creature " +
        "gains your choice of flying or lifelink until end of turn."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "It gains flying until end of turn",
                effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            ),
            EffectChoice(
                label = "It gains lifelink until end of turn",
                effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self),
            ),
        ))
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, this " +
            "creature gains your choice of flying or lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "233"
        artist = "Denman Rooke"
        flavorText = "\"I avoid violence. It is the last resort of the witless. But if you insist, " +
            "I'll make an exception for you.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3f64ad2-4041-421d-baa2-206cedcecf0e.jpg?1783927292"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
