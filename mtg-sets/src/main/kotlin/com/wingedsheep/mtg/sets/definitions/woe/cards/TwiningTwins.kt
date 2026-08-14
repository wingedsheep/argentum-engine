package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Twining Twins // Swift Spiral
 * {2}{U}{U}
 * Creature — Faerie Wizard
 * 4/4
 * Flying, vigilance, ward {1}
 *
 * Adventure: Swift Spiral — {1}{W}, Instant — Adventure
 * Exile target nontoken creature. Return it to the battlefield under its owner's control at the
 * beginning of the next end step.
 *
 * The Adventure is the plain blink pattern — [Patterns.Exile.exileUntilEndStep] moves the target
 * to exile and schedules a delayed trigger that returns it at the *next* end step (never the one
 * currently in progress). The return carries no controller override, so the card comes back under
 * its owner's control exactly as the oracle text says, and it returns as a new object with no
 * memory of counters, auras, or damage.
 *
 * The "nontoken" restriction is a real targeting restriction, not a resolution check: a token
 * exiled this way would cease to exist and never come back, so the card refuses to target one at
 * all (compare Parting Gust).
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val TwiningTwins = card("Twining Twins") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Faerie Wizard"
    oracleText = "Flying, vigilance, ward {1} (Whenever this creature becomes the target of a spell " +
        "or ability an opponent controls, counter it unless that player pays {1}.)"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.ward("{1}"))

    adventure("Swift Spiral") {
        manaCost = "{1}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Exile target nontoken creature. Return it to the battlefield under its " +
            "owner's control at the beginning of the next end step. " +
            "(Then exile this card. You may cast the creature later from exile.)"

        spell {
            val creature = target(
                "target nontoken creature",
                TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.nontoken())),
            )
            effect = Patterns.Exile.exileUntilEndStep(creature)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Fajareka Setiawan"
        flavorText = "\"We spin and spin to keep us busy, but somehow you're the one who's dizzy.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/4/043718ea-59f6-4d1a-94c5-271704c1a38a.jpg?1783915061"
    }
}
