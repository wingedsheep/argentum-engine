package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Xathrid Gorgon
 * {5}{B}
 * Creature — Gorgon
 * 3/6
 * Deathtouch
 * {2}{B}, {T}: Put a petrification counter on target creature. It gains defender and becomes a
 * colorless artifact in addition to its other types. Its activated abilities can't be activated.
 *
 * The counter is a pure marker (2012-07-01 ruling): every rider is its own permanent effect, so
 * removing the counter leaves the creature a colorless artifact with defender and no activations.
 */
val XathridGorgon = card("Xathrid Gorgon") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon"
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\n" +
        "{2}{B}, {T}: Put a petrification counter on target creature. It gains defender and becomes a " +
        "colorless artifact in addition to its other types. Its activated abilities can't be activated. " +
        "(A creature with defender can't attack.)"
    power = 3
    toughness = 6

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PETRIFICATION, 1, creature) then
            Effects.GrantKeyword(Keyword.DEFENDER, creature, Duration.Permanent) then
            Effects.AddCardType("ARTIFACT", creature, Duration.Permanent) then
            Effects.ChangeColor(creature, emptySet(), Duration.Permanent) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = creature,
                duration = Duration.Permanent,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e07524e0-303d-465d-b112-ca605b9b27fc.jpg?1783940488"
        ruling(
            "2016-06-08",
            "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keywords are activated abilities and will have colons in their reminder text."
        )
        ruling(
            "2012-07-01",
            "The effects of Xathrid Gorgon's ability don't depend on the petrification counter. If that counter is removed, the creature remains a colorless artifact with defender, and so on."
        )
    }
}
