package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mentor's Guidance — Strixhaven: School of Mages #46 (canonical printing)
 * {2}{U} · Sorcery
 *
 * When you cast this spell, copy it if you control a planeswalker, Cleric, Druid, Shaman,
 * Warlock, or Wizard.
 * Scry 1, then draw a card.
 *
 * The cast trigger ([Triggers.self] `isCast()`, the Sage of the Skies shape) always triggers; the
 * "if you control …" is checked only as it resolves (ruling), so it is an [Effects.If] around
 * [Effects.CopyTargetSpell] of the triggering spell, not an intervening-if or a trigger
 * restriction. [Conditions.YouControl] over "a planeswalker, or a permanent with any of those
 * subtypes" copies exactly once however many qualify. A copy is put on the stack, not cast
 * (CR 707.10), so it doesn't re-trigger.
 */
val MentorsGuidance = card("Mentor's Guidance") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "When you cast this spell, copy it if you control a planeswalker, Cleric, Druid, " +
        "Shaman, Warlock, or Wizard.\n" +
        "Scry 1, then draw a card."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.If(
            Conditions.YouControl(
                GameObjectFilter.Planeswalker or
                    GameObjectFilter.Permanent.withAnySubtype("Cleric", "Druid", "Shaman", "Warlock", "Wizard")
            ),
            Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)
        )
        description = "When you cast this spell, copy it if you control a planeswalker, Cleric, " +
            "Druid, Shaman, Warlock, or Wizard."
    }

    spell {
        effect = Effects.Scry(1) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Brian Valeza"
        flavorText = "Kasmina had encouraged the twins to apply. Strixhaven would test not only their magical potential, but also their character."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dbb1230-a465-4499-bb9c-35a06712a08b.jpg?1783927378"
        ruling("2021-04-16", "The triggered ability will trigger when you cast Mentor's Guidance no matter what permanents you control. As that ability tries to resolve, it will check to see if you control at least one permanent with the listed type or subtypes. If you do, Mentor's Guidance will be copied once, not once for each of those permanents. If you don't control a qualifying permanent at that time, the triggered ability won't resolve and it won't copy Mentor's Guidance.")
    }
}
