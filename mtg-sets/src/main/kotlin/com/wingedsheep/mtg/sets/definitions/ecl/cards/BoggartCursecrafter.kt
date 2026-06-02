package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boggart Cursecrafter
 * {B}{R}
 * Creature — Goblin Warlock
 * 2/3
 *
 * Deathtouch
 * Whenever another Goblin you control dies, this creature deals 1 damage to each opponent.
 */
val BoggartCursecrafter = card("Boggart Cursecrafter") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Goblin Warlock"
    power = 2
    toughness = 3
    oracleText = "Deathtouch\n" +
        "Whenever another Goblin you control dies, this creature deals 1 damage to each opponent."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withSubtype("Goblin"),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Alex Stone"
        flavorText = "\"This bone is a good bone. Very angry. I can make lots of pain with this one.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0b67eb9-0d88-4f2c-8063-e8bedfa78556.jpg?1767952311"
        ruling(
            "2025-11-17",
            "If Boggart Cursecrafter dies at the same time as one or more other Goblins you control, " +
                "its last ability will trigger for each of those other Goblins."
        )
    }
}
