package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * White Widow, Free Agent — Marvel Super Heroes #42
 * {3}{W} · Legendary Creature — Human Hero Villain · 2/3
 *
 * When White Widow enters, choose one —
 * • Put a +1/+1 counter on each of up to two target creatures.
 * • Return target artifact or enchantment card from your graveyard to your hand.
 *
 * A modal *triggered* ability (CR 603.3c): because both modes target, the mode and its targets
 * are chosen as the ability goes on the stack, not on resolution. Mode 1 is the Byrke,
 * Long Ear of the Law shape — an optional `count = 2` [TargetCreature] fanned out with
 * [ForEachTargetEffect] so each chosen creature gets exactly one counter (and "up to two" stays
 * honest: zero or one target is legal). Mode 2 mirrors Hanna, Ship's Navigator — an
 * `ArtifactOrEnchantment.ownedByYou()` filter constrained to [Zone.GRAVEYARD].
 */
val WhiteWidowFreeAgent = card("White Widow, Free Agent") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Hero Villain"
    power = 2
    toughness = 3
    oracleText = "When White Widow enters, choose one —\n" +
        "• Put a +1/+1 counter on each of up to two target creatures.\n" +
        "• Return target artifact or enchantment card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode(
                effect = ForEachTargetEffect(
                    listOf(
                        Effects.AddCounters(
                            Counters.PLUS_ONE_PLUS_ONE,
                            1,
                            EffectTarget.ContextTarget(0),
                        )
                    )
                ),
                targetRequirements = listOf(TargetCreature(count = 2, optional = true)),
                description = "Put a +1/+1 counter on each of up to two target creatures",
            ),
            Mode.withTarget(
                Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
                TargetObject(
                    filter = TargetFilter(
                        baseFilter = GameObjectFilter.ArtifactOrEnchantment.ownedByYou(),
                        zone = Zone.GRAVEYARD,
                    )
                ),
                "Return target artifact or enchantment card from your graveyard to your hand",
            ),
        )
        description = "When White Widow enters, choose one — " +
            "• Put a +1/+1 counter on each of up to two target creatures. " +
            "• Return target artifact or enchantment card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Julia Vasilyeva"
        flavorText = "\"For years I wanted to be you, Natasha, but I make my own way now.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57da765f-7d87-42ac-9eb0-a49c296fbbf8.jpg?1783902963"
    }
}
