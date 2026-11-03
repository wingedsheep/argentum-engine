package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kirk, Commanding Officer — Star Trek #243
 * {1}{R}{W} · Legendary Creature — Human Officer · 3/3
 *
 * Whenever you attack while creatures you control have total power 8 or greater, exile the top
 * card of your library. You may play that card this turn.
 * {2}, Sacrifice Kirk: Destroy target artifact or enchantment.
 *
 * "Whenever you attack while …" fires once per combat and gates the trigger itself, so the
 * total-power check is a `triggerRestriction` (same shape as Tactical Officer); the impulse draw
 * mirrors Kirk, Enterprising Captain.
 */
val KirkCommandingOfficer = card("Kirk, Commanding Officer") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Officer"
    oracleText = "Whenever you attack while creatures you control have total power 8 or greater, " +
        "exile the top card of your library. You may play that card this turn.\n" +
        "{2}, Sacrifice Kirk: Destroy target artifact or enchantment."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.attacks()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        effect = Effects.Pipeline {
            val exiledCard = gather(CardSource.TopOfLibrary(1))
            exile(exiledCard)
            run(Effects.GrantMayPlayFromExile(exiledCard))
        }
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        val victim = target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment))
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Cliff Nielsen"
        flavorText = "\"I don't believe in a no-win scenario.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/4/245d0430-0967-44f1-8c59-985104a8db30.jpg?1792508356"
    }
}
