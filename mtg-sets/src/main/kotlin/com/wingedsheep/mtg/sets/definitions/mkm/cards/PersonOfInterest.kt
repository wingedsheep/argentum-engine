package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Person of Interest — Murders at Karlov Manor #139
 * {3}{R} · Creature — Human Rogue · 2/2
 *
 * When this creature enters, suspect it. Create a 2/2 white and blue Detective creature token.
 *
 * One trigger with two sentences, so both happen on the same resolution. "Suspect it" is not a
 * target — it names the source itself (`EffectTarget.Self`), which is why the Detective still
 * arrives if Person of Interest has already left the battlefield by resolution.
 *
 * Suspecting itself is a real drawback traded for the body: CR 701.60a gives it menace *and*
 * "can't block", permanently. The Detective token is the untainted blocker of the pair.
 *
 * The token's art comes from the MKM `tokenArt` layer (a 2/2 white-and-blue Detective is one of
 * the set's printed tokens), so no `imageUri` is baked in here.
 */
val PersonOfInterest = card("Person of Interest") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    oracleText = "When this creature enters, suspect it. Create a 2/2 white and blue Detective " +
        "creature token. (A suspected creature has menace and can't block.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.Suspect(EffectTarget.Self),
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE, Color.BLUE),
                creatureTypes = setOf("Detective")
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Justyna Dura"
        flavorText = "\"Maybe they're following someone else,\" Alvis thought to himself, despite " +
            "mounting evidence to the contrary."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d56ebff-67c8-4bc7-a533-ddde4ce0c2af.jpg?1783912876"

        ruling(
            "2024-02-02",
            "If Person of Interest is no longer on the battlefield when its triggered ability " +
                "resolves, you'll still create a Detective token."
        )
        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
    }
}
