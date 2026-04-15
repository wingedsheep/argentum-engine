package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dawnhand Eulogist
 * {3}{B}
 * Creature — Elf Warlock
 * 3/3
 *
 * Menace
 * When this creature enters, mill three cards. Then if there is an Elf card
 * in your graveyard, each opponent loses 2 life and you gain 2 life.
 */
val DawnhandEulogist = card("Dawnhand Eulogist") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Elf Warlock"
    power = 3
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, mill three cards. Then if there is an Elf card in your graveyard, " +
        "each opponent loses 2 life and you gain 2 life."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.mill(3) then ConditionalEffect(
            condition = Conditions.GraveyardContainsSubtype(Subtype.ELF),
            effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
                .then(Effects.GainLife(2))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75a97f69-f3dc-4d33-8008-c1f1a7c15a2f.jpg?1767732686"
    }
}
