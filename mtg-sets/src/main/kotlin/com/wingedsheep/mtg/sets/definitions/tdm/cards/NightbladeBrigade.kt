package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nightblade Brigade — Tarkir: Dragonstorm #85
 * {2}{B} · Creature — Goblin Soldier · 1/3
 *
 * Deathtouch
 * Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red Warrior
 * creature token. Sacrifice it at the beginning of the next end step.)
 * When this creature enters, surveil 1.
 *
 * Deathtouch and Mobilize 1 are keyword helpers (`mobilize(n)` adds the display keyword plus the
 * attack-triggered tapped-and-attacking Warrior token that's sacrificed at the next end step). The
 * enters-the-battlefield surveil 1 uses the atomic [EffectPatterns.surveil] composition.
 */
val NightbladeBrigade = card("Nightblade Brigade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Goblin Soldier"
    power = 1
    toughness = 3
    oracleText = "Deathtouch\n" +
        "Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red Warrior creature token. Sacrifice it at the beginning of the next end step.)\n" +
        "When this creature enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    keywords(Keyword.DEATHTOUCH)
    mobilize(1)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
        description = "When this creature enters, surveil 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Gary Laib"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/648debd9-d4cf-4788-8882-f1601a3d87f5.jpg?1743204303"
    }
}
