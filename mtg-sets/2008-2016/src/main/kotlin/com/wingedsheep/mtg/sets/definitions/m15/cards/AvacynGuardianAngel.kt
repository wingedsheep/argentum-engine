package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Avacyn, Guardian Angel — Magic 2015 #3 (canonical printing)
 * {2}{W}{W}{W} · Legendary Creature — Angel · 5/4
 *
 * Flying, vigilance
 * {1}{W}: Prevent all damage that would be dealt to another target creature this turn by sources
 * of the color of your choice.
 * {5}{W}{W}: Prevent all damage that would be dealt to target player or planeswalker this turn by
 * sources of the color of your choice.
 *
 * Each ability chooses its color as it resolves (`ChooseColorThen`); the shield pins that color,
 * while a source's color is read only when its damage would be dealt.
 */
private val ofChosenColor = PreventionSourceFilter.Matching(GameObjectFilter.Any.withChosenColor())

val AvacynGuardianAngel = card("Avacyn, Guardian Angel") {
    manaCost = "{2}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Angel"
    power = 5
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "{1}{W}: Prevent all damage that would be dealt to another target creature this turn by sources of the color of your choice.\n" +
        "{5}{W}{W}: Prevent all damage that would be dealt to target player or planeswalker this turn by sources of the color of your choice."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val t = target(TargetFilter.OtherCreature)
        effect = Effects.ChooseColorThen(Effects.PreventDamage(target = t, sources = ofChosenColor))
    }

    activatedAbility {
        cost = Costs.Mana("{5}{W}{W}")
        val t = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.ChooseColorThen(Effects.PreventDamage(target = t, sources = ofChosenColor))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a79662d5-3b6f-4c5a-8540-63c201027b66.jpg?1783939204"
        ruling("2014-07-18", "Neither activated ability has any effect on damage that's already been dealt.")
        ruling("2014-07-18", "The possible color choices are white, blue, black, red, and green. You can't choose artifact, colorless, or any other word.")
        ruling("2014-07-18", "For each activated ability, you choose the color as the ability resolves. However, the color of a source that would deal damage to the permanent or player is checked only when it would deal that damage, not as Avacyn's ability resolves. For example, if you choose blue, damage a blue creature would deal is prevented, even if that creature wasn't blue (or wasn't on the battlefield) when Avacyn's ability resolved.")
        ruling("2014-07-18", "Whether the target permanent or player is still a legal target is no longer checked after the appropriate ability resolves. For example, if a creature targeted by the first ability gains protection from white after the ability resolves but before it prevents damage, the effect will still prevent damage.")
        ruling("2014-07-18", "Both activated abilities prevent all damage of the chosen color, not just combat damage.")
    }
}
