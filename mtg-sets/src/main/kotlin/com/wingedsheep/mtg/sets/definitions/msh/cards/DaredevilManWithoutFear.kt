package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Daredevil, Man Without Fear — Marvel Super Heroes #213
 * {2}{R}{W} · Legendary Creature — Human Hero · 3/4
 *
 * Vigilance, haste
 * Radar Sense — You may look at the top card of your library any time.
 * Whenever you attack, you may exile the top card of your library. If that card is a Hero card,
 * Daredevil gets +2/+1 until end of turn. You may play that card this turn.
 *
 * Modeling notes:
 *  - "Radar Sense" is an ability word (pure flavor, no rules meaning), so the clause is just the
 *    static [LookAtTopOfLibrary] — the private top-card peek, as on Madame Web and Glowcap Lantern.
 *    No new vocabulary; the label lives in `oracleText` only.
 *  - "Whenever you attack" is the once-per-combat [Triggers.YouAttack] batch trigger (CR 506.5,
 *    one trigger no matter how many creatures attack), *not* a per-attacker `attacks()`.
 *  - The trigger body is the Bonehoard Dracosaur shape wrapped in a "you may": `Patterns.Exile.impulse`
 *    exiles the top card into the `daredevilExiled` collection and grants play-this-turn permission
 *    ("You may play that card this turn"), then a [ConditionalEffect] reads that same collection to
 *    decide the +2/+1. Declining the may skips the exile entirely, so no pump and no permission —
 *    exactly the printed sequencing.
 *  - "Hero card" is a subtype match on any card type (Hero is a creature type in this set), the
 *    house filter `GameObjectFilter.Any.withSubtype(Subtype.HERO)`.
 *  - The pump lands on [EffectTarget.Self] — Daredevil himself — even though the trigger has ANY
 *    binding, since `Self` resolves to the ability's source.
 */
val DaredevilManWithoutFear = card("Daredevil, Man Without Fear") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Legendary Creature — Human Hero"
    power = 3
    toughness = 4
    oracleText = "Vigilance, haste\n" +
        "Radar Sense — You may look at the top card of your library any time.\n" +
        "Whenever you attack, you may exile the top card of your library. If that card is a Hero " +
        "card, Daredevil gets +2/+1 until end of turn. You may play that card this turn."

    keywords(Keyword.VIGILANCE, Keyword.HASTE)

    // Radar Sense — You may look at the top card of your library any time.
    staticAbility {
        ability = LookAtTopOfLibrary
    }

    // Whenever you attack, you may exile the top card of your library. …
    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = MayEffect(
            Effects.Composite(
                listOf(
                    Patterns.Exile.impulse(count = 1, storeAs = "daredevilExiled"),
                    ConditionalEffect(
                        condition = Conditions.CollectionContainsMatch(
                            "daredevilExiled",
                            GameObjectFilter.Any.withSubtype(Subtype.HERO),
                        ),
                        effect = Effects.ModifyStats(2, 1, EffectTarget.Self),
                    ),
                ),
            ),
            descriptionOverride = "You may exile the top card of your library. If that card is a " +
                "Hero card, Daredevil gets +2/+1 until end of turn. You may play that card this turn.",
        )
        description = "Whenever you attack, you may exile the top card of your library. If that " +
            "card is a Hero card, Daredevil gets +2/+1 until end of turn. You may play that card this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Dan Brereton"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14e821bb-55cc-474e-9b63-0ceecc2666c1.jpg?1783902902"
    }
}
