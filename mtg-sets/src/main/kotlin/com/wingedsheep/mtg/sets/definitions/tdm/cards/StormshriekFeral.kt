package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Stormshriek Feral // Flush Out — Tarkir: Dragonstorm #124
 * {4}{R} · Creature — Dragon · 3/3
 *
 * Flying, haste
 * {1}{R}: This creature gets +1/+0 until end of turn.
 *
 * Omen: Flush Out — {1}{R}, Sorcery — Omen
 * Discard a card. If you do, draw two cards. (Then shuffle this card into its owner's library.)
 *
 * (Omen, Tarkir: Dragonstorm: casting the Omen face shuffles this card into its owner's library on
 * resolution instead of putting it in the graveyard. From every zone other than the stack the card
 * is just the Dragon — see [com.wingedsheep.sdk.model.CardLayout.OMEN].)
 *
 * "If you do" is modeled with [IfYouDoEffect]: the mandatory discard runs first, and the draw only
 * happens when a card was actually discarded (matters with an otherwise-empty hand).
 */
val StormshriekFeral = card("Stormshriek Feral") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Flying, haste\n{1}{R}: This creature gets +1/+0 until end of turn."

    keywords(Keyword.FLYING, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "{1}{R}: This creature gets +1/+0 until end of turn."
    }

    // Omen: Flush Out — Sorcery. Discard a card. If you do, draw two cards.
    omen("Flush Out") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery — Omen"
        oracleText = "Discard a card. If you do, draw two cards. " +
            "(Then shuffle this card into its owner's library.)"
        spell {
            effect = IfYouDoEffect(
                action = HandPatterns.discardCards(1),
                ifYouDo = Effects.DrawCards(2),
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ec92c44-7cf0-48a5-a3ca-bc633496d887.jpg?1743204459"
    }
}
