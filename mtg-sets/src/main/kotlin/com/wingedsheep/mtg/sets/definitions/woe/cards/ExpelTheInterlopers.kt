package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Expel the Interlopers
 * {3}{W}{W}
 * Sorcery
 *
 * Choose a number between 0 and 10. Destroy all creatures with power greater than or equal to
 * the chosen number.
 *
 * "Choose a number" is [Effects.ChooseNumberThen] (bounded 0–10, per the Scryfall ruling that
 * both endpoints are legal choices), which stamps the chosen number onto the effect context as
 * X — the same machinery Void uses. The wipe then filters by
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.PowerAtLeastX] (`powerAtLeastX()`),
 * the "greater than or equal to" mirror of Zero Point Ballad's `toughnessAtMostX()`.
 *
 * Symmetrical and non-targeted, so it hits your own creatures too and ignores "can't be the
 * target of" protection. Power is read from projected state at resolution, so pumps and
 * -X/-X effects that resolved earlier count; choosing 0 destroys every creature whose power
 * isn't negative.
 */
val ExpelTheInterlopers = card("Expel the Interlopers") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose a number between 0 and 10. Destroy all creatures with power greater " +
        "than or equal to the chosen number."

    spell {
        effect = Effects.ChooseNumberThen(
            then = Effects.DestroyAll(filter = GameObjectFilter.Creature.powerAtLeastX()),
            minValue = 0,
            maxValue = 10,
            prompt = "Choose a number between 0 and 10",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Andreas Zafiratos"
        flavorText = "When a redcap raid threatened the Grand Ball at Delverhaugh, Goddric had " +
            "no choice but to reveal his true identity and douse the invaders in dragonfire."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1094eef0-6c57-4bfa-a584-f708b87354fb.jpg?1783915132"

        ruling("2023-09-01", "The numbers you may choose include 0 and 10.")
    }
}
