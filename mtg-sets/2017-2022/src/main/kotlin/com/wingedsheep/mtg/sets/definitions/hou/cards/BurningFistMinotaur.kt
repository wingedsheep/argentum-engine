package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val BurningFistMinotaur = card("Burning-Fist Minotaur") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Wizard"
    oracleText = "First strike\n{1}{R}, Discard a card: This creature gets +2/+0 until end of turn."
    power = 2
    toughness = 1

    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardCard)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Matt Stewart"
        flavorText = "\"There is no afterlife. Not for me, and not for you. So think again before getting in my way.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52f191fb-eaf6-432f-a668-37ad48ffabaf.jpg?1783936033"
    }
}
