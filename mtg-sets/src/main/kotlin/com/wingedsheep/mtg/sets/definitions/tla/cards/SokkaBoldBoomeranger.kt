package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sokka, Bold Boomeranger — Avatar: The Last Airbender #240
 * {U}{R} · Legendary Creature — Human Warrior Ally · Rare
 * 1/1
 *
 * When Sokka enters, discard up to two cards, then draw that many cards.
 * Whenever you cast an artifact or Lesson spell, put a +1/+1 counter on Sokka.
 */
val SokkaBoldBoomeranger = card("Sokka, Bold Boomeranger") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Human Warrior Ally"
    power = 1
    toughness = 1
    oracleText = "When Sokka enters, discard up to two cards, then draw that many cards.\n" +
        "Whenever you cast an artifact or Lesson spell, put a +1/+1 counter on Sokka."

    // ETB loot run backwards: discard up to two, then draw that many (declining discards draws zero).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Hand.discardUpToThenDraw(2)
        description = "When Sokka enters, discard up to two cards, then draw that many cards."
    }

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.Artifact or GameObjectFilter.Any.withSubtype(Subtype.LESSON)
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cast an artifact or Lesson spell, put a +1/+1 counter on Sokka."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Toni Infante"
        flavorText = "\"I'm just a guy with a boomerang!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a1f1472-55b4-450d-8e4d-7297130a0cf3.jpg?1764121775"
    }
}
