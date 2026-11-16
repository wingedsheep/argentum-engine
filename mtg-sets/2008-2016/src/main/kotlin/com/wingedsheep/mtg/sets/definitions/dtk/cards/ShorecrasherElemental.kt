package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shorecrasher Elemental
 * {U}{U}{U}
 * Creature — Elemental
 * 3/3
 * {U}: Exile this creature, then return it to the battlefield face down under its owner's control.
 * {1}: This creature gets +1/-1 or -1/+1 until end of turn.
 * Megamorph {4}{U}
 *
 * The blink is two moves on `Self`, as on Ojutai Exemplars: the engine follows the object through
 * the exile move the ability itself made, and if the Elemental left the battlefield in response,
 * `Self` no longer names it and nothing moves. The return is gated on `fromZone = EXILE` (it only
 * comes back if it actually went to exile) and carries no controller override, so it enters under
 * its owner's control. `FaceDownMode.MORPH` makes it a 2/2 face-down permanent whose turn-up
 * procedure is the card's own megamorph cost, so turning it up also adds the +1/+1 counter.
 *
 * The P/T choice is made as the ability resolves (second ruling), so it is a `ChooseAction` in the
 * effect rather than two separate abilities.
 */
val ShorecrasherElemental = card("Shorecrasher Elemental") {
    manaCost = "{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 3
    oracleText = "{U}: Exile this creature, then return it to the battlefield face down under its " +
        "owner's control.\n" +
        "{1}: This creature gets +1/-1 or -1/+1 until end of turn.\n" +
        "Megamorph {4}{U} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.Move(EffectTarget.Self, Zone.EXILE) then
            Effects.Move(
                EffectTarget.Self,
                Zone.BATTLEFIELD,
                fromZone = Zone.EXILE,
                faceDown = FaceDownMode.MORPH,
            )
        description = "{U}: Exile Shorecrasher Elemental, then return it to the battlefield face " +
            "down under its owner's control."
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Shorecrasher Elemental gets +1/-1 until end of turn",
                effect = Effects.ModifyStats(1, -1, EffectTarget.Self),
            ),
            EffectChoice(
                label = "Shorecrasher Elemental gets -1/+1 until end of turn",
                effect = Effects.ModifyStats(-1, 1, EffectTarget.Self),
            ),
        ))
        description = "{1}: Shorecrasher Elemental gets +1/-1 or -1/+1 until end of turn."
    }

    // Megamorph {4}{U}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{4}{U}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "73"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8890fc6-45e9-443d-ae86-f1acb1ebd0aa.jpg?1783938604"
        ruling(
            "2015-02-25",
            "After Shorecrasher Elemental's first ability returns it to the battlefield, it will be " +
                "a new object with no connection to the Shorecrasher Elemental that left the " +
                "battlefield. It won't be in combat or have any additional abilities it may have had " +
                "when it left the battlefield. Any +1/+1 counters on it or Auras attached to it are " +
                "removed."
        )
        ruling(
            "2015-02-25",
            "You choose whether Shorecrasher Elemental gets +1/-1 or -1/+1 as the last activated " +
                "ability resolves."
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it."
        )
    }
}
