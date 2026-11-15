package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silumgar's Command
 * {3}{U}{B}
 * Instant
 *
 * Choose two —
 * • Counter target noncreature spell.
 * • Return target permanent to its owner's hand.
 * • Target creature gets -3/-3 until end of turn.
 * • Destroy target planeswalker.
 *
 * "Choose two —" over four modes is `modal(chooseCount = 2)` — two *different* modes. Every mode
 * targets something different, so each declares its target inside its own mode (as Kolaghan's
 * Command does): only the chosen modes' requirements are asked for.
 */
val SilumgarsCommand = card("Silumgar's Command") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Choose two —\n" +
        "• Counter target noncreature spell.\n" +
        "• Return target permanent to its owner's hand.\n" +
        "• Target creature gets -3/-3 until end of turn.\n" +
        "• Destroy target planeswalker."

    spell {
        modal(chooseCount = 2) {
            mode("Counter target noncreature spell") {
                target(TargetFilter.NoncreatureSpellOnStack)
                effect = Effects.CounterSpell()
            }
            mode("Return target permanent to its owner's hand") {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.ReturnToHand(permanent)
            }
            mode("Target creature gets -3/-3 until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-3, -3, creature)
            }
            mode("Destroy target planeswalker") {
                val planeswalker = target(TargetFilter.Planeswalker)
                effect = Effects.Destroy(planeswalker)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba26dbbc-d4a2-44a1-8e6b-affe61f43a34.jpg"
    }
}
