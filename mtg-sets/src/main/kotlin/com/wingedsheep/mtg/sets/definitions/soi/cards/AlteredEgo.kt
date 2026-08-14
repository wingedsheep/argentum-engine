package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Altered Ego (Shadows over Innistrad #241 — the card's earliest printing; also reprinted in
 * Shadows over Innistrad Remastered and Innistrad Remastered)
 * {X}{2}{G}{U}
 * Creature — Shapeshifter 0/0
 *
 * This spell can't be countered.
 * You may have this creature enter as a copy of any creature on the battlefield, except it enters
 * with X additional +1/+1 counters on it.
 *
 * Implementation:
 *  - "This spell can't be countered" is the `cantBeCountered` card flag (stamped on the spell at
 *    cast time), not a granted static — it protects only this spell.
 *  - The copy is a plain [EntersAsCopy] over creatures on the battlefield, with the "except it
 *    enters with X additional +1/+1 counters" rider carried by `additionalCounters` =
 *    [DynamicAmount.XValue]. The rider belongs to the copy effect rather than a separate
 *    enters-with-counters replacement, which is what makes the printed rulings fall out for free:
 *    declining the copy also declines the counters ("It won't have +1/+1 counters placed on it by
 *    its ability"), and X = 0 is simply a straight copy.
 */
val AlteredEgo = card("Altered Ego") {
    manaCost = "{X}{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "This spell can't be countered.\n" +
        "You may have this creature enter as a copy of any creature on the battlefield, except it " +
        "enters with X additional +1/+1 counters on it."

    cantBeCountered = true

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature,
            additionalCounters = DynamicAmount.XValue,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c05da08d-8fac-47bc-80d8-78a80d1463d2.jpg?1783937715"
        ruling(
            "2025-01-24",
            "Altered Ego copies exactly what was printed on the original creature (unless that " +
                "creature is copying something else or is a token; see below). It doesn't copy " +
                "whether that creature is tapped or untapped, whether it has any counters on it or " +
                "any Auras and Equipment attached to it, or any non-copy effects that have changed " +
                "its power, toughness, types, color, or so on."
        )
        ruling(
            "2025-01-24",
            "Any \"enters\" abilities of the copied creature will trigger when Altered Ego enters " +
                "the battlefield. Any \"as [this creature] enters\" or \"[this creature] enters " +
                "with\" abilities of the chosen creature will also work."
        )
        ruling(
            "2025-01-24",
            "If the chosen creature is a token, Altered Ego copies the original characteristics of " +
                "that token as stated by the effect that created that token. Altered Ego isn't a token."
        )
        ruling(
            "2025-01-24",
            "If Altered Ego somehow enters at the same time as another creature, Altered Ego can't " +
                "become a copy of that creature. You may choose only a creature that's already on " +
                "the battlefield."
        )
        ruling(
            "2025-01-24",
            "If the chosen creature has {X} in its mana cost, that X is considered to be 0. The " +
                "value of X in Altered Ego's last ability will be whatever value was chosen for X " +
                "while casting Altered Ego."
        )
        ruling(
            "2025-01-24",
            "You can choose not to copy anything. In that case, Altered Ego enters as a 0/0 " +
                "creature and is probably put into the graveyard immediately. It won't have +1/+1 " +
                "counters placed on it by its ability."
        )
        ruling(
            "2025-01-24",
            "X can be 0. Altered Ego won't enter with any additional +1/+1 counters, and it will " +
                "just be a copy of the chosen creature."
        )
    }
}
