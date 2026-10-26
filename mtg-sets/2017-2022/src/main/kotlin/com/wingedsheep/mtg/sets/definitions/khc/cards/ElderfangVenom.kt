package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elderfang Venom
 * {2}{B}{G}
 * Enchantment
 * Attacking Elves you control have deathtouch.
 * Whenever an Elf you control dies, each opponent loses 1 life and you gain 1 life.
 *
 * "An Elf you control" is any Elf permanent (a noncreature Elf — a Kindred permanent — dies too),
 * so the trigger filters on Permanent rather than Creature.
 */
val ElderfangVenom = card("Elderfang Venom") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment"
    oracleText = "Attacking Elves you control have deathtouch.\n" +
        "Whenever an Elf you control dies, each opponent loses 1 life and you gain 1 life."

    staticAbility {
        ability = GrantKeyword(
            Keyword.DEATHTOUCH,
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.ELF).attacking().youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.ELF).youControl()).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Alessandra Pisano"
        flavorText = "\"The Cosmos Serpent favored us with his venom. As thanks, we sink fangs into those who dishonor the old ways.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d5bbb02-a04e-4add-ae9a-67819cffdb3a.jpg?1783928335"
    }
}
