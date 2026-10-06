package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kothophed, Soul Hoarder
 * {4}{B}{B}
 * Legendary Creature — Demon
 * 6/6
 * Flying
 * Whenever a permanent owned by another player is put into a graveyard from the battlefield,
 * you draw a card and you lose 1 life.
 *
 * "Owned by another player" reads the permanent's immutable owner, not its controller (per the
 * ruling), so the filter is `Not(OwnedByYou)` — a permanent you stole still counts, and one you
 * own that an opponent controls doesn't. ANY binding over every permanent; tokens count too.
 */
val KothophedSoulHoarder = card("Kothophed, Soul Hoarder") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\nWhenever a permanent owned by another player is put into a graveyard " +
        "from the battlefield, you draw a card and you lose 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.ownedByOpponent()).dies()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Jakub Kasper"
        flavorText = "\"I will be your most demanding master, Liliana. But I have great things in mind for you.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/036e696a-b2aa-40ba-9ed6-3a859c4288a0.jpg?1783938340"
        ruling("2015-06-22", "It doesn't matter who controlled the permanent when it was put into a graveyard.")
        ruling("2015-06-22", "The triggered ability is mandatory. You can't decline to draw the card and lose life, even if you want to.")
    }
}
