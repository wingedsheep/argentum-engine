package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gingerbread Hunter // Puny Snack
 * {4}{G}
 * Creature — Giant
 * 5/5
 * When this creature enters, create a Food token.
 *
 * Adventure: Puny Snack — {2}{B}, Instant — Adventure
 * Target creature gets -2/-2 until end of turn.
 *
 * The Food token is the shared predefined artifact token behind [Effects.CreateFood]; Puny Snack
 * is a plain until-end-of-turn [Effects.ModifyStats] on any target creature (not just an
 * opponent's).
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val GingerbreadHunter = card("Gingerbread Hunter") {
    manaCost = "{4}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Giant"
    oracleText = "When this creature enters, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    adventure("Puny Snack") {
        manaCost = "{2}{B}"
        typeLine = "Instant — Adventure"
        oracleText = "Target creature gets -2/-2 until end of turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target", Targets.Creature)
            effect = Effects.ModifyStats(-2, -2, t)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e77a8fd4-af5f-42b3-a87e-788baf2562dd.jpg?1783915065"
    }
}
