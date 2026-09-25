package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Soilshaper
 * {1}{G}
 * Creature — Spirit
 * 1/1
 * Whenever you cast a Spirit or Arcane spell, target land becomes a 3/3 creature until end of
 * turn. It's still a land.
 *
 * "Target land" is any land — the opponent's included. The animation adds the creature type and
 * sets base P/T 3/3 until end of turn while keeping the land type (CR 205.1b), so the land keeps
 * its mana abilities; attacking or tapping it for mana still obeys summoning sickness (Scryfall
 * ruling 2008-08-01).
 */
val Soilshaper = card("Soilshaper") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    oracleText = "Whenever you cast a Spirit or Arcane spell, target land becomes a 3/3 creature " +
        "until end of turn. It's still a land."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withAnySubtype("Spirit", "Arcane"))
        val land = target(TargetFilter.Land)
        effect = Effects.BecomeCreature(target = land, power = 3, toughness = 3)
        description = "Whenever you cast a Spirit or Arcane spell, target land becomes a 3/3 " +
            "creature until end of turn. It's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Thomas M. Baxa"
        flavorText = "It spoke with the voice of nature, but its words were curses spat upon humankind."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae4066c3-bcb0-41c4-a4d3-a22bd186fd13.jpg?1783944282"
        ruling("2008-08-01", "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be activated, only if its controller has continuously controlled that permanent since the beginning of their most recent turn. It doesn't matter how long the permanent has been a creature.")
    }
}
