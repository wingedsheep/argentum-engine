package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bubble Smuggler — Murders at Karlov Manor #41
 * {1}{U} · Creature — Octopus Fish · 2/1
 *
 * Disguise {5}{U}
 * As this creature is turned face up, put four +1/+1 counters on it.
 *
 * A {3} 2/2 with ward {2} early that later unfolds into a 6/5 for {5}{U}. The counters make it a
 * genuine 6/5 rather than a pumped 2/1, so they survive the turn and stack with anything else.
 *
 * **"As … is turned face up" is a replacement, not a trigger.** It applies as part of the special
 * action that flips the permanent, so it doesn't use the stack and can't be responded to: an
 * opponent holding a 4-damage burn spell never gets a window against the 2/1 body. That is the
 * whole difference between this and the `Triggers.TurnedFaceUp` shape used by [GraniteWitness] and
 * [ExitSpecialist] — modelling it as a triggered ability would hand the opponent a response window
 * the card doesn't give them.
 *
 * It is carried by `disguiseFaceUpEffect`, the disguise-side sibling of `morphFaceUpEffect`
 * (Hooded Hydra). Both ride the *turn-up procedure*, matching CR 702.37b's treatment of megamorph.
 * Known limitation, inherited from the morph model: if this card were put onto the battlefield face
 * down by cloak or manifest and then turned face up by paying its mana cost instead of its disguise
 * cost, the counters would not be placed. Flipping it for the disguise cost — the only route MKM
 * itself offers — is correct.
 */
val BubbleSmuggler = card("Bubble Smuggler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus Fish"
    oracleText = "Disguise {5}{U} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "As this creature is turned face up, put four +1/+1 counters on it."
    power = 2
    toughness = 1

    disguise = "{5}{U}"
    disguiseFaceUpEffect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 4, EffectTarget.Self)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Leesha Hannigan"
        flavorText = "By the time they noticed the missing vials, Glovax was three fathoms away."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b863ee0-d9f3-4b1e-993d-5212731d9353.jpg?1783912915"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
