package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dazzling Ramparts
 * {4}{W}
 * Creature — Wall
 * 0/7
 * Defender
 * {1}{W}, {T}: Tap target creature.
 */
val DazzlingRamparts = card("Dazzling Ramparts") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 7
    oracleText = "Defender\n{1}{W}, {T}: Tap target creature."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val t = target("target", TargetCreature())
        effect = TapUntapEffect(
            target = t,
            tap = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Jung Park"
        flavorText = "\"When Anafenza holds court under the First Tree, the gates of Mer-Ek are sealed. No safer place exists in all of Tarkir.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce43a2df-4305-4ab1-ae45-96cca597650e.jpg?1562793747"
    }
}
