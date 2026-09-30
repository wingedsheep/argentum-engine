package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Necrogen Communion
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature you control
 * Enchanted creature has toxic 2.
 * When enchanted creature dies, return that card to the battlefield under your control.
 *
 * "Has toxic 2" is a static grant of the projected `TOXIC_2` keyword — the same form printed toxic
 * projects as and `Effects.GrantToxic` floats, so the projector sums it with any other toxic the
 * creature has (CR 702.164b) and combat damage reads the total. The return is Ferocity of the
 * Hunt's shape: an attached-bound dies trigger moving the triggering card out of the graveyard
 * (nothing returns if it left the graveyard first), with the controller override putting it
 * under the Aura controller's control rather than its owner's.
 */
val NecrogenCommunion = card("Necrogen Communion") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "Enchanted creature has toxic 2. (Players dealt combat damage by it also get two poison counters.)\n" +
        "When enchanted creature dies, return that card to the battlefield under your control."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    staticAbility {
        ability = GrantKeyword("${Keyword.TOXIC.name}_2")
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Ernanda Souza"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ac8cd7c-992f-4953-b5bf-42e5a9ff09ad.jpg?1783918044"
    }
}
