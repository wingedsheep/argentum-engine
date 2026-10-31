package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cerulean Drake (M20 #53)
 * {1}{U}  Creature — Drake  1/1
 *
 * Flying
 * Protection from red
 * Sacrifice this creature: Counter target spell that targets you.
 *
 * "That targets you" is `targetsPlayer(Player.You)` on any spell on the stack (same shape as
 * Shell of the Last Kappa), read relative to the ability's controller; a spell with several
 * targets qualifies as long as one of them is you.
 */
val CeruleanDrake = card("Cerulean Drake") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    oracleText = "Flying\n" +
        "Protection from red (This creature can't be blocked, targeted, dealt damage, enchanted, or equipped by anything red.)\n" +
        "Sacrifice this creature: Counter target spell that targets you."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.protectionFrom(Color.RED))

    activatedAbility {
        cost = Costs.SacrificeSelf
        target(TargetFilter.SpellOnStack.targetsPlayer(Player.You))
        effect = Effects.CounterSpell()
        description = "Counter target spell that targets you."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b11f9e7d-3f56-4e28-9908-157735f6dfa9.jpg?1783933013"

        ruling("2019-07-12", "Cerulean Drake's activated ability can target a spell that has multiple targets if at least one of those targets is you.")
    }
}
