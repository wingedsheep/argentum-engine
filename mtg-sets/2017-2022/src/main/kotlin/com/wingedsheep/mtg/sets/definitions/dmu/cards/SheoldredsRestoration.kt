package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sheoldred's Restoration — Dominaria United #108
 * {3}{B} · Sorcery
 *
 * "That card's mana value" is the card's in the graveyard, frozen before the move (Reanimate's
 * shape), so a creature that changes once on the battlefield can't change the life swing.
 * Kicked gains that much; unkicked loses it. `selfExile()` exiles the spell only when it resolves —
 * if the target is gone, the spell fizzles and goes to the graveyard (2022-09-09 ruling).
 */
val SheoldredsRestoration = card("Sheoldred's Restoration") {
    manaCost = "{3}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Kicker {2}{W} (You may pay an additional {2}{W} as you cast this spell.)\n" +
        "Return target creature card from your graveyard to the battlefield. If this spell was " +
        "kicked, you gain life equal to that card's mana value. Otherwise, you lose that much life.\n" +
        "Exile Sheoldred's Restoration."

    keywordAbility(KeywordAbility.kicker("{2}{W}"))

    spell {
        selfExile()
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        val manaValue = DynamicAmounts.storedNumber("mana_value")
        effect = Effects.StoreNumber("mana_value", DynamicAmounts.manaValueOf(creature)) then
            Effects.PutOntoBattlefieldFromGraveyard(creature) then
            Effects.If(
                Conditions.WasKicked,
                Effects.GainLife(manaValue),
                Effects.LoseLife(manaValue, EffectTarget.Controller)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed719f25-1aab-4a1e-8c9f-dd574db5de0c.jpg?1783921325"
        ruling(
            "2022-09-09",
            "If the target creature card is an illegal target as Sheoldred's Restoration tries to " +
                "resolve (usually because the card has been removed from the graveyard in response), " +
                "it won't do anything. You won't gain or lose life, and Sheoldred's Restoration will " +
                "go to the graveyard rather than be exiled by its last ability."
        )
    }
}
