package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shaman of the Pack — Magic Origins #217
 * {1}{B}{G} · Creature — Elf Shaman · 3/2
 *
 * When this creature enters, target opponent loses life equal to the number of Elves you control.
 *
 * The Elves are counted as the trigger resolves, Shaman of the Pack included if it's still on
 * the battlefield (ruling).
 */
val ShamanOfThePack = card("Shaman of the Pack") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Shaman"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target opponent loses life equal to the number of Elves you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype(Subtype.ELF)).count(),
            opponent,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Dan Murayama Scott"
        flavorText = "To the elves, her spear is a compass; to the boggarts, a harbinger of doom."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/304687b1-2294-4144-b2bd-7e36f9aaac34.jpg?1783938313"
        ruling("2016-06-08", "Count the number of Elves you control as Shaman of the Pack's ability resolves, including Shaman of the Pack if it's still on the battlefield, to determine how much life is lost.")
    }
}
