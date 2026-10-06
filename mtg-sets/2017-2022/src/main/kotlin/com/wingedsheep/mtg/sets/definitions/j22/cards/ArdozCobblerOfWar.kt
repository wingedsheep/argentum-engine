package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ardoz, Cobbler of War
 * {1}{R}
 * Legendary Creature — Goblin Shaman
 * 1/1
 *
 * Haste
 * Whenever Ardoz or another creature you control enters, that creature gets +2/+0 until end of turn.
 * {3}{R}: Create a 1/1 red Goblin creature token with haste. Activate only as a sorcery.
 *
 * The inclusive "Ardoz or another creature you control enters" is `Triggers.a(...).enters()` over
 * `Creature.youControl()` (the ANY binding fires for the source itself too, as on Fallaji Vanguard),
 * and "that creature" is [EffectTarget.TriggeringEntity] — the entering creature, untargeted.
 */
val ArdozCobblerOfWar = card("Ardoz, Cobbler of War") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Shaman"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "Whenever Ardoz or another creature you control enters, that creature gets +2/+0 until end of turn.\n" +
        "{3}{R}: Create a 1/1 red Goblin creature token with haste. Activate only as a sorcery."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(2, 0, EffectTarget.TriggeringEntity)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            keywords = setOf(Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/e/d/ed418a8b-f158-492d-a323-6265b3175292.jpg?1562640121"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a012f05-0009-42cf-8a69-07140b2a2aef.jpg?1783919185"
    }
}
