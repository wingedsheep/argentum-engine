package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Sedgemoor Witch — Strixhaven: School of Mages #86 (canonical printing)
 * {2}{B} · Creature — Human Warlock · 3/2
 *
 * Menace
 * Ward—Pay 3 life.
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, create a 1/1 black and green
 * Pest creature token with "When this token dies, you gain 1 life."
 *
 * Menace is the bare keyword; ward is Owlin Shieldmage's [WardCost.Life]; magecraft is
 * `Triggers.you.castsOrCopies(InstantOrSorcery)` paying off with the set's Pest token
 * ([Effects.CreatePest]).
 */
val SedgemoorWitch = card("Sedgemoor Witch") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    oracleText =
        "Menace\n" +
        "Ward—Pay 3 life. (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays 3 life.)\n" +
        "Magecraft — Whenever you cast or copy an instant or sorcery spell, create a 1/1 black and green Pest creature token with \"When this token dies, you gain 1 life.\""
    power = 3
    toughness = 2

    keywords(Keyword.MENACE)
    keywordAbility(KeywordAbility.Ward(WardCost.Life(3)))

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.CreatePest()
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, create a " +
            "1/1 black and green Pest creature token with \"When this token dies, you gain 1 life.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e900c1eb-968b-4046-b824-c167a7a5b682.jpg?1783927362"
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "If a player casts a spell that targets multiple permanents their opponent controls with ward, each of those ward abilities will trigger. If that player doesn't pay for all of them, the spell will be countered.")
    }
}
