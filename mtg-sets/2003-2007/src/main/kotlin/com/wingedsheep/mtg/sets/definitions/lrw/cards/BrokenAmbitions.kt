package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.values.DynamicAmount

val BrokenAmbitions = card("Broken Ambitions") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {X}. Clash with an opponent. If you win, that spell's controller mills four cards. (Each clashing player reveals the top card of their library, then puts that card on their choice of the top or bottom. A player wins if their card had a greater mana value.)"

    spell {
        target("target spell", Targets.Spell)
        effect = Effects.Pipeline {
            val spell = gather(CardSource.ChosenTargets, name = "ambitionsSpell")
            val controllers = captureControllers(spell, name = "ambitionsControllers")
            run(Effects.CounterUnlessDynamicPays(DynamicAmount.XValue))
            run(Patterns.Mechanic.clash(ifYouWin = Effects.Pipeline {
                // The rider applies even when payment or an ability prevents the counter.
                forEachCaptured(spell, spell, controllers) {
                    run(Patterns.Library.mill(4))
                }
            }))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8052d90b-bc49-4a9e-9211-159a54aa2bcd.jpg?1783942905"
        ruling("2007-10-01", "The opponent you clash with doesn't have to be the controller of the targeted spell.")
        ruling("2007-10-01", "The clash (and the result of the clash if you win) happens regardless of whether the targeted spell was countered or {X} was paid.")
    }
}
