package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Urborg Repossession — Dominaria United #114
 * {B} · Sorcery
 *
 * The kicked clause adds a target, so the two casts announce different targets (CR 601.2c):
 * the plain cast has only the creature card (2022-09-09 ruling), the kicked cast rides the
 * `kickerTarget` / `kickerEffect` branch and also needs "another" permanent card — a
 * [TargetOther], so the second pick can't be the creature card already chosen. A kicked cast
 * therefore needs both cards in the graveyard to be cast at all, as the ruling says.
 */
val UrborgRepossession = card("Urborg Repossession") {
    manaCost = "{B}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Kicker {1}{G} (You may pay an additional {1}{G} as you cast this spell.)\n" +
        "Return target creature card from your graveyard to your hand. You gain 2 life. If this " +
        "spell was kicked, return another target permanent card from your graveyard to your hand."

    keywordAbility(KeywordAbility.kicker("{1}{G}"))

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creature) then Effects.GainLife(2)

        val kCreature = kickerTarget(TargetFilter.CreatureInYourGraveyard)
        val kPermanent = kickerTarget(TargetOther(TargetObject(filter = TargetFilter.PermanentInYourGraveyard)))
        kickerEffect = Effects.ReturnToHand(kCreature) then Effects.GainLife(2) then
            Effects.ReturnToHand(kPermanent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecb8dab0-ecce-4321-b382-c58b296c2fd3.jpg?1783921322"
        ruling(
            "2022-09-09",
            "You can't cast Urborg Repossession kicked if there aren't both a creature card and " +
                "another permanent card in your graveyard."
        )
        ruling("2022-09-09", "If it isn't kicked, Urborg Repossession has only one target.")
    }
}
