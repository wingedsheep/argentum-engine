package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Axebane Guardian — Return to Ravnica #115
 * {2}{G} · Creature — Human Druid · 0/3
 *
 * Defender
 * {T}: Add X mana in any combination of colors, where X is the number of creatures you control
 * with defender.
 *
 * The mana ability is the Selvala shape: `AddManaInAnyCombination` over a count of your
 * defender creatures, counted (on projected state) as the ability resolves. It counts itself.
 */
val AxebaneGuardian = card("Axebane Guardian") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 0
    toughness = 3
    oracleText = "Defender\n{T}: Add X mana in any combination of colors, where X is the number of " +
        "creatures you control with defender."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaInAnyCombination(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER),
            ).count()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Add X mana in any combination of colors, where X is the number of creatures you " +
            "control with defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Slawomir Maniak"
        flavorText = "Only a worthy few are permitted within the sentient forest known as Axebane."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/725584fe-9e97-4020-89b1-5e5b45a5beb2.jpg?1783940351"
        ruling(
            "2012-10-01",
            "Axebane Guardian's activated ability is a mana ability. It doesn't use the stack and can't be " +
                "responded to. The number of creatures with defender you control is counted, and the color(s) " +
                "of mana produced chosen, immediately after the ability is activated when the ability resolves.",
        )
    }
}
