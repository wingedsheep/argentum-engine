package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bant Charm
 * {G}{W}{U}
 * Instant
 * Choose one —
 * • Destroy target artifact.
 * • Put target creature on the bottom of its owner's library.
 * • Counter target instant spell.
 *
 * The Alara charm cycle's `modal(chooseCount = 1)` shape: each mode owns its own target, so only
 * the chosen mode's target is picked. Mode two is Proteus Staff's [Effects.PutOnBottomOfLibrary];
 * mode three is Flash Counter's [Effects.CounterSpell] over an instant spell on the stack.
 */
val BantCharm = card("Bant Charm") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GUW"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target artifact.\n" +
        "• Put target creature on the bottom of its owner's library.\n" +
        "• Counter target instant spell."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target artifact") {
                val t = target(TargetFilter.Artifact)
                effect = Effects.Destroy(t)
            }
            mode("Put target creature on the bottom of its owner's library") {
                val t = target(TargetFilter.Creature)
                effect = Effects.PutOnBottomOfLibrary(t)
            }
            mode("Counter target instant spell") {
                target(TargetFilter.InstantSpellOnStack)
                effect = Effects.CounterSpell()
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Randy Gallegos"
        flavorText = "Bant is a world where death and chaos hold no sway."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65b65c87-b084-44aa-b841-411a3c73e234.jpg?1783942548"
        ruling("2008-10-01", "You can choose a mode only if you can choose legal targets for that mode. If you can't choose legal targets for any of the modes, you can't cast the spell.")
    }
}
