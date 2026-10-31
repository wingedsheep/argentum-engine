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
 * Witch's Cottage
 * Land — Swamp
 *
 * ({T}: Add {B}.)
 * This land enters tapped unless you control three or more other Swamps.
 * When this land enters untapped, you may put target creature card from your graveyard on top
 * of your library.
 *
 * Same shape as [MysticSanctuary] with a creature-card target: the Swamp subtype supplies the
 * {B} ability, "enters untapped" is the `untapped()` state predicate on the self enters trigger,
 * the target is chosen when the trigger is put on the stack, and the "you may" is asked on
 * resolution.
 */
val WitchsCottage = card("Witch's Cottage") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land — Swamp"
    oracleText = "({T}: Add {B}.)\n" +
        "This land enters tapped unless you control three or more other Swamps.\n" +
        "When this land enters untapped, you may put target creature card from your graveyard " +
        "on top of your library."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Land.withSubtype(Subtype.SWAMP))
    ))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.May(Effects.PutOnTopOfLibrary(creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "249"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b87891cd-b457-4dff-8d18-a7eaf6748fc6.jpg?1783932575"
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
