package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Journey to Oblivion — Zendikar Rising #17
 * {4}{W} · Enchantment
 *
 * This spell costs {1} less to cast for each creature in your party.
 * When this enchantment enters, exile target nonland permanent an opponent controls until this
 * enchantment leaves the battlefield.
 *
 * The discount is a self-cast [ModifySpellCost] reading [DynamicAmounts.partySize] for the caster;
 * it reduces only the generic part, so a full party casts it for {W}. The exile is the Banishing
 * Light pair: [Effects.ExileUntilLeaves] links the exiled permanent, and the leave trigger returns
 * it with [Effects.ReturnLinkedExileUnderOwnersControl].
 */
val JourneyToOblivion = card("Journey to Oblivion") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)\n" +
        "When this enchantment enters, exile target nonland permanent an opponent controls until this " +
        "enchantment leaves the battlefield."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47120e3c-73aa-468d-96b7-47aead342e31.jpg"
    }
}
