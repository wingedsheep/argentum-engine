package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * Phantom Interference
 * {U}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {3} — Create a 2/2 white Spirit creature token with flying.
 * + {1} — Counter target spell unless its controller pays {2}.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`, per-mode
 * `additionalManaCost`, and `allowRepeat = false` (CR 700.2 / OTJ release notes).
 *
 * Mode 1 (token) doesn't target — [Effects.CreateToken] of a 2/2 white flying Spirit.
 * Mode 2 (counter) targets a spell ([Targets.Spell]); [Effects.CounterUnlessPays] builds a
 * `CounterEffect` whose `CounterTarget.Spell` reads the mode's chosen `ContextTarget(0)`
 * (see Complicate / Explosive Derailment). When only the counter mode is chosen the spell
 * needs a legal target on the stack to be cast at all (CR 601.2c); choosing only the token
 * mode lets it be cast with no spell on the stack.
 */
val PhantomInterference = card("Phantom Interference") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {3} — Create a 2/2 white Spirit creature token with flying.\n" +
        "+ {1} — Counter target spell unless its controller pays {2}."

    spell {
        effect = ModalEffect(
            modes = listOf(
                // + {3} — Create a 2/2 white Spirit creature token with flying.
                Mode(
                    effect = Effects.CreateToken(
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.WHITE),
                        creatureTypes = setOf("Spirit"),
                        keywords = setOf(Keyword.FLYING),
                        count = 1,
                        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c15b18b-1ab1-44e9-b96f-c36d52f11ea0.jpg?1712316119"
                    ),
                    description = "+ {3} — Create a 2/2 white Spirit creature token with flying.",
                    additionalManaCost = "{3}"
                ),
                // + {1} — Counter target spell unless its controller pays {2}.
                Mode(
                    effect = Effects.CounterUnlessPays("{2}"),
                    targetRequirements = listOf(Targets.Spell),
                    description = "+ {1} — Counter target spell unless its controller pays {2}.",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Ruxing Gao"
        flavorText = "To his enemies' chagrin, Winston was every bit as meddlesome dead as he had been alive."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00bf4dd1-5468-4594-9c7b-0737610f19d4.jpg?1712860598"
    }
}
