package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Highly Illogical — Star Trek #62
 * {1}{U} · Instant
 *
 * The kicked clause replaces the target restriction (targets are chosen with the kicker
 * decision already made), so it rides the `kickerTarget` / `kickerEffect` branch like
 * Bloodchief's Thirst: unkicked targets a spell with mana value 2 or less, kicked targets
 * any spell.
 */
val HighlyIllogical = card("Highly Illogical") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Kicker {1}{U} (You may pay an additional {1}{U} as you cast this spell.)\n" +
        "Counter target spell with mana value 2 or less. If this spell was kicked, instead counter target spell."

    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    spell {
        target(TargetFilter.SpellOnStack.manaValueAtMost(2))
        effect = Effects.CounterSpell()

        kickerTarget(TargetFilter.SpellOnStack)
        kickerEffect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Joel Chaim Holtzman"
        flavorText = "For the briefest of moments, Spock felt fear. Not of the rabbit, but that the universe " +
            "could let it exist without logical cause."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11c94c5f-abf4-429e-8703-c5db9abd79bf.jpg?1784065440"
    }
}
