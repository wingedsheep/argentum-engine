package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flameblade Adept
 * {R}
 * Creature — Jackal Warrior
 * 1/2
 * Menace
 * Whenever you cycle or discard a card, this creature gets +1/+0 until end of turn.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, and wiring `cycles()` as well would
 * double-trigger against the ruling.
 */
val FlamebladeAdept = card("Flameblade Adept") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Jackal Warrior"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever you cycle or discard a card, this creature gets +1/+0 until end of turn."
    power = 1
    toughness = 2

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Whenever you cycle or discard a card, this creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Allow zealous flames to light your path and the glory you seek will be revealed.\"\n—Hazoret, god of zeal"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f974d05a-3b31-4661-af18-58d87b76f19e.jpg?1783936489"
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
