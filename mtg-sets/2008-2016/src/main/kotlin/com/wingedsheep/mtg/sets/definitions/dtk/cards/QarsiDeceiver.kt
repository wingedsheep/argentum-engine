package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Qarsi Deceiver — Dragons of Tarkir #71
 * {1}{U} · Creature — Snake Wizard · 0/4
 *
 * {T}: Add {C}. Spend this mana only to cast a face-down creature spell, pay a mana cost to turn a
 * manifested creature face up, or pay a morph cost. (A megamorph cost is a morph cost.)
 *
 * The spend clause is two [ManaRestriction.AnyOf] atoms: [ManaRestriction.FaceDownSpellsOnly]
 * (a morph or disguise card cast face down — not a spell that tells you to manifest, per the
 * ruling) and [ManaRestriction.TurnPermanentsFaceUpOnly] narrowed to the manifest procedure ("pay
 * its mana cost", CR 701.40b — manifest dread manifests too; cloak's procedure is its own) and the
 * morph procedure, which a manifested card that prints morph may also use (CR 701.40c). A
 * manifested disguise card turned up for its disguise cost isn't paying a mana cost or a morph cost.
 */
val QarsiDeceiver = card("Qarsi Deceiver") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Wizard"
    power = 0
    toughness = 4
    oracleText = "{T}: Add {C}. Spend this mana only to cast a face-down creature spell, pay a mana cost to " +
        "turn a manifested creature face up, or pay a morph cost. (A megamorph cost is a morph cost.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(
            1,
            restriction = ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.FaceDownSpellsOnly,
                    ManaRestriction.TurnPermanentsFaceUpOnly(
                        procedures = setOf(FaceDownMode.MANIFEST, FaceDownMode.MORPH)
                    ),
                )
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. Spend this mana only to cast a face-down creature spell, pay a mana cost " +
            "to turn a manifested creature face up, or pay a morph cost."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64d64264-895f-4d4d-8e5c-ab1d0d6e340b.jpg?1783938604"

        ruling(
            "2015-02-25",
            "The mana produced by Qarsi Deceiver can't be used to cast a spell that instructs you to manifest cards."
        )
        ruling(
            "2015-02-25",
            "Turning a face-down creature with megamorph face up and putting a +1/+1 counter on it is a special " +
                "action. It doesn't use the stack and can't be responded to."
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, if you manifest " +
                "a card with megamorph and then pay its mana cost to turn it face up), you won't put a +1/+1 " +
                "counter on it."
        )
    }
}
