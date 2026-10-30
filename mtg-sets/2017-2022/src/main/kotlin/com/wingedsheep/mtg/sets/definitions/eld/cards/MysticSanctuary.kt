package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mystic Sanctuary
 * Land — Island
 *
 * ({T}: Add {U}.)
 * This land enters tapped unless you control three or more other Islands.
 * When this land enters untapped, you may put target instant or sorcery card from your graveyard
 * on top of your library.
 *
 * The Island subtype supplies the {T}: Add {U} ability. "Enters untapped" is the `untapped()`
 * state predicate on the self enters trigger, which the trigger matcher reads off how the land
 * *entered* — a land that entered tapped never triggers, even if untapped later in the same
 * resolution. The "you may" is answered at targeting, as on Mortuary Mire.
 */
val MysticSanctuary = card("Mystic Sanctuary") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Land — Island"
    oracleText = "({T}: Add {U}.)\n" +
        "This land enters tapped unless you control three or more other Islands.\n" +
        "When this land enters untapped, you may put target instant or sorcery card from your " +
        "graveyard on top of your library."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Land.withSubtype(Subtype.ISLAND))
    ))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        val spell = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.May(Effects.PutOnTopOfLibrary(spell))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/170e792c-80d5-4775-ad95-37614574ab84.jpg?1783932577"
        ruling(
            "2019-10-04",
            "As these lands are entering the battlefield, they check for lands that are already on the " +
                "battlefield. They won't see lands that are entering the battlefield at the same time " +
                "(due to Scapeshift, for example).",
        )
        ruling(
            "2019-10-04",
            "If another effect puts these lands onto the battlefield tapped, they enter tapped, even if " +
                "you control enough lands with the appropriate basic land type.",
        )
    }
}
