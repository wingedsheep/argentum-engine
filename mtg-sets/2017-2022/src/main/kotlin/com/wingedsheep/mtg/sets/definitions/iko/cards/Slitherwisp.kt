package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slitherwisp
 * {U}{B}{B}
 * Creature — Elemental Nightmare
 * 3/2
 * Flash
 * Whenever you cast another spell that has flash, you draw a card and each opponent loses 1 life.
 *
 * The cast trigger filters on the spell's own flash keyword (Cunning Nightbonder's filter), so a
 * spell cast "as though it had flash" doesn't trigger it (ruling). "Another" needs no filter of its
 * own: while Slitherwisp itself is being cast it is on the stack, not on the battlefield where the
 * ability functions.
 */
val Slitherwisp = card("Slitherwisp") {
    manaCost = "{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Elemental Nightmare"
    power = 3
    toughness = 2
    oracleText = "Flash\nWhenever you cast another spell that has flash, you draw a card and each opponent loses 1 life."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withKeyword(Keyword.FLASH))
        effect = Effects.DrawCards(1) then
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever you cast another spell that has flash, you draw a card and each opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Yigit Koroglu"
        flavorText = "Fear is its habitat, sanity its prey."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8067f195-8b6e-4329-a34e-43e52f67c571.jpg?1783931018"
        ruling("2020-04-17", "If an effect allows you to cast a spell “as though” it has flash, Slitherwisp’s last ability won’t trigger.")
        ruling("2020-04-17", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-04-17", "Players can cast spells and activate abilities after the triggered ability resolves but before the spell that caused it to trigger does.")
        ruling("2020-04-17", "In a Two-Headed Giant game, Slitherwisp’s last ability causes the opposing team to lose 2 life and you to draw one card.")
    }
}
