package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TotalCap

/**
 * Nethroi, Apex of Death
 * {2}{W}{B}{G}
 * Legendary Creature — Cat Nightmare Beast
 * 5/5
 * Mutate {4}{G/W}{B}{B}
 * Deathtouch, lifelink
 * Whenever this creature mutates, return any number of target creature cards with total power 10
 * or less from your graveyard to the battlefield.
 *
 * "Total power 10 or less" is the targeting cap `TotalCap.power`: the chosen cards' powers as they
 * are in the graveyard are summed when targets are chosen and again on resolution, where an
 * over-cap set makes every target illegal (rulings).
 */
val NethroiApexOfDeath = card("Nethroi, Apex of Death") {
    manaCost = "{2}{W}{B}{G}"
    colorIdentity = "BGW"
    typeLine = "Legendary Creature — Cat Nightmare Beast"
    power = 5
    toughness = 5
    oracleText = "Mutate {4}{G/W}{B}{B} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Deathtouch, lifelink\n" +
        "Whenever this creature mutates, return any number of target creature cards with total power 10 or less from your graveyard to the battlefield."

    keywordAbility(KeywordAbility.mutate("{4}{G/W}{B}{B}"))
    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        targets(
            TargetFilter.CreatureInYourGraveyard,
            unlimited = true,
            totalAtMost = TotalCap.power(DynamicAmounts.fixed(10)),
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ca6eb5a-8bc9-4091-bcfb-b207f0afd188.jpg?1783931020"
        ruling("2020-04-17", "Nethroi's triggered ability can return any combination of creatures whose powers total 10 or less. For example, it could target ten 1/1 creature cards, two 5/5 creature cards, and so on. Regardless of what other creature cards you choose as targets, you can also choose any number of creature cards with power 0 as targets.")
        ruling("2020-04-17", "Use the power of the creature cards as they exist in your graveyard to determine whether they can be the targets of Nethroi's triggered ability. For example, a 0/0 creature card that will enter the battlefield as a copy of a 15/15 creature can be returned, as can five 0/0 creature cards that will each enter the battlefield with three +1/+1 counters on them.")
        ruling("2020-04-17", "If the power of the target creature cards change while in your graveyard, most likely because one has a characteristic-defining ability that defines a * in its power, the total power of the creatures may become greater than 10. If this happens, the entire selection of targets is illegal and nothing is returned to the battlefield.")
    }
}
