package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Misty Mountains Cold
 * {2}{R}
 * Enchantment — Saga
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I, II, III, IV — Create a Treasure token. Then if you control four or more Treasures, sacrifice
 * this Saga. If you do, create a 6/6 red Dragon creature token with flying.
 *
 *  - **All four chapters are the same ability**, so [mistyMountainsChapter] is declared once and
 *    wired to chapters I–IV. The Saga's own "sacrifice after IV" state trigger still applies if the
 *    Dragon never comes: reaching IV without four Treasures just ends the Saga the ordinary way
 *    (CR 714.4).
 *  - **Two gates, not one.** "Then if you control four or more Treasures" is a state check at
 *    resolution ([ConditionalEffect] over [Conditions.YouControlAtLeast]) — and it counts *after*
 *    this chapter's Treasure is created, so the fourth Treasure the chapter itself mints turns it
 *    on. "If you do" is a second gate on the sacrifice actually happening
 *    ([SuccessCriterion.PermanentsSacrificed]): if the Saga has already left the battlefield, or
 *    can't be sacrificed, there is no Dragon. `SuccessCriterion.Always` here would fail open and
 *    mint a free Dragon.
 *  - The count is every Treasure you control, not just the ones this Saga made.
 */
val TheMistyMountainsCold = card("The Misty Mountains Cold") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I, II, III, IV — Create a Treasure token. Then if you control four or more Treasures, " +
        "sacrifice this Saga. If you do, create a 6/6 red Dragon creature token with flying. " +
        "(A Treasure token is an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    sagaChapter(1) { effect = mistyMountainsChapter() }
    sagaChapter(2) { effect = mistyMountainsChapter() }
    sagaChapter(3) { effect = mistyMountainsChapter() }
    sagaChapter(4) { effect = mistyMountainsChapter() }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d5f35ff-4146-4844-9da5-031461cc8c05.jpg?1784673451"
    }
}

/** The one chapter ability shared by I, II, III and IV. */
private fun mistyMountainsChapter(): Effect =
    Effects.CreateTreasure()
        .then(
            ConditionalEffect(
                condition = Conditions.YouControlAtLeast(
                    4,
                    GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE)
                ),
                effect = Effects.IfYouDo(
                    action = Effects.SacrificeTarget(EffectTarget.Self),
                    ifYouDo = Effects.CreateToken(
                        power = 6,
                        toughness = 6,
                        colors = setOf(Color.RED),
                        creatureTypes = setOf("Dragon"),
                        keywords = setOf(Keyword.FLYING),
                        controller = EffectTarget.Controller,
                        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e4408fa-8037-42f1-989e-2da84867f76c.jpg?1785497692",
                    ),
                    successCriterion = SuccessCriterion.PermanentsSacrificed,
                )
            )
        )
