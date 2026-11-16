package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silumgar Assassin
 * {1}{B}
 * Creature — Human Assassin
 * 2/1
 * Creatures with power greater than this creature's power can't block it.
 * Megamorph {2}{B}
 * When this creature is turned face up, destroy target creature with power 3 or less an opponent controls.
 *
 * The evasion is a printed `CantBeBlockedBy` comparing each blocker's power with this creature's
 * projected power as blockers are declared. It is not skulk (no keyword), and like every printed
 * ability it is off while the Assassin is a face-down 2/2.
 */
val SilumgarAssassin = card("Silumgar Assassin") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    power = 2
    toughness = 1
    oracleText = "Creatures with power greater than this creature's power can't block it.\n" +
        "Megamorph {2}{B} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, destroy target creature with power 3 or less " +
        "an opponent controls."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerGreaterThanEntity(EffectTarget.Self))
    }

    // Megamorph {2}{B}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{2}{B}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val creature = target(TargetFilter.Creature.powerAtMost(3).opponentControls())
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Yohann Schepacz"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecc4693f-59fe-4671-b8c6-66fe53a76bbe.jpg?1783938593"
        ruling(
            "2015-02-25",
            "The powers of Silumgar Assassin and the creature trying to block it are checked only as " +
                "blockers are declared. Changing the powers of either of those creatures after Silumgar " +
                "Assassin is legally blocked won't change or undo the block.",
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
