package com.wingedsheep.mtg.sets.definitions.pls.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dromar's Charm
 * {W}{U}{B}
 * Instant
 *
 * Choose one —
 * • You gain 5 life.
 * • Counter target spell.
 * • Target creature gets -2/-2 until end of turn.
 *
 * A plain choose-one `modal(chooseCount = 1)`. The life mode is a targetless
 * [Effects.GainLife] (its default target is already the controller); the counter mode is
 * [Effects.CounterSpell] over the unfiltered [Targets.Spell]; the shrink mode is
 * [Effects.ModifyStats]`(-2, -2)` on a [TargetCreature], which defaults to end of turn.
 */
val DromarSCharm = card("Dromar's Charm") {
    manaCost = "{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• You gain 5 life.\n" +
        "• Counter target spell.\n" +
        "• Target creature gets -2/-2 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("You gain 5 life") {
                effect = Effects.GainLife(5)
            }
            mode("Counter target spell") {
                target("target", Targets.Spell)
                effect = Effects.CounterSpell()
            }
            mode("Target creature gets -2/-2 until end of turn") {
                val t = target("target", TargetCreature())
                effect = Effects.ModifyStats(-2, -2, t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7a1894c-af4e-4530-960f-2225916be8cb.jpg"
    }
}
