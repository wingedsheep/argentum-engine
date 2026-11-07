package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hungry Megasloth — Foundations Jumpstart #21
 * {2}{G} · Creature — Sloth Beast · 3/3
 *
 * Reach
 * {2}, {T}: Put a +1/+1 counter on this creature.
 */
val HungryMegasloth = card("Hungry Megasloth") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Sloth Beast"
    power = 3
    toughness = 3
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "{2}, {T}: Put a +1/+1 counter on this creature."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Svetlin Velinov"
        flavorText = "It supplements its leafy diet with delicacies snatched from low-flying airships."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/455c92ca-bb23-495d-be9e-2e0101b115ac.jpg?1783908864"
    }
}
