package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reptilian Reflection
 * {2}{R}
 * Enchantment
 * Whenever you cycle a card, you may have this enchantment become a 5/4 Dinosaur creature with
 * trample and haste in addition to its other types until end of turn.
 *
 * "In addition to its other types" is [Effects.BecomeCreature]'s default: CREATURE is added and the
 * Enchantment type stays.
 */
val ReptilianReflection = card("Reptilian Reflection") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cycle a card, you may have this enchantment become a 5/4 Dinosaur " +
        "creature with trample and haste in addition to its other types until end of turn."

    triggeredAbility {
        trigger = Triggers.you.cycles()
        optional = true
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 4,
            keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
            creatureTypes = setOf("Dinosaur"),
            duration = Duration.EndOfTurn
        )
        description = "Whenever you cycle a card, you may have this enchantment become a 5/4 Dinosaur " +
            "creature with trample and haste in addition to its other types until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Antonio José Manzanedo"
        flavorText = "Danger often lurks in the allure of crystal shards."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7871b4dd-9085-4a3f-a1ae-9a292f73689b.jpg?1783931045"
        ruling("2020-04-17", "Some cards with cycling have an ability that triggers when you cycle them, and some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
    }
}
