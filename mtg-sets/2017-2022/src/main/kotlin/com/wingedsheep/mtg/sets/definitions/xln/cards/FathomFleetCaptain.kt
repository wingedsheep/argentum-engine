package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Fathom Fleet Captain
 * {1}{B}
 * Creature — Human Pirate
 * 2/1
 * Menace
 * Whenever this creature attacks, if you control another nontoken Pirate, you may pay {2}. If you
 *   do, create a 2/2 black Pirate creature token with menace.
 *
 * "Another" is [Conditions.YouControlOtherAtLeast] (the count excludes the Captain itself), as an
 * intervening "if" so it is checked both when the trigger fires and on resolution (ruling). The
 * optional {2} is the mana form of [Effects.MayPay] — a single payment, so at most one token.
 */
val FathomFleetCaptain = card("Fathom Fleet Captain") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 1
    oracleText = "Menace\nWhenever this creature attacks, if you control another nontoken Pirate, you may " +
        "pay {2}. If you do, create a 2/2 black Pirate creature token with menace."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.YouControlOtherAtLeast(
            1,
            GameObjectFilter.Permanent.withSubtype(Subtype.PIRATE).nontoken()
        )
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Pirate"),
                keywords = setOf(Keyword.MENACE),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Matt Stewart"
        flavorText = "\"Join me, and take the world for yourself!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36cd7467-ff8c-4a92-ae55-6894213d6c0b.jpg?1783935760"
        ruling("2017-09-29", "While resolving Fathom Fleet Captain's triggered ability, you can't pay {2} multiple times to create more than one Pirate token.")
        ruling("2017-09-29", "If you don't control another nontoken Pirate at the moment Fathom Fleet Captain attacks, its triggered ability won't trigger. If you don't control another nontoken Pirate as that ability resolves, you can't pay {2}.")
    }
}
