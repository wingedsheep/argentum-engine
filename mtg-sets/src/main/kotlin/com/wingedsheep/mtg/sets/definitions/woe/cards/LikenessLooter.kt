package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Likeness Looter {U}{B}
 * Creature — Faerie Shapeshifter
 * 1/1
 *
 * Flying
 * {T}: Draw a card, then discard a card.
 * {X}: This creature becomes a copy of target creature card in your graveyard with mana value X,
 * except it has flying and this ability. Activate only as a sorcery.
 *
 * The copy ability is the Rydia, Summoner of Mist shape — an `{X}` activation whose chosen X
 * threads into the target filter via `manaValueEqualsX()`, so only a graveyard creature whose mana
 * value is exactly X is legal. `sourceFromAnyZone` lets the copy source stay in the graveyard
 * (CR 707.2 reads its copiable characteristics wherever it is), and `affected = Self` narrows the
 * usual mass-copy to this one permanent.
 *
 * The "except" clause is both copy-exception riders at once (CR 707.9): `CopyExceptions.addedKeywords`
 * re-adds flying on top of whatever the copied card has, and `retainActivatingAbility` re-grants this very
 * ability — without it the copy would replace the card component wholesale and the permanent could
 * never be re-aimed at a different graveyard card.
 *
 * Per the rulings the copy takes only what was printed on the original card, so no
 * enters-the-battlefield ability of the copied card applies (nothing is entering), and counters
 * and effects already on this creature carry over untouched — all of which follows from copying
 * only the `CardComponent`.
 */
val LikenessLooter = card("Likeness Looter") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Faerie Shapeshifter"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "{T}: Draw a card, then discard a card.\n" +
        "{X}: This creature becomes a copy of target creature card in your graveyard with mana " +
        "value X, except it has flying and this ability. Activate only as a sorcery."
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Hand.loot()
    }

    activatedAbility {
        cost = Costs.Mana("{X}")
        val creatureCard = target(
            "target creature card in your graveyard with mana value X",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Creature.ownedByYou().manaValueEqualsX(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = EachPermanentBecomesCopyOfTargetEffect(
            target = creatureCard,
            affected = EffectTarget.Self,
            sourceFromAnyZone = true,
            duration = Duration.Permanent,
            exceptions = CopyExceptions(addedKeywords = setOf(Keyword.FLYING)),
            retainActivatingAbility = true,
        )
        timing = TimingRule.SorcerySpeed
        description = "{X}: This creature becomes a copy of target creature card in your graveyard " +
            "with mana value X, except it has flying and this ability. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Ben Hill"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2957472a-825e-4904-b7e8-62bef1cb432d.jpg?1783915070"
        ruling(
            "2023-09-01",
            "Likeness Looter copies exactly what was printed on the original card and nothing else, " +
                "except the characteristics it specifically modifies. It doesn't copy any information " +
                "about the object the card was before it was put into your graveyard."
        )
        ruling(
            "2023-09-01",
            "Any effects that applied to Likeness Looter before it becomes a copy of another card will " +
                "continue to apply after it becomes a copy. The same is true of any counters that are on " +
                "Likeness Looter."
        )
        ruling("2023-09-01", "If a card in your graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2023-09-01",
            "Because Likeness Looter isn't entering the battlefield when it becomes a copy of a card, any " +
                "\"When [this creature] enters the battlefield\" or \"[This creature] enters the battlefield " +
                "with\" abilities of the copied card won't apply."
        )
        ruling(
            "2023-09-01",
            "If the copied card has an ability that can be activated only once each turn, copying that card " +
                "a second time will allow you to activate the new instance of that ability."
        )
    }
}
