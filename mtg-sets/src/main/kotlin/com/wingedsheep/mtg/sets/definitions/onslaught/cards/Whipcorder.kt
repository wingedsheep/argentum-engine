package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Whipcorder
 * {W}{W}
 * Creature — Human Soldier Rebel
 * 2/2
 * {W}, {T}: Tap target creature.
 * Morph {W}
 */
val Whipcorder = card("Whipcorder") {
    manaCost = "{W}{W}"
    typeLine = "Creature — Human Soldier Rebel"
    power = 2
    toughness = 2
    oracleText = "{W}, {T}: Tap target creature.\nMorph {W}"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val t = target("target", TargetCreature())
        effect = TapUntapEffect(
            target = t,
            tap = true
        )
    }

    morph = "{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Ron Spencer"
        flavorText = "\"The bigger they are, the harder they fall.\""
        imageUri = "https://cards.scryfall.io/large/front/3/b/3bf6987e-a6e4-4a88-af0b-cf3b2d2b80c7.jpg?1562909065"
    }
}
