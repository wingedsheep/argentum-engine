package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Locutus, the One Who Speaks
 * {W}{B}
 * Legendary Artifact Creature — Borg
 * 1/1
 *
 * Artifact creatures you control get +1/+1.
 * {W}{B}, Discard this card: It deals 2 damage to target creature. You gain 2 life.
 *
 * - The lord counts Locutus itself (it is an artifact creature), so it plays as a 2/2.
 * - The discard ability is activated from hand; the discarded card is the damage source.
 */
val LocutusTheOneWhoSpeaks = card("Locutus, the One Who Speaks") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Artifact Creature — Borg"
    power = 1
    toughness = 1
    oracleText = "Artifact creatures you control get +1/+1.\n" +
        "{W}{B}, Discard this card: It deals 2 damage to target creature. You gain 2 life."

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{B}"), Costs.DiscardSelf)
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(2, creature, damageSource = EffectTarget.Self) then Effects.GainLife(2)
        activateFromZone = Zone.HAND
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Ariel Orea"
        flavorText = "\"The knowledge and experience of the human, Picard, is part of us now.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/296f1127-14a0-42c4-9e4a-c0d929720748.jpg"
    }
}
