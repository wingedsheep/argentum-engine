package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice

/**
 * Extinction Event — Ikoria: Lair of Behemoths #88 (canonical printing)
 * {3}{B} · Sorcery
 *
 * Choose odd or even. Exile each creature with mana value of the chosen quality. (Zero is even.)
 *
 * The parity is chosen **while the spell resolves** (the 2020-04-17 ruling), so it is a
 * resolution-time [Effects.ChooseAction] rather than a cast-time `modal { }` block — opponents
 * can't respond to the choice. Each option is the Gather -> exile sweep Farewell uses, narrowed by
 * the SDK's parity predicates; "Zero is even" is what `manaValueIsEven()` already answers for a
 * token or an {X} creature on the battlefield (X is 0 there).
 */
val ExtinctionEvent = card("Extinction Event") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose odd or even. Exile each creature with mana value of the chosen quality. (Zero is even.)"

    spell {
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    label = "Odd — Exile each creature with an odd mana value",
                    effect = Effects.Pipeline {
                        exile(gather(GameObjectFilter.Creature.manaValueIsOdd()))
                    }
                ),
                EffectChoice(
                    label = "Even — Exile each creature with an even mana value",
                    effect = Effects.Pipeline {
                        exile(gather(GameObjectFilter.Creature.manaValueIsEven()))
                    }
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Filip Burburan"
        flavorText = "\"Nature is more merciless than any hunter I've ever known.\"\n—Zai, nightsquad captain"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8725a869-462b-4381-880a-b4bcc63a655b.jpg?1783931062"

        ruling(
            "2020-04-17",
            "You choose odd or even while Extinction Event is resolving. Players can't take any actions " +
                "between the time you make that choice and the time some creatures go extinct."
        )
        ruling("2020-04-17", "If a creature on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2020-04-17",
            "The mana value of a token that isn't a copy of another object is 0. A token that is a copy of " +
                "another object has the same mana cost as that object."
        )
    }
}
