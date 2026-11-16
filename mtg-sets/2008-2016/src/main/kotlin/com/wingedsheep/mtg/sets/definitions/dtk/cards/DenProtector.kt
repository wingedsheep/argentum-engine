package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Den Protector
 * {1}{G}
 * Creature — Human Warrior
 * 2/1
 * Creatures with power less than this creature's power can't block it.
 * Megamorph {1}{G}
 * When this creature is turned face up, return target card from your graveyard to your hand.
 *
 * The evasion clause is the attacker-side static [CantBeBlockedByCreaturesWithLessPower], compared on
 * projected power only as blockers are declared (the printed ruling). While face down the creature
 * has no printed abilities, so the restriction is off until it is turned face up.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on the
 * creature when it is turned face up for its megamorph cost. The return trigger is a separate
 * "turned face up" trigger, so it also fires when the creature is turned face up some other way.
 */
val DenProtector = card("Den Protector") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Creatures with power less than this creature's power can't block it.\n" +
        "Megamorph {1}{G} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, return target card from your graveyard to your hand."

    staticAbility {
        ability = CantBeBlockedByCreaturesWithLessPower()
    }

    // Megamorph {1}{G}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{1}{G}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val card = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.ReturnToHand(card)
        description = "When this creature is turned face up, return target card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd90d5ea-c586-4afd-be75-1c821e48df0c.jpg?1783938581"
        ruling(
            "2015-02-25",
            "You compare Den Protector's power to the power of any creature trying to block it only as " +
                "blockers are assigned. Once Den Protector has been legally blocked by a creature, changing " +
                "the power of either creature won't change or undo the block.",
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
