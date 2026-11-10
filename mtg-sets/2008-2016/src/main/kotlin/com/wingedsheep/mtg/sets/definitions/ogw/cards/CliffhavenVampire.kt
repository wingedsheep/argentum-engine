package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cliffhaven Vampire
 * {2}{W}{B}
 * Creature — Vampire Warrior Ally
 * 2/4
 * Flying
 * Whenever you gain life, each opponent loses 1 life.
 */
val CliffhavenVampire = card("Cliffhaven Vampire") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "BW"
    typeLine = "Creature — Vampire Warrior Ally"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you gain life, each opponent loses 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "David Gaillet"
        flavorText = "\"My people are no longer slaves to the Eldrazi, beholden to the past. We will determine our own future.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5544247d-1f8f-443f-a4c4-d8461db28a79.jpg"
    }
}
